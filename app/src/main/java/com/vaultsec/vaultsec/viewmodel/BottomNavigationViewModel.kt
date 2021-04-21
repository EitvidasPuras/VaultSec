package com.vaultsec.vaultsec.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.NotesSortOrder
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.repository.BottomNavigationRepository
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottomNavigationViewModel @Inject constructor(
    private val bottomNavigationRepository: BottomNavigationRepository,
    private val prefsManager: PasswordManagerPreferences
) : ViewModel() {

    private val bottomNavigationEventChannel = Channel<BottomNavigationEvent>()
    val bottomNavigationEvent = bottomNavigationEventChannel.receiveAsFlow()

    fun onLogoutClick(databaseDir: String) = viewModelScope.launch(Dispatchers.IO) {
        bottomNavigationEventChannel.send(BottomNavigationEvent.ShowProgressBar(true))
        val response: Resource<*> = bottomNavigationRepository.postLogout(databaseDir)
        bottomNavigationEventChannel.send(BottomNavigationEvent.ShowProgressBar(false))
        if (response is Resource.Success) {
            bottomNavigationEventChannel.send(BottomNavigationEvent.SuccessfulLogout)
            prefsManager.updateSortOrderForNotes(NotesSortOrder.BY_TITLE) // Reset to default
            prefsManager.updateSortDirectionForNotes(true) // Reset to default
        } else {
            when (response.type) {
                ErrorTypes.HTTP -> bottomNavigationEventChannel.send(
                    BottomNavigationEvent.ShowHttpError(
                        response.message!!,
                        0
                    )
                )
                ErrorTypes.SOCKET_TIMEOUT -> bottomNavigationEventChannel.send(
                    BottomNavigationEvent.ShowRequestError(R.string.error_connection_timed_out)
                )
                ErrorTypes.CONNECTION -> bottomNavigationEventChannel.send(
                    BottomNavigationEvent.ShowRequestError(R.string.error_connection_timed_out)
                )
                ErrorTypes.SOCKET -> bottomNavigationEventChannel.send(
                    BottomNavigationEvent.ShowRequestError(R.string.error_connection_lost)
                )
                ErrorTypes.GENERAL -> bottomNavigationEventChannel.send(
                    BottomNavigationEvent.ShowRequestError(R.string.error_generic_connection)
                )
            }
        }
    }

    fun onLogIn() = viewModelScope.launch(Dispatchers.IO) {
        bottomNavigationEventChannel.send(BottomNavigationEvent.ShowProgressBar(true))
        val response = bottomNavigationRepository.onLogIn()
        if (response is Resource.Success) {
            bottomNavigationEventChannel.send(BottomNavigationEvent.SuccessfulLogin)
        }
        bottomNavigationEventChannel.send(BottomNavigationEvent.ShowProgressBar(false))
    }

    fun printTokenToConsoleForTesting() {
        bottomNavigationRepository.printTokenToConsoleForTesting()
    }

    sealed class BottomNavigationEvent {
        object SuccessfulLogout : BottomNavigationEvent()
        object SuccessfulLogin : BottomNavigationEvent()
        data class ShowProgressBar(val doShow: Boolean) : BottomNavigationEvent()
        data class ShowHttpError(val message: String, val whereToDisplay: Int) :
            BottomNavigationEvent()

        data class ShowRequestError(val message: Int) : BottomNavigationEvent()

    }
}