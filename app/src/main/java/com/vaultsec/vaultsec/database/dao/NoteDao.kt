package com.vaultsec.vaultsec.database.dao

import androidx.room.*
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.entity.ApiNote
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertList(noteList: ArrayList<Note>)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("""DELETE FROM vault_notes""")
    suspend fun deleteAll()

    @Query("""DELETE FROM vault_notes WHERE id IN (:idList)""")
    suspend fun deleteSelectedNotes(idList: ArrayList<Int>)

    @Query("""SELECT * FROM vault_notes WHERE synced = 0""")
    fun getUnsyncedNotes(): Flow<List<Note>>

    fun getNotes(searchQuery: String, sortOrder: SortOrder, isAsc: Boolean): Flow<List<Note>> =
        when (sortOrder) {
            SortOrder.BY_TITLE -> getNotesSortedByTitle(searchQuery, isAsc)
            SortOrder.BY_DATE_CREATED -> getNotesSortedByDateCreated(searchQuery, isAsc)
            SortOrder.BY_DATE_UPDATED -> getNotesSortedByDateUpdated(searchQuery, isAsc)
            SortOrder.BY_COLOR -> getNotesSortedByColor(searchQuery, isAsc)
            SortOrder.BY_FONT_SIZE -> getNotesSortedByFontSize(searchQuery, isAsc)
        }

    /*
    * If '%' || were to be removed, that would mean that the text or title of the note
    * would have to start with the search query. If || '%' were to be removed, that would mean that
    * the text or title of the note would have to end with the search query
    * */
    @Query(
        """SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY
            CASE WHEN :isAsc = 1 THEN title END ASC,
            CASE WHEN :isAsc = 1 AND title = "" THEN text END ASC,
            CASE WHEN :isAsc = 0 THEN title END DESC,
            CASE WHEN :isAsc = 0 AND title = "" THEN text END DESC"""
    )
    fun getNotesSortedByTitle(searchQuery: String, isAsc: Boolean): Flow<List<Note>>

    @Query(
        """SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY
            CASE WHEN :isAsc = 1 THEN created_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN created_at_local END DESC """
    )
    fun getNotesSortedByDateCreated(searchQuery: String, isAsc: Boolean): Flow<List<Note>>

    @Query(
        """SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY
            CASE WHEN :isAsc = 1 THEN updated_at_local END ASC,
            CASE WHEN :isAsc = 0 THEN updated_at_local END DESC """
    )
    fun getNotesSortedByDateUpdated(searchQuery: String, isAsc: Boolean): Flow<List<Note>>

    @Query(
        """SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY
            CASE WHEN :isAsc = 1 THEN color END ASC,
            CASE WHEN :isAsc = 0 THEN color END DESC """
    )
    fun getNotesSortedByColor(searchQuery: String, isAsc: Boolean): Flow<List<Note>>

    @Query(
        """SELECT * FROM vault_notes WHERE title LIKE '%' || :searchQuery || '%' OR text LIKE '%' || :searchQuery || '%' ORDER BY
            CASE WHEN :isAsc = 1 THEN font_size END ASC,
            CASE WHEN :isAsc = 0 THEN font_size END DESC """
    )
    fun getNotesSortedByFontSize(searchQuery: String, isAsc: Boolean): Flow<List<Note>>

}