package com.vaultsec.vaultsec.database.dao

import androidx.room.*
import com.vaultsec.vaultsec.database.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM vault_notes")
    suspend fun deleteAll()

    @Query("SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ")
    fun getNotes(searchQuery: String): Flow<List<Note>>

    @Query("SELECT COUNT(title) FROM vault_notes")
    suspend fun getItemCount(): Int

}