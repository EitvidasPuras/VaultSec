package com.vaultsec.vaultsec.network

import com.google.gson.JsonObject
import com.vaultsec.vaultsec.network.entity.ApiUser
import retrofit2.http.Body
import retrofit2.http.POST

interface PasswordManagerApi {
    @POST("api/register")
    suspend fun postRegister(@Body apiUser: ApiUser): JsonObject
}