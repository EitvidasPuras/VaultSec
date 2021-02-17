package com.vaultsec.vaultsec.network

import com.google.gson.JsonObject
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.entity.ApiNote
import com.vaultsec.vaultsec.network.entity.ApiUser
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface PasswordManagerApi {
    @POST("api/register")
    suspend fun postRegister(@Body apiUser: ApiUser): JsonObject

    @POST("api/login")
    suspend fun postLogin(@Body apiUser: ApiUser): JsonObject

    @POST("api/logout")
    suspend fun postLogout(@Header("Authorization") header: String): JsonObject

    @POST("api/notes")
    suspend fun postUnsyncedNotes(
        @Body unsyncedNotes: List<Note>,
        @Header("Authorization") header: String
    ): ArrayList<ApiNote>

    @GET("api/notes")
    suspend fun getUserNotes(
        @Header("Authorization") header: String
    ): ArrayList<ApiNote>
}