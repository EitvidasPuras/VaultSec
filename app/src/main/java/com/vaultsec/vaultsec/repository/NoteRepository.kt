package com.vaultsec.vaultsec.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.dao.NoteDao
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.network.PasswordManagerService

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

    fun getAllNotes(): LiveData<List<Note>> {
        return noteDao.getAllNotes()
    }

    suspend fun getItemCount(): Int {
        return noteDao.getItemCount()
    }
}