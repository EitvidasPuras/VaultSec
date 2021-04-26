package com.vaultsec.vaultsec.repository

import android.util.Log
import androidx.room.withTransaction
import com.vaultsec.vaultsec.database.NotesSortOrder
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import com.vaultsec.vaultsec.util.cipher.CipherManager
import com.vaultsec.vaultsec.util.isNetworkAvailable
import com.vaultsec.vaultsec.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences,
    private val cm: CipherManager
) {
    private val noteDao = db.noteDao()

    private var didPerformDeletionAPICall: Boolean = false
    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.NoteRepository"
    }

    suspend fun insert(note: Note) {
        if (isNetworkAvailable) {
            try {
                var newNote = note.copy(
                    title = cm.encrypt(note.title),
                    text = cm.encrypt(note.text)!!,
                    color = note.color,
                    fontSize = note.fontSize,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt
                )
                val id = api.postSingleNote(newNote, "Bearer ${encryptedSharedPrefs.getToken()}")
                newNote = note.copy(
                    title = note.title,
                    text = note.text,
                    color = note.color,
                    fontSize = note.fontSize,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    syncState = SyncType.NOTHING_REQUIRED,
                    id = id
                )
                noteDao.insert(newNote)
            } catch (e: Exception) {
                noteDao.insert(note)
                when (e) {
                    is HttpException -> {
                        apiError = JSONObject()
                        apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                        Log.e(
                            "errorBody",
                            apiError.toString()
                        )
                        Log.e(
                            "$TAG.insert.HTTP",
                            apiError.getString("error")
                        )
                    }
                    else -> {
                        Log.e(
                            "$TAG.insert.ELSE",
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
        if (isNetworkAvailable) {
            try {
                if (note.syncState == SyncType.NOTHING_REQUIRED || note.syncState == SyncType.UPDATE_REQUIRED) {
                    var newNote = note.copy(
                        title = cm.encrypt(note.title),
                        text = cm.encrypt(note.text)!!,
                        color = note.color,
                        fontSize = note.fontSize,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt
                    )

                    val id = api.putNoteUpdate(
                        note.id,
                        newNote,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    newNote = note.copy(
                        title = note.title,
                        text = note.text,
                        color = note.color,
                        fontSize = note.fontSize,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = id
                    )
                    noteDao.update(newNote)
                } else {
                    /*
                    * Insert it since it isn't synced with the server
                    * */
                    insert(note)
                }
            } catch (e: Exception) {
                if (note.syncState == SyncType.NOTHING_REQUIRED || note.syncState == SyncType.UPDATE_REQUIRED) {
                    note.syncState = SyncType.UPDATE_REQUIRED
                    noteDao.update(note)
                } else {
                    insert(note)
                }
                when (e) {
                    is HttpException -> {
                        apiError = JSONObject()
                        apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                        Log.e(
                            "errorBody",
                            apiError.toString()
                        )
                        Log.e(
                            "$TAG.update.HTTP",
                            apiError.getString("error")
                        )
                    }
                    else -> {
                        Log.e(
                            "$TAG.update.ELSE",
                            e.localizedMessage!!
                        )
                    }
                }
            }
        } else {
            if (note.syncState == SyncType.NOTHING_REQUIRED || note.syncState == SyncType.UPDATE_REQUIRED) {
                note.syncState = SyncType.UPDATE_REQUIRED
                noteDao.update(note)
            } else {
                insert(note)
            }
        }
    }

    suspend fun deleteAll() {
        noteDao.deleteAll()
    }

    fun synchronizeNotes(
        didRefresh: Boolean,
        searchQuery: String,
        notesSortOrder: NotesSortOrder,
        isAsc: Boolean,
        onFetchComplete: () -> Unit
    ): Flow<Resource<List<Note>>> =
        networkBoundResource(
            query = {
                noteDao.getNotes(searchQuery, notesSortOrder, isAsc)
            },
            fetch = {
                if (noteDao.getSyncedDeletedNotesIds().first().isNotEmpty()) {
                    api.deleteNotes(
                        noteDao.getSyncedDeletedNotesIds().first() as ArrayList<Int>,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                }
                val combinedNotes = arrayListOf<Note>()
                combinedNotes.addAll(noteDao.getSyncedUpdatedNotes().first())
                combinedNotes.addAll(noteDao.getUnsyncedNotes().first())
//                Log.e("combinedNotes", "$combinedNotes")
                combinedNotes.map {
                    it.title = cm.encrypt(it.title)
                    it.text = cm.encrypt(it.text)!!
                }
                val notes = api.postStoreNotes(
                    combinedNotes,
                    "Bearer ${encryptedSharedPrefs.getToken()}"
                )
//                Log.e("returned notes", "$notes")
                notes
            },
            saveFetchResult = { notesApi ->
                val notes = arrayListOf<Note>()
                notesApi.map {
                    notes.add(
                        Note(
                            title = cm.decrypt(it.title),
                            text = cm.decrypt(it.text)!!,
                            color = it.color,
                            fontSize = it.font_size,
                            createdAt = it.created_at_device,
                            updatedAt = it.updated_at_device,
                            syncState = SyncType.NOTHING_REQUIRED,
                            id = it.id
                        )
                    )
                }
                /*
                * withTransaction either performs all the code inside the block or none of it.
                * If one operation fails - it rolls back and doesn't change the database
                * */
                db.withTransaction {
                    noteDao.deleteAll()
                    noteDao.insertList(notes)
                }
            },
            shouldFetch = {
                if (didRefresh) {
                    if (isNetworkAvailable) {
                        noteDao.getUnsyncedNotes().first().isNotEmpty() ||
                                noteDao.getSyncedDeletedNotesIds().first().isNotEmpty() ||
                                noteDao.getSyncedUpdatedNotes().first().isNotEmpty()
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
            onFetchSuccess = onFetchComplete,
            onFetchFailed = onFetchComplete
        )

    suspend fun deleteSelectedNotes(noteList: ArrayList<Note>): Resource<Any> {
        didPerformDeletionAPICall = false
        /*
        * Creating a copy of the list, so that the original list that is passed here wouldn't be affected
        * by the changes made. Have to copy an actual Note object, not just the list itself
        * */
        val noteListCopy = arrayListOf<Note>()
        with(noteList.iterator()) {
            forEach {
                noteListCopy.add(it.copy())
            }
        }

        val unsyncedNotesIds = arrayListOf<Int>()
        val syncedNotesIds = arrayListOf<Int>()
        val syncedNotes = arrayListOf<Note>()

        noteListCopy.map {
            /*
            * Already deleted notes (syncStateInt = 2) should never come here
            * */
            if (it.syncState == SyncType.NOTHING_REQUIRED || it.syncState == SyncType.UPDATE_REQUIRED) {
                it.syncState = SyncType.DELETE_REQUIRED
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
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    didPerformDeletionAPICall = true
                    noteDao.deleteSelectedNotes(syncedNotesIds)
                    return Resource.Success()
                } catch (e: Exception) {
                    when (e) {
                        is HttpException -> {
                            apiError = JSONObject()
                            apiError =
                                JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                            Log.e(
                                "errorBody",
                                apiError.toString()
                            )
                            Log.e(
                                "$TAG.deleteSelectedNotes.HTTP",
                                apiError.getString("error")
                            )
                            return Resource.Error()
                        }
                        else -> {
                            Log.e(
                                "$TAG.deleteSelectedNotes.ELSE",
                                e.localizedMessage!!
                            )
                            return Resource.Error()
                        }
                    }
                }
            } else {
                return Resource.Success()
            }
        } else {
            return Resource.Success()
        }
    }

    suspend fun undoDeletedNotes(noteList: ArrayList<Note>) {
        Log.e("Notes that come to undoDelete", "$noteList")
        val unsyncedNotes = arrayListOf<Note>()
        val syncedNotes = arrayListOf<Note>()
        val bothNotesCombinedForSmoothInsertion = arrayListOf<Note>()
        noteList.map {
//            it.isDeleted = false
//            if (it.isSynced) {
//                syncedNotes.add(it)
//            } else {
//                unsyncedNotes.add(it)
//            }
            when (it.syncState) {
                SyncType.UPDATE_REQUIRED, SyncType.NOTHING_REQUIRED -> {
                    syncedNotes.add(it)
                }
                SyncType.CREATE_REQUIRED -> {
                    unsyncedNotes.add(it)
                }
                else -> Log.e(
                    "$TAG.undoDeletedNotes.WHEN",
                    "Invalid operation"
                )
            }
        }
        if (isNetworkAvailable) {
            if (syncedNotes.isNotEmpty()) {
                /*
                * Perform restoration in the server ONLY IF notes were deleted from the server.
                * This is done to avoid duplicating notes
                * */
                if (didPerformDeletionAPICall) {
                    try {
                        bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                        bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                        bothNotesCombinedForSmoothInsertion.map {
                            it.title = cm.encrypt(it.title)
                            it.text = cm.encrypt(it.text)!!
                        }
                        val notesApi = api.postRecoverNotes(
                            bothNotesCombinedForSmoothInsertion,
                            "Bearer ${encryptedSharedPrefs.getToken()}"
                        )
                        val notes = arrayListOf<Note>()
                        notesApi.map {
                            notes.add(
                                Note(
                                    title = cm.decrypt(it.title),
                                    text = cm.decrypt(it.text)!!,
                                    color = it.color,
                                    fontSize = it.font_size,
                                    createdAt = it.created_at_device,
                                    updatedAt = it.updated_at_device,
                                    syncState = SyncType.NOTHING_REQUIRED,
                                    id = it.id
                                )
                            )
                        }
                        noteDao.insertList(notes)
                    } catch (e: Exception) {
                        if (didPerformDeletionAPICall) {
                            syncedNotes.map {
                                it.syncState = SyncType.CREATE_REQUIRED
                            }
                            bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                            bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                            noteDao.insertList(bothNotesCombinedForSmoothInsertion)
                            didPerformDeletionAPICall = false
                        } else {
                            bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                            bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                            noteDao.insertList(bothNotesCombinedForSmoothInsertion)
                        }
                        when (e) {
                            is HttpException -> {
                                apiError = JSONObject()
                                apiError =
                                    JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                                Log.e(
                                    "errorBody",
                                    apiError.toString()
                                )
                                Log.e(
                                    "$TAG.undoDeletedNotes.HTTP",
                                    apiError.getString("error")
                                )
                            }
                            else -> {
                                Log.e(
                                    "$TAG.undoDeletedNotes.ELSE",
                                    e.localizedMessage!!
                                )
                            }
                        }
                    }
                } else {
                    bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                    bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                    noteDao.insertList(bothNotesCombinedForSmoothInsertion)
                }
            } else {
                if (unsyncedNotes.isNotEmpty()) {
                    unsyncedNotes.map {
                        it.title = cm.encrypt(it.title)
                        it.text = cm.encrypt(it.text)!!
                    }
                    val notesApi = api.postRecoverNotes(
                        unsyncedNotes,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    val notes = arrayListOf<Note>()
                    notesApi.map {
                        notes.add(
                            Note(
                                title = cm.decrypt(it.title),
                                text = cm.decrypt(it.text)!!,
                                color = it.color,
                                fontSize = it.font_size,
                                createdAt = it.created_at_device,
                                updatedAt = it.updated_at_device,
                                syncState = SyncType.NOTHING_REQUIRED,
                                id = it.id
                            )
                        )
                    }
                    noteDao.insertList(notes)
                }
            }
        } else {
            if (syncedNotes.isNotEmpty()) {
                if (didPerformDeletionAPICall) {
                    syncedNotes.map {
                        it.syncState = SyncType.CREATE_REQUIRED
                    }
                    bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                    bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                    noteDao.insertList(bothNotesCombinedForSmoothInsertion)
                    didPerformDeletionAPICall = false
                } else {
                    bothNotesCombinedForSmoothInsertion.addAll(unsyncedNotes)
                    bothNotesCombinedForSmoothInsertion.addAll(syncedNotes)
                    noteDao.insertList(bothNotesCombinedForSmoothInsertion)
                }
            } else {
                noteDao.insertList(unsyncedNotes)
            }
        }
    }
}