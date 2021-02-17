package com.vaultsec.vaultsec.repository

import android.util.Log
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.Holder
import com.vaultsec.vaultsec.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val api: PasswordManagerApi,
    private val tokenRepository: TokenRepository
) {
//    private val database = PasswordManagerDatabase.getInstance(application)
//    private val noteDao: NoteDao = database.noteDao()
//    private val api: PasswordManagerApi = PasswordManagerService().apiService

    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun deleteAll() {
        noteDao.deleteAll()
    }

    fun synchronizeNotes(
        didRefresh: Boolean,
        searchQuery: String, sortOrder: SortOrder, isAsc: Boolean, onFetchComplete: () -> Unit
    ): Flow<Holder<List<Note>>> =
        networkBoundResource(
            query = {
                noteDao.getNotes(searchQuery, sortOrder, isAsc)
            },
            fetch = {
                val notes = api.postUnsyncedNotes(
                    noteDao.getUnsyncedNotes().first(),
                    "Bearer ${tokenRepository.getToken().token}"
                )
                notes
            },
            saveFetchResult = { notesApi ->
                val notes = arrayListOf<Note>()
                notesApi.map {
                    notes.add(
                        Note(
                            title = it.title,
                            text = it.text,
                            color = it.color,
                            fontSize = it.font_size,
                            createdAt = it.created_at_device,
                            updatedAt = it.updated_at_device,
                            synced = true,
                            id = it.id
                        )
                    )
                }
                noteDao.deleteAll()
                noteDao.insertList(notes)
            },
            shouldFetch = {
                if (didRefresh) {
                    Log.e("didRefresh", "$didRefresh")
                    noteDao.getUnsyncedNotes().first().isNotEmpty()
                } else {
                    Log.e("didRefresh", "$didRefresh")
                    false
                }
            },
            onFetchSuccess = onFetchComplete,
            onFetchFailed = onFetchComplete
        )

    suspend fun deleteSelectedNotes(noteList: ArrayList<Note>) {
        val idList: ArrayList<Int> = noteList.map {
            it.id
        } as ArrayList<Int>
        noteDao.deleteSelectedNotes(idList)
    }

    suspend fun insertList(noteList: ArrayList<Note>) {
        noteDao.insertList(noteList)
    }
}