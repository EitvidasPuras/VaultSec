package com.vaultsec.vaultsec.network

import com.google.gson.JsonObject
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.entity.ApiNote
import com.vaultsec.vaultsec.network.entity.ApiUser
import retrofit2.http.*

interface PasswordManagerApi {
    @POST("api/register")
    suspend fun postRegister(@Body apiUser: ApiUser): JsonObject

    @POST("api/login")
    suspend fun postLogin(@Body apiUser: ApiUser): JsonObject

    @POST("api/logout")
    suspend fun postLogout(@Header("Authorization") header: String): JsonObject

    @POST("api/notes")
    suspend fun postStoreNotes(
        @Body notes: List<Note>,
        @Header("Authorization") header: String
    ): ArrayList<ApiNote>

    @POST("api/notes/recover")
    suspend fun postRecoverNotes(
        @Body notes: List<Note>,
        @Header("Authorization") header: String
    ): ArrayList<ApiNote>

    @GET("api/notes")
    suspend fun getUserNotes(
        @Header("Authorization") header: String
    ): ArrayList<ApiNote>

    @HTTP(method = "DELETE", hasBody = true, path = "api/notes/delete")
    suspend fun deleteNotes(
        @Body syncedNotesIds: ArrayList<Int>,
        @Header("Authorization") header: String
    ): JsonObject

    @POST("api/notes/singular")
    suspend fun postSingleNote(
        @Body note: Note,
        @Header("Authorization") header: String
    ): Int
}