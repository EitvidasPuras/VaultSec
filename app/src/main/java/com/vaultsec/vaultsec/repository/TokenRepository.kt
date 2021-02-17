package com.vaultsec.vaultsec.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.dao.TokenDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.entity.*
import com.vaultsec.vaultsec.util.Holder
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.lang.IllegalStateException
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

    suspend fun insert(token: Token) {
        tokenDao.insert(token)
    }

    suspend fun delete(token: Token) {
        tokenDao.delete(token)
    }

    suspend fun getToken(): Token {
        return tokenDao.getToken()
    }

    suspend fun postRegister(user: ApiUser): ApiResponse<*> {
        try {
            val response = api.postRegister(user)
            try {
                response.get("success").asJsonObject
                ApiResponse.Success<Any>()
            } catch (e: ClassCastException) {
                Log.e(
                    "com.vaultsec.vaultsec.repository.postRegister.CAST", e.toString()
                )
                ApiResponse.Success<Any>()
            }
            return ApiResponse.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
//                    Log.e("errorBody", errorBody!!.string())
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.charStream(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postRegister.HTTP", apiError.error)
                    return ApiResponse.Error<Any>(ErrorTypes.HTTP, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.TIMEOUT",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.CONNECTION",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.SOCKET",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postRegister.GENERIC",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun postLogin(user: ApiUser): ApiResponse<*> {
        try {
            var loginResponse = api.postLogin(user)
            try {
                loginResponse = loginResponse.get("success").asJsonObject
                if (loginResponse.has("token")) {
                    val notesResponse = getUserNotes(loginResponse["token"].asString)
                    if (notesResponse is ApiResponse.Success){
                        val token = Token(loginResponse["token"].asString)
                        tokenDao.deleteAll()
                        tokenDao.insert(token)
                        noteDao.deleteAll()
                        noteDao.insertList(notesResponse.data as ArrayList<Note>)
                        return ApiResponse.Success<Any>()
                    } else {
                        return notesResponse
                    }
                }
            } catch (e: ClassCastException) {
                Log.e("com.vaultsec.vaultsec.repository.postLogin.CAST", e.toString())
                return ApiResponse.Success<Any>()
            }
            return ApiResponse.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.charStream(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postLogin.HTTP", apiError.error)
                    return ApiResponse.Error<Any>(ErrorTypes.HTTP, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.TIMEOUT",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.CONNECTION",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin.SOCKET", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin.GENERIC",
                        e.message.toString()
                    )
                    return ApiResponse.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun postLogout(header: String): ApiResponse<*> {
        try {
            // Sync noted before logout
            val unsyncedNotes = noteDao.getUnsyncedNotes()
            api.postUnsyncedNotes(unsyncedNotes.first(), header)
            // Actually logout
            api.postLogout(header)
            // Empty the database
            db.clearAllTables()
            return ApiResponse.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.charStream(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postLogout.HTTP", apiError.error)
                    return ApiResponse.Error<Any>(ErrorTypes.HTTP, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogout.TIMEOUT", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogout.CONNECTION", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin.SOCKET", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogout.GENERAL", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    private suspend fun getUserNotes(token: String): ApiResponse<*> {
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
                        synced = true,
                        id = it.id
                    )
                )
            }
            return ApiResponse.Success(notes)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.charStream(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.getUserNotes.HTTP", apiError.error)
                    return ApiResponse.Error<Any>(ErrorTypes.HTTP, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e("com.vaultsec.vaultsec.repository.getUserNotes.TIMEOUT", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e("com.vaultsec.vaultsec.repository.getUserNotes.CONNECTION", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("com.vaultsec.vaultsec.repository.getUserNotes.SOCKET", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e("com.vaultsec.vaultsec.repository.getUserNotes.GENERAL", e.message.toString())
                    return ApiResponse.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }
}