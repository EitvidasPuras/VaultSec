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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository = NoteRepository(application)
    private val prefsManager = PasswordManagerPreferences(application)

    val searchQuery = MutableStateFlow("")
    val preferencesFlow = prefsManager.preferencesFlow

    private val notesEventChannel = Channel<NotesEvent>()
    val notesEvent = notesEventChannel.receiveAsFlow()

    private val notesFlow = combine(
        searchQuery,
        preferencesFlow
    ) { query, prefs ->
        Pair(query, prefs)
    }.flatMapLatest { (query, prefs) ->
        noteRepository.getNotes(query, prefs.sortOrder, prefs.isAsc)
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

    fun onSortDirectionSelected(isAsc: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortDirection(isAsc)
    }

    fun deleteSelectedNotes(noteList: ArrayList<Note>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.deleteSelectedNotes(noteList)
        notesEventChannel.send(NotesEvent.ShowUndoDeleteNoteMessage(noteList))
    }

    fun onUndoDeleteClick(noteList: ArrayList<Note>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.insertList(noteList)
    }

    // Sealed class in case of more events would be added later on
    // Won't be exhaustive to check all the different events using the when statement
    sealed class NotesEvent {
        data class ShowUndoDeleteNoteMessage(val noteList: ArrayList<Note>) : NotesEvent()
    }
}