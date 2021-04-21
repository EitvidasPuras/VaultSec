package com.vaultsec.vaultsec.network

import com.google.gson.JsonObject
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.network.entity.ApiNote
import com.vaultsec.vaultsec.network.entity.ApiPassword
import com.vaultsec.vaultsec.network.entity.ApiUser
import retrofit2.http.*

interface PasswordManagerApi {
    @POST("api/register")
    suspend fun postRegister(@Body apiUser: ApiUser): JsonObject

    @POST("api/login")
    suspend fun postLogin(@Body apiUser: ApiUser): JsonObject

    @POST("api/logout")
    suspend fun postLogout(@Header("Authorization") header: String): JsonObject

    /*
    * ----- NOTES -----
    * */
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

    @PUT("api/notes/{id}")
    suspend fun putNoteUpdate(
        @Path("id") id: Int,
        @Body note: Note,
        @Header("Authorization") header: String
    ): Int
    /*
    * ----- NOTES -----
    * */

    /*
    * ----- PASSWORDS -----
    * */
    @POST("api/passwords")
    suspend fun postStorePasswords(
        @Body passwords: List<Password>,
        @Header("Authorization") header: String
    ): ArrayList<ApiPassword>

    @POST("api/passwords/recover")
    suspend fun postRecoverPasswords(
        @Body passwords: List<Password>,
        @Header("Authorization") header: String
    ): ArrayList<ApiPassword>

    @GET("api/passwords")
    suspend fun getUserPasswords(
        @Header("Authorization") header: String
    ): ArrayList<ApiPassword>

    @HTTP(method = "DELETE", hasBody = true, path = "api/passwords/delete")
    suspend fun deletePasswords(
        @Body syncedPasswordsIds: ArrayList<Int>,
        @Header("Authorization") header: String
    ): JsonObject

    @POST("api/passwords/singular")
    suspend fun postSinglePassword(
        @Body password: Password,
        @Header("Authorization") header: String
    ): Int

    @PUT("api/passwords/{id}")
    suspend fun putPasswordUpdate(
        @Path("id") id: Int,
        @Body password: Password,
        @Header("Authorization") header: String
    ): Int
    /*
    * ----- PASSWORDS -----
    * */
}