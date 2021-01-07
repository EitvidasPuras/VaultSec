package com.vaultsec.vaultsec.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository = NoteRepository(application)
    private val prefsManager = PasswordManagerPreferences(application)

    val searchQuery = MutableStateFlow("")
    private val sortOrder = prefsManager.preferencesFlow

    private val notesFlow = combine(
        searchQuery,
        sortOrder
    ) { query, sortOrder ->
        Pair(query, sortOrder)
    }.flatMapLatest { (query, sortOrder) ->
        noteRepository.getNotes(query, sortOrder)
    }
    val notes = notesFlow.asLiveData()


    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.delete(note)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.deleteAll()
    }

    fun getItemCount(): Int {
        return runBlocking {
            noteRepository.getItemCount()
        }
    }

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortOrder(sortOrder)
    }
}