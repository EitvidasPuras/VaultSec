package com.vaultsec.vaultsec.repository

import android.app.Application
import android.util.Log
import com.google.gson.Gson
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
            Log.e("Hm", response["token"].asString)
            return ApiResponse(false)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    val errorBody = e.response()?.errorBody()
                    val apiError: ApiError = Gson().fromJson(
                        errorBody!!.string(),
                        ApiError::class.java
                    )
                    Log.e("com.vaultsec.vaultsec.repository.postRegister", e.message.toString())
                    if (apiError.error.isNotEmpty()) {
                        return ApiResponse(
                            true,
                            ErrorTypes.HTTP_ERROR,
                            apiError.error.values.first()[0]
                        )
                    }
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
        return ApiResponse(
            true,
            ErrorTypes.GENERAL
        )
    }
}