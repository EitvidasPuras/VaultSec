package com.vaultsec.vaultsec.viewmodel

import android.app.Activity
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp

const val ADD_NOTE_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_NOTE_RESULT_OK = Activity.RESULT_FIRST_USER + 1

class AddEditNoteViewModel
@ViewModelInject constructor(
    private val noteRepository: NoteRepository,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {

    val note = state.get<Note>("note")

    private val addEditNoteEventChannel = Channel<AddEditNoteEvent>()
    val addEditTaskEvent = addEditNoteEventChannel.receiveAsFlow()


    var noteTitle: String? = state.get<String>("noteTitle") ?: note?.title ?: ""
        set(value) {
            field = value
            state.set("noteTitle", value)
        }

    var noteText = state.get<String>("noteText") ?: note?.text ?: ""
        set(value) {
            field = value
            state.set("noteText", value)
        }

    var noteColor = state.get<String>("noteColor") ?: note?.color ?: "#ffffff"
        set(value) {
            field = value
            state.set("noteColor", value)
        }

    var noteFontSize = state.get<Int>("noteFontSize") ?: note?.fontSize ?: 16
        set(value) {
            field = value
            state.set("noteFontSize", value)
        }

    var noteDateCreated = state.get<Timestamp>("noteDateCreated") ?: note?.createdAt ?: Timestamp(
        System.currentTimeMillis()
    )
        set(value) {
            field = value
            state.set("noteDateCreated", value)
        }

    var noteDateUpdated = state.get<Timestamp>("noteDateUpdated") ?: note?.updatedAt ?: Timestamp(
        System.currentTimeMillis()
    )
        set(value) {
            field = value
            state.set("noteDateUpdated", value)
        }

    private fun areNotesTheSame(): Boolean {
        return (note!!.title == noteTitle
                && note.text == noteText
                && note.color == noteColor
                && note.fontSize == noteFontSize
                && note.createdAt == noteDateCreated)
    }

    fun onSaveNoteClick() {
        if (noteText.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditNoteEventChannel.send(AddEditNoteEvent.ShowInvalidInputMessage(R.string.add_edit_note_text_input_error))
            }
            return // To stop executing the code after the error
        }

        if (note != null) {
            if (areNotesTheSame()) {
                viewModelScope.launch(Dispatchers.IO) {
                    addEditNoteEventChannel.send(AddEditNoteEvent.NavigateBackWithoutResult)
                }
                return
            } else {
                val updatedNote = note.copy(
                    title = noteTitle,
                    text = noteText,
                    color = noteColor,
                    fontSize = noteFontSize,
                    createdAt = noteDateCreated,
                    updatedAt = noteDateUpdated,
                    synced = false
                )
                updateNote(updatedNote)
            }
        } else {
            val newNote = Note(
                title = noteTitle,
                text = noteText,
                color = noteColor,
                fontSize = noteFontSize,
                createdAt = noteDateCreated,
                updatedAt = noteDateUpdated
            )
            createNote(newNote)
        }
    }

    private fun createNote(newNote: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.insert(newNote)
        addEditNoteEventChannel.send(AddEditNoteEvent.NavigateBackWithResult(ADD_NOTE_RESULT_OK))
    }

    private fun updateNote(updatedNote: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.update(updatedNote)
        addEditNoteEventChannel.send(AddEditNoteEvent.NavigateBackWithResult(EDIT_NOTE_RESULT_OK))
    }

    sealed class AddEditNoteEvent {
        data class ShowInvalidInputMessage(val message: Int) : AddEditNoteEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditNoteEvent()
        object NavigateBackWithoutResult : AddEditNoteEvent()
    }
}