package com.vaultsec.vaultsec.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vaultsec.vaultsec.database.entity.Note

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM vault_notes")
    suspend fun deleteAll()

    @Query("SELECT * FROM vault_notes ORDER BY created_at_local DESC")
    fun getAllNotes(): LiveData<List<Note>>

}