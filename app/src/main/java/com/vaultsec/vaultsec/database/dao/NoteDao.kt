package com.vaultsec.vaultsec.database.dao

import androidx.room.*
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.viewmodel.SortOrder
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

    fun getNotes(searchQuery: String, sortOrder: SortOrder): Flow<List<Note>> =
        when (sortOrder) {
            SortOrder.BY_TITLE -> getNotesSortedByTitle(searchQuery)
            SortOrder.BY_DATE_CREATED -> getNotesSortedByDateCreated(searchQuery)
            SortOrder.BY_DATE_UPDATED -> getNotesSortedByDateUpdated(searchQuery)
            SortOrder.BY_COLOR -> getNotesSortedByColor(searchQuery)
        }

    @Query("SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY title ASC ")
    fun getNotesSortedByTitle(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY created_at_local ASC ")
    fun getNotesSortedByDateCreated(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY updated_at_local ASC ")
    fun getNotesSortedByDateUpdated(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY color ASC ")
    fun getNotesSortedByColor(searchQuery: String): Flow<List<Note>>

    @Query("SELECT COUNT(title) FROM vault_notes")
    suspend fun getItemCount(): Int

}