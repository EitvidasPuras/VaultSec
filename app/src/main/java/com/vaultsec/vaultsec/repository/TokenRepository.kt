package com.vaultsec.vaultsec.repository

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.PasswordManagerService
import com.vaultsec.vaultsec.network.entity.ApiError
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import retrofit2.HttpException
import java.net.SocketTimeoutException

class TokenRepository(application: Application) {

    private val database = PasswordManagerDatabase.getInstance(application)
    private val tokenDao = database.tokenDao()
    private val api: PasswordManagerApi = PasswordManagerService().apiService

    suspend fun insert(token: Token) {
        tokenDao.insert(token)
    }

    suspend fun delete(token: Token) {
        tokenDao.delete(token)
    }

    suspend fun getToken(): Token {
        return tokenDao.getToken()
    }

    suspend fun postRegister(user: ApiUser): ApiResponse {
        try {
            var response = api.postRegister(user)
            response = response.getAsJsonObject("success")
            tokenDao.deleteAll()
            val token = Token(0, response["token"].asString)
            tokenDao.insert(token)
            Log.e("com.vaultsec.vaultsec.repository.postRegister", response["token"].asString)
            return ApiResponse(false)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.string(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postRegister", apiError.error)
                    return ApiResponse(true, ErrorTypes.HTTP_ERROR, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postRegister", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.SOCKET_TIMEOUT
                    )
                }
                else -> {
                    Log.e("com.vaultsec.vaultsec.repository.postRegister", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.GENERAL
                    )
                }
            }
        }
    }

    suspend fun postLogin(user: ApiUser): ApiResponse {
        try {
            var response: JsonObject? = api.postLogin(user)
            try {
                response = response?.getAsJsonObject("success")
                if (response!!.has("token")) {
                    tokenDao.deleteAll()
                    val token = Token(0, response["token"].asString)
                    tokenDao.insert(token)
                    Log.e(
                        "com.vaultsec.vaultsec.repository.postLogin",
                        response["token"].asString
                    )
                    return ApiResponse(false)
                }
            } catch (e: ClassCastException) {
                return ApiResponse(false)
            }
            return ApiResponse(false)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.string(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postLogin", apiError.error)
                    return ApiResponse(true, ErrorTypes.HTTP_ERROR, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.SOCKET_TIMEOUT
                    )
                }
                else -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogin", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.GENERAL
                    )
                }
            }
        }
    }

    suspend fun postLogout(header: String): ApiResponse {
        try {
//            val response =
            api.postLogout(header)
            tokenDao.deleteAll()
//            if (response.has("success")){
            return ApiResponse(false)
//            }
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.string(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postLogout", apiError.error)
                    return ApiResponse(true, ErrorTypes.HTTP_ERROR, apiError.error)
                }
                is SocketTimeoutException -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogout", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.SOCKET_TIMEOUT
                    )
                }
                else -> {
                    Log.e("com.vaultsec.vaultsec.repository.postLogout", e.message.toString())
                    return ApiResponse(
                        true,
                        ErrorTypes.GENERAL
                    )
                }
            }
        }
    }
}