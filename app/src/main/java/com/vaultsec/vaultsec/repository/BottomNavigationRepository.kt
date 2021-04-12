package com.vaultsec.vaultsec.repository

import android.util.Log
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import com.vaultsec.vaultsec.util.cipher.CipherManager
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject

class BottomNavigationRepository @Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences,
    private val cm: CipherManager
) {
    private val noteDao = db.noteDao()

    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.BottomNavigationRepository"
    }

    private fun getToken(): String {
        return encryptedSharedPrefs.getToken()!!
    }

    suspend fun postLogout(databaseDir: String): Resource<*> {
        try {
            val combinedNotes = arrayListOf<Note>()
            // Sync noted before logout
            val syncedButDeletedIds = noteDao.getSyncedDeletedNotesIds().first()
            if (syncedButDeletedIds.isNotEmpty()) {
                api.deleteNotes(syncedButDeletedIds as ArrayList<Int>, "Bearer ${getToken()}")
            }

            val syncedUpdatedNotes = noteDao.getSyncedUpdatedNotes().first().map {
                it.title = cm.encrypt(it.title)
                it.text = cm.encrypt(it.text)!!
            } as List<Note>
            val unsyncedNotes = noteDao.getUnsyncedNotes().first().map {
                it.title = cm.encrypt(it.title)
                it.text = cm.encrypt(it.text)!!
            } as List<Note>
            combinedNotes.addAll(syncedUpdatedNotes)
            combinedNotes.addAll(unsyncedNotes)

            api.postStoreNotes(combinedNotes, "Bearer ${getToken()}")
            // Actually logout
            api.postLogout("Bearer ${getToken()}")
            // Empty the database
            db.clearAllTables()
            /*
            * The database file has to be deleted, because password generated key is slightly different
            * after each logout because of salt and IV
            * */
            File(File(databaseDir), "vaultsec-database").delete()
            // Clear encrypted shared preferences
            encryptedSharedPrefs.emptyEncryptedSharedPrefs()
            return Resource.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.postLogout.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.postLogout.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.postLogout.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("$TAG.postLogout.SOCKET", e.message.toString())
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.postLogout.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun onLogIn(): Resource<*> {
        val notesResponse = getUserNotes(getToken())
        return if (notesResponse is Resource.Success) {
            noteDao.deleteAll()
            noteDao.insertList(notesResponse.data as ArrayList<Note>)
            Resource.Success<Any>()
        } else {
            notesResponse
        }
    }

    private suspend fun getUserNotes(token: String): Resource<*> {
        try {
            val notesResponse = api.getUserNotes("Bearer $token")
            val notes = arrayListOf<Note>()
            notesResponse.map {
                notes.add(
                    Note(
                        title = cm.decrypt(it.title),
                        text = cm.decrypt(it.text)!!,
                        color = it.color,
                        fontSize = it.font_size,
                        createdAt = it.created_at_device,
                        updatedAt = it.updated_at_device,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = it.id
                    )
                )
            }
            return Resource.Success(notes)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.getUserNotes.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.getUserNotes.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.getUserNotes.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "$TAG.getUserNotes.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.getUserNotes.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    fun printTokenToConsoleForTesting() {
        Log.e("ACCESS TOKEN:::", getToken())
    }
}