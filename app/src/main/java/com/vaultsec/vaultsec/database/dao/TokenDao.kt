package com.vaultsec.vaultsec.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vaultsec.vaultsec.database.entity.Token

@Dao
interface TokenDao {
    @Insert
    suspend fun insert(token: Token)

    @Delete
    suspend fun delete(token: Token)

    @Query("SELECT * FROM access_tokens ORDER BY id ASC LIMIT 1")
    suspend fun getToken(): Token

//    @Query("SELECT * FROM access_tokens ORDER BY id ASC LIMIT 1")
//    fun getToken(): Flow<Token>

    @Query("DELETE FROM access_tokens")
    suspend fun deleteAll()
}