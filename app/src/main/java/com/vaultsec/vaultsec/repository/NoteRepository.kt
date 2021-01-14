package com.vaultsec.vaultsec.repository

import android.app.Application
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.PasswordManagerService
import kotlinx.coroutines.flow.Flow

class NoteRepository(application: Application) {
    private val database = PasswordManagerDatabase.getInstance(application)
    private val noteDao: NoteDao = database.noteDao()
    private val api: PasswordManagerApi = PasswordManagerService().apiService

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

    fun getNotes(searchQuery: String, sortOrder: SortOrder, isAsc: Boolean): Flow<List<Note>> {
        return noteDao.getNotes(searchQuery, sortOrder, isAsc)
    }

    suspend fun getItemCount(): Int {
        return noteDao.getItemCount()
    }

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