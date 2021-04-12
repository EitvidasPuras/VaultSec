package com.vaultsec.vaultsec.repository

import android.util.Log
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import com.vaultsec.vaultsec.util.hashString
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject

class AuthenticationRepository
@Inject constructor(
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences
) {
    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.AuthenticationRepository"
    }

    private fun getToken(): String {
        return encryptedSharedPrefs.getToken()!!
    }


    suspend fun postRegister(user: ApiUser): Resource<*> {
        try {
            val response = api.postRegister(user)
            try {
                response.get("success").asJsonObject
                Resource.Success<Any>()
            } catch (e: ClassCastException) {
                Log.e(
                    "$TAG.postRegister.CAST", e.toString()
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
                        "$TAG.postRegister.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.postRegister.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.postRegister.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "$TAG.postRegister.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.postRegister.GENERIC",
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
                        encryptedSharedPrefs.storeAccessToken(loginResponse["token"].asString)
                        encryptedSharedPrefs.storeCredentials(
                            hashString(user.password, 1),
                            hashString(user.email, 2)
                        )
//                        db.noteDao().deleteAll()
//                        db.noteDao().insertList(notesResponse.data as ArrayList<Note>)
                        return Resource.Success<Any>()
                    } else {
                        return notesResponse
                    }
                }
            } catch (e: ClassCastException) {
                Log.e("$TAG.postLogin.CAST", e.toString())
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
                        "$TAG.postLogin.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.postLogin.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.postLogin.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("$TAG.postLogin.SOCKET", e.message.toString())
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.postLogin.GENERIC",
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

    fun isUserLoggedIn(): Boolean {
        return encryptedSharedPrefs.getToken() != null && encryptedSharedPrefs.getCredentials() != null
    }
}