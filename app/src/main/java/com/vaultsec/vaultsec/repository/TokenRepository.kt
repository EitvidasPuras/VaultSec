package com.vaultsec.vaultsec.repository

import android.util.Log
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject

class TokenRepository
@Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi
) {

    private val tokenDao = db.tokenDao()
    private val noteDao = db.noteDao()
//    private val database = PasswordManagerDatabase.getInstance(application)
//    private val tokenDao = database.tokenDao()
//    private val api: PasswordManagerApi = PasswordManagerService().apiService

    private var apiError = JSONObject()

    suspend fun insert(token: Token) {
        tokenDao.insert(token)
    }

    suspend fun delete(token: Token) {
        tokenDao.delete(token)
    }

    suspend fun getToken(): Token {
        return tokenDao.getToken()
    }

    suspend fun postRegister(user: ApiUser): Resource<*> {
        try {
            val response = api.postRegister(user)
            try {
                response.get("success").asJsonObject
                Resource.Success<Any>()
            } catch (e: ClassCastException) {
                Log.e(
                    "com.vaultsec.vaultsec.repository.postRegister.CAST", e.toString()
                )
                Resource.Success<Any>()
            }
            return Resource.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    /*
                    * To prevent it from displaying previously shown errors, in case the
                    * errorBody()?.charStream()!! doesn't work
                    * */
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "com.vaultsec.vaultsec.repository.TokenRepository.postRegister.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.GENERIC",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun postLogin(user: ApiUser): Resource<*> {
        try {
            var loginResponse = api.postLogin(user)
            try {
                loginResponse = loginResponse.get("success").asJsonObject
                if (loginResponse.has("token")) {
                    val notesResponse = getUserNotes(loginResponse["token"].asString)
                    if (notesResponse is Resource.Success) {
                        val token = Token(loginResponse["token"].asString)
                        tokenDao.deleteAll()
                        tokenDao.insert(token)
                        noteDao.deleteAll()
                        noteDao.insertList(notesResponse.data as ArrayList<Note>)
                        return Resource.Success<Any>()
                    } else {
                        return notesResponse
                    }
                }
            } catch (e: ClassCastException) {
                Log.e("com.vaultsec.vaultsec.repository.postLogin.CAST", e.toString())
                return Resource.Success<Any>()
            }
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
                        "com.vaultsec.vaultsec.repository.TokenRepository.postLogin.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin.SOCKET", e.message.toString())
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.GENERIC",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun postLogout(header: String): Resource<*> {
        try {
            val combinedNotes = arrayListOf<Note>()
            // Sync noted before logout
            val syncedButDeleted = noteDao.getSyncedDeletedNotesIds()
            api.deleteNotes(syncedButDeleted.first() as ArrayList<Int>, header)

            combinedNotes.addAll(noteDao.getSyncedUpdatedNotes().first())
            combinedNotes.addAll(noteDao.getUnsyncedNotes().first())

            api.postStoreNotes(combinedNotes, header)
            // Actually logout
            api.postLogout(header)
            // Empty the database
            db.clearAllTables()
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
                        "com.vaultsec.vaultsec.repository.TokenRepository.postLogout.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogout.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogout.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin.SOCKET", e.message.toString())
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogout.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    private suspend fun getUserNotes(token: String): Resource<*> {
        try {
            val notesResponse = api.getUserNotes("Bearer $token")
            val notes = arrayListOf<Note>()
            notesResponse.map {
                notes.add(
                    Note(
                        title = it.title,
                        text = it.text,
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
                        "com.vaultsec.vaultsec.repository.TokenRepository.getUserNotes.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.getUserNotes.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.getUserNotes.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.getUserNotes.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.getUserNotes.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }
}