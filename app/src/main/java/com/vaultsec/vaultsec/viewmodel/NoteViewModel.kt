package com.vaultsec.vaultsec.viewmodel

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.repository.NoteRepository
import com.vaultsec.vaultsec.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

    fun onStart() {
        viewModelScope.launch {
            if (notes.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DIDNT)
            }
        }
    }

    private val refreshAction = MutableLiveData(Refresh.DIDNT)

    private val refreshTriggerChannel = Channel<Refresh>()
    private val refreshTrigger = refreshTriggerChannel.receiveAsFlow()

    private val notesEventChannel = Channel<NotesEvent>()
    val notesEvent = notesEventChannel.receiveAsFlow()

    private val multiSelectedNotes: ArrayList<Note> = arrayListOf()

    private var deletionResponse: Resource<*> = Resource.Loading<Any>()

    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.insert(note)
    }

    /*
    * Both of the solutions below work fine.
    * TODO: Test for performance
    * */
    val notes: LiveData<Resource<List<Note>>> = refreshTrigger.flatMapLatest {
        combine(
            searchQuery,
            preferencesFlow
        ) { query, prefs ->
            Pair(query, prefs)
        }.flatMapLatest { (query, prefs) ->
            noteRepository.synchronizeNotes(
                didRefresh = (it == Refresh.DID),
                searchQuery = query,
                sortOrder = prefs.sortOrder,
                isAsc = prefs.isAsc,
                onFetchComplete = {}
            )
        }
    }.asLiveData()

//    val notes: LiveData<Resource<List<Note>>> = refreshAction.asFlow().flatMapLatest {
//        combine(
//            searchQuery,
//            preferencesFlow
//        ) { query, prefs ->
//            Pair(query, prefs)
//        }.flatMapLatest { (query, prefs) ->
//            noteRepository.synchronizeNotes(
//                didRefresh = (it == Refresh.DID),
//                searchQuery = query,
//                sortOrder = prefs.sortOrder,
//                isAsc = prefs.isAsc,
//                onFetchComplete = {
//                    refreshAction.value = Refresh.DIDNT
//                }
//            )
//        }
//    }.asLiveData()

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
        multiSelectedNotes.remove(note)
    }

    fun onDeleteSelectedNotesClick() = viewModelScope.launch(Dispatchers.IO) {
        if (multiSelectedNotes.isNotEmpty()) {
            val multiSelectedNotesClone = multiSelectedNotes.clone() as ArrayList<Note>
            viewModelScope.launch {
                deletionResponse = noteRepository.deleteSelectedNotes(multiSelectedNotesClone)
                Log.e("deletionResponse", "$deletionResponse")
            }
            notesEventChannel.send(NotesEvent.ShowUndoDeleteNoteMessage(multiSelectedNotesClone))
            multiSelectedNotes.clear()
        }
    }

    fun onMultiSelectActionModeClose() {
        multiSelectedNotes.clear()
    }

    fun onUndoDeleteClick(noteList: ArrayList<Note>) = viewModelScope.launch(Dispatchers.IO) {
        notesEventChannel.send(NotesEvent.DoShowRefreshing(true))
        /*
        * The app continuously executes this part of code for (iterations*delay/1000) seconds while waiting
        * for the noteRepository.deleteSelectedNotes to return a response. Only then does the app
        * try to recover deleted notes.
        * */
        if (deletionResponse is Resource.Loading) {
            for (i in (0..400)) {
                delay(25)
                if (deletionResponse is Resource.Success || deletionResponse is Resource.Error) {
                    noteRepository.undoDeletedNotes(noteList)
                    notesEventChannel.send(NotesEvent.DoShowRefreshing(false))
                    deletionResponse = Resource.Loading<Any>()
                    return@launch
                }
            }
        } else {
            noteRepository.undoDeletedNotes(noteList)
            deletionResponse = Resource.Loading<Any>()
            notesEventChannel.send(NotesEvent.DoShowRefreshing(false))
        }
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
        val isListEmpty = notes.value?.data?.isEmpty()
        if (isListEmpty == true) {
            viewModelScope.launch(Dispatchers.IO) {
                notesEventChannel.send(NotesEvent.DoShowEmptyRecyclerViewMessage(true))
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                notesEventChannel.send(NotesEvent.DoShowEmptyRecyclerViewMessage(false))
            }
        }
    }

    /*
    * Different methods for different synchronization solutions
    * */
    fun onManualNoteSync() {
        viewModelScope.launch {
            if (notes.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DID)
            }
        }
    }

//    fun onManualNoteSync() {
//        if (notes.value is Resource.Loading) return
//        refreshAction.value = Refresh.DID
//    }

    enum class Refresh {
        DID, DIDNT
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
        data class DoShowEmptyRecyclerViewMessage(val visible: Boolean) : NotesEvent()
        data class DoShowRefreshing(val visible: Boolean) : NotesEvent()
    }
}