package com.vaultsec.vaultsec.repository

import android.util.Log
import com.google.gson.Gson
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.entity.ApiError
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.isNetworkAvailable
import com.vaultsec.vaultsec.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val api: PasswordManagerApi,
    private val tokenRepository: TokenRepository
) {
    private var didPerformDeletionAPICall: Boolean = false

    suspend fun insert(note: Note) {
        if (isNetworkAvailable) {
            try {
                val id = api.postSingleNote(note, "Bearer ${tokenRepository.getToken().token}")
                val newNote = note.copy(
                    title = note.title,
                    text = note.text,
                    color = note.color,
                    fontSize = note.fontSize,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isSynced = true,
                    id = id
                )
                noteDao.insert(newNote)
            } catch (e: Exception) {
                noteDao.insert(note)
                when (e) {
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()
                        Log.e("errorBody", errorBody!!.string())
                        val apiError: ApiError = Gson().fromJson(
                            errorBody!!.charStream(),
                            ApiError::class.java
                        )
                        Log.e(
                            "com.vaultsec.vaultsec.repository.NoteRepository.insert.HTTP",
                            apiError.error
                        )
                    }
                    else -> {
                        Log.e(
                            "com.vaultsec.vaultsec.repository.NoteRepository.insert.ELSE",
                            e.localizedMessage!!
                        )
                    }
                }
            }
        } else {
            noteDao.insert(note)
        }
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
    ): Flow<Resource<List<Note>>> =
        networkBoundResource(
            query = {
                noteDao.getNotes(searchQuery, sortOrder, isAsc)
            },
            fetch = {
                if (noteDao.getSyncedDeletedNotesIds().first().isNotEmpty()) {
                    api.deleteNotes(
                        noteDao.getSyncedDeletedNotesIds().first() as ArrayList<Int>,
                        "Bearer ${tokenRepository.getToken().token}"
                    )
                }
                val notes = api.postStoreNotes(
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
                            isSynced = true,
                            id = it.id
                        )
                    )
                }
                noteDao.deleteAll()
                noteDao.insertList(notes)
            },
            shouldFetch = {
                if (didRefresh) {
                    noteDao.getUnsyncedNotes().first().isNotEmpty() ||
                            noteDao.getSyncedDeletedNotesIds().first().isNotEmpty()
                } else {
                    false
                }
            },
            onFetchSuccess = onFetchComplete,
            onFetchFailed = onFetchComplete
        )

    suspend fun deleteSelectedNotes(noteList: ArrayList<Note>): Resource<*> {
        didPerformDeletionAPICall = false
        val unsyncedNotesIds = arrayListOf<Int>()
        val syncedNotesIds = arrayListOf<Int>()
        val syncedNotes = arrayListOf<Note>()
        noteList.map {
            it.isDeleted = true
            if (it.isSynced) {
                syncedNotesIds.add(it.id)
                syncedNotes.add(it)
            } else {
                unsyncedNotesIds.add(it.id)
            }
        }
        noteDao.deleteSelectedNotes(unsyncedNotesIds)
        noteDao.insertList(syncedNotes)
        /*
        * Don't know if, based on the MVVM architecture, it is allowed to use isNetworkAvailable variable
        * inside a repository, since the variable is somewhat dependant on the context.
        * If not, fix it by calling the function from the fragment like this:
        * noteViewModel.onDeleteClick((isNetworkAvailable == true))
        * and then pass it down the line to here
        * */
        if (isNetworkAvailable) {
            if (syncedNotesIds.isNotEmpty()) {
                try {
                    api.deleteNotes(
                        syncedNotesIds,
                        "Bearer ${tokenRepository.getToken().token}"
                    )
                    didPerformDeletionAPICall = true
                    noteDao.deleteSelectedNotes(syncedNotesIds)
                    return Resource.Success<Any>()
                } catch (e: Exception) {
                    when (e) {
                        is HttpException -> {
                            val errorBody = e.response()?.errorBody()
                            Log.e("errorBody", errorBody!!.string())
                            val apiError: ApiError = Gson().fromJson(
                                errorBody!!.charStream(),
                                ApiError::class.java
                            )
                            Log.e(
                                "com.vaultsec.vaultsec.repository.NoteRepository.deleteSelectedNotes.HTTP",
                                apiError.error
                            )
                            return Resource.Error<Any>(ErrorTypes.HTTP, apiError.error)
                        }
                        else -> {
                            Log.e(
                                "com.vaultsec.vaultsec.repository.NoteRepository.deleteSelectedNotes.ELSE",
                                e.localizedMessage!!
                            )
                            return Resource.Error<Any>(ErrorTypes.GENERAL)
                        }
                    }
                }
            } else {
                return Resource.Success<Any>()
            }
        } else {
            return Resource.Success<Any>()
        }
    }

    suspend fun undoDeletedNotes(noteList: ArrayList<Note>) {
        val unsyncedNotes = arrayListOf<Note>()
        val syncedNotes = arrayListOf<Note>()
        noteList.map {
            it.isDeleted = false
            if (it.isSynced) {
                syncedNotes.add(it)
            } else {
                unsyncedNotes.add(it)
            }
        }
        noteDao.insertList(unsyncedNotes)
        if (isNetworkAvailable) {
            if (syncedNotes.isNotEmpty()) {
                /*
                * Perform restoration in the server ONLY IF notes were deleted from the server.
                * This is done to avoid duplicating notes
                * */
                if (didPerformDeletionAPICall) {
                    try {
                        val notesApi = api.postRecoverNotes(
                            syncedNotes,
                            "Bearer ${tokenRepository.getToken().token}"
                        )
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
                                    isSynced = true,
                                    id = it.id
                                )
                            )
                        }
                        noteDao.insertList(notes)
                    } catch (e: Exception) {
                        if (didPerformDeletionAPICall) {
                            syncedNotes.map {
                                it.isSynced = false
                            }
                            noteDao.insertList(syncedNotes)
                            didPerformDeletionAPICall = false
                        } else {
                            noteDao.insertList(syncedNotes)
                        }
                        when (e) {
                            is HttpException -> {
                                val errorBody = e.response()?.errorBody()
                                Log.e("errorBody", errorBody!!.string())
                                val apiError: ApiError = Gson().fromJson(
                                    errorBody!!.charStream(),
                                    ApiError::class.java
                                )
                                Log.e(
                                    "com.vaultsec.vaultsec.repository.NoteRepository.undoDeletedNotes.HTTP",
                                    apiError.error
                                )
                            }
                            else -> {
                                Log.e(
                                    "com.vaultsec.vaultsec.repository.NoteRepository.undoDeletedNotes.ELSE",
                                    e.localizedMessage!!
                                )
                            }
                        }
                    }
                } else {
                    noteDao.insertList(syncedNotes)
                }
            }
        } else {
            if (didPerformDeletionAPICall) {
                syncedNotes.map {
                    it.isSynced = false
                }
                noteDao.insertList(syncedNotes)
                didPerformDeletionAPICall = false
            } else {
                noteDao.insertList(syncedNotes)
            }
        }
    }
}

// TODO: 2021-02-17 --------------------------------------------------------------------------------
//  Pass the whole list here for deletion.
//  Split the list into synced and unsynced notes. Unsynced notes can be deleted immediately.
//  For the synced notes try to perform an API request to delete them from the server.
//  If it succeeds: delete the notes inside Room
//  If it fails: mark the isDeleted to true and try to sync later

// TODO: 2021-02-17 --------------------------------------------------------------------------------
//  Upon clicking the undo button, pass the whole list here.
//  Split the list into synced and unsynced notes. Unsynced notes can be restored immediately.
//  For the synced notes try to perform an API request to restore them in the server.
//  If it succeeds: restore the notes inside Room
//  If it fails: mark the isDeleted to false and try to sync later