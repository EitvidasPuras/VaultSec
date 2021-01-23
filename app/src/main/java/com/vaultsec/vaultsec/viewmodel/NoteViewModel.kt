package com.vaultsec.vaultsec.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
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

class NoteViewModel
@ViewModelInject constructor(
    private val noteRepository: NoteRepository,
    private val prefsManager: PasswordManagerPreferences
) : ViewModel() {
    /*
    * In case you want to store search query into the SavedStateArguments
    * so that when the app is killed in the background and you return to it the search
    * query would still be there
    * */
//    val searchQuery = state.getLiveData("searchQuery", "")
    val searchQuery = MutableStateFlow("")
    val preferencesFlow = prefsManager.preferencesFlow

    private val notesEventChannel = Channel<NotesEvent>()
    val notesEvent = notesEventChannel.receiveAsFlow()

    private val multiSelectedNotes: ArrayList<Note> = arrayListOf()

    /*
    * With flatMapLatest, when a new Observable is mapped, it overwrites the last Observable
    * if there was one. Combine combines multiple flows into a single flow
    * */
    private val notesFlow = combine(
//        searchQuery.asFlow(),
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

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortOrder(sortOrder)
    }

    fun onSortDirectionSelected(isAsc: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortDirection(isAsc)
    }

    fun onNoteSelection(note: Note) {
        multiSelectedNotes.add(note)
    }

    fun onNoteDeselection(note: Note) {
        multiSelectedNotes.add(note)
    }

    fun onDeleteSelectedNotesClick() = viewModelScope.launch(Dispatchers.IO) {
        if (multiSelectedNotes.isNotEmpty()) {
            val multiSelectedNotesClone = multiSelectedNotes.clone() as ArrayList<Note>
            noteRepository.deleteSelectedNotes(multiSelectedNotesClone)
            notesEventChannel.send(NotesEvent.ShowUndoDeleteNoteMessage(multiSelectedNotesClone))
            multiSelectedNotes.clear()
        }
    }

    fun onMultiSelectActionModeClose() {
        multiSelectedNotes.clear()
    }

    fun onUndoDeleteClick(noteList: ArrayList<Note>) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.insertList(noteList)
    }

    fun onAddNewNoteClick() = viewModelScope.launch(Dispatchers.IO) {
        notesEventChannel.send(NotesEvent.NavigateToAddNoteFragment)
    }

    fun onNoteClicked(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        notesEventChannel.send(NotesEvent.NavigateToEditNoteFragment(note))
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_NOTE_RESULT_OK -> showNoteSavedConfirmationMessage(R.string.add_note_confirmation)
            EDIT_NOTE_RESULT_OK -> showNoteSavedConfirmationMessage(R.string.edit_note_confirmation)
        }
    }

    private fun showNoteSavedConfirmationMessage(message: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            notesEventChannel.send(NotesEvent.ShowNoteSavedConfirmationMessage(message))
        }

    fun onDisplayEmptyRecyclerViewMessage() {
        if (notes.value?.isEmpty() == true) {
            viewModelScope.launch(Dispatchers.IO) {
                notesEventChannel.send(NotesEvent.ShowEmptyRecyclerViewMessage)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                notesEventChannel.send(NotesEvent.HideEmptyRecyclerViewMessage)
            }
        }
    }

    /*
    * Sealed class in case of more events would be added later on
    * Won't be exhaustive to check all the different events using the when statement
    * */
    sealed class NotesEvent {
        object NavigateToAddNoteFragment : NotesEvent()
        data class NavigateToEditNoteFragment(val note: Note) : NotesEvent()
        data class ShowUndoDeleteNoteMessage(val noteList: ArrayList<Note>) : NotesEvent()
        data class ShowNoteSavedConfirmationMessage(val message: Int) : NotesEvent()
        object ShowEmptyRecyclerViewMessage : NotesEvent()
        object HideEmptyRecyclerViewMessage : NotesEvent()
    }
}