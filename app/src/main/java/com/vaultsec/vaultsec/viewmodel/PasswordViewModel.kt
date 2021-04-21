package com.vaultsec.vaultsec.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.PasswordsSortOrder
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.repository.PasswordRepository
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.cipher.CipherManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordViewModel
@Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val prefsManager: PasswordManagerPreferences,
    private val cipherManager: CipherManager
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val preferencesFlow = prefsManager.preferencesFlow

    private val passwordsEventChannel = Channel<PasswordEvent>()
    val passwordsEvent = passwordsEventChannel.receiveAsFlow()

    private val deletePasswordsState = MutableStateFlow<Resource<*>>(Resource.Empty<Any>())
    private var deletionResponse: Resource<*> = Resource.Loading<Any>()

    private val refreshTriggerChannel = Channel<Refresh>()
    private val refreshTrigger = refreshTriggerChannel.receiveAsFlow()

    private val multiSelectedPasswords: ArrayList<Password> = arrayListOf()

    val passwords: LiveData<Resource<List<Password>>> = refreshTrigger.flatMapLatest {
        combine(
            searchQuery,
            preferencesFlow
        ) { query, prefs ->
            Pair(query, prefs)
        }.flatMapLatest { (query, prefs) ->
            passwordRepository.synchronizePasswords(
                didRefresh = (it == Refresh.DID),
                searchQuery = query,
                sortOrder = prefs.passwordsSortOrder,
                isAsc = prefs.isAscPasswords,
                onFetchComplete = {
                    viewModelScope.launch(Dispatchers.IO) {
                        refreshTriggerChannel.send(Refresh.DIDNT)
                    }
                }
            )
        }
    }.asLiveData()

    fun onStart() {
        viewModelScope.launch {
            if (passwords.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DIDNT)
            }
        }
    }

    fun onSortOrderSelected(sortOrder: PasswordsSortOrder) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortOrderForPasswords(sortOrder)
    }

    fun onSortDirectionSelected(isAsc: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortDirectionForPasswords(isAsc)
    }

    fun onPasswordSelection(password: Password) {
        multiSelectedPasswords.add(password)
    }

    fun onPasswordDeselection(password: Password) {
        multiSelectedPasswords.remove(password)
    }

    fun onDeleteSelectedPasswordsClick() = viewModelScope.launch {
        deletePasswordsState.value = Resource.Loading<Any>()
        if (multiSelectedPasswords.isNotEmpty()) {
            val multiSelectedPasswordsClone = multiSelectedPasswords.clone() as ArrayList<Password>
            viewModelScope.launch(Dispatchers.IO) {
                deletePasswordsState.value =
                    passwordRepository.deleteSelectedPasswords(multiSelectedPasswordsClone)
            }
            passwordsEventChannel.send(
                PasswordEvent.ShowUndoDeletePasswordMessage(
                    multiSelectedPasswordsClone
                )
            )
            multiSelectedPasswords.clear()
        }
    }

    fun onMultiSelectActionModeClose() {
        multiSelectedPasswords.clear()
    }

    fun onUndoDeleteClick(passwordList: ArrayList<Password>) =
        viewModelScope.launch(Dispatchers.IO) {
            passwordsEventChannel.send(PasswordEvent.DoShowRefreshing(true))

            if (deletePasswordsState.value is Resource.Loading) {
                viewModelScope.launch {
                    deletePasswordsState.collect {
                        when (it) {
                            is Resource.Success -> {
                                passwordRepository.undoDeletedPasswords(passwordList)
                                deletePasswordsState.value = Resource.Empty<Any>()
                                passwordsEventChannel.send(PasswordEvent.DoShowRefreshing(false))
                                cancel()
                            }
                            is Resource.Error -> {
                                passwordRepository.undoDeletedPasswords(passwordList)
                                deletePasswordsState.value = Resource.Empty<Any>()
                                passwordsEventChannel.send(PasswordEvent.DoShowRefreshing(false))
                                cancel()
                            }
                        }
                    }
                }
            } else {
                passwordRepository.undoDeletedPasswords(passwordList)
                deletionResponse = Resource.Loading<Any>()
                passwordsEventChannel.send(PasswordEvent.DoShowRefreshing(false))
            }
        }

    fun onAddNewPasswordClick() = viewModelScope.launch(Dispatchers.IO) {
        passwordsEventChannel.send(PasswordEvent.NavigateToAddPasswordFragment)
    }

    fun onPasswordClicked(password: Password) = viewModelScope.launch(Dispatchers.IO) {
        passwordsEventChannel.send(PasswordEvent.NavigateToEditPasswordFragment(password))
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_PASSWORD_RESULT_OK -> showPasswordSavedConfirmationMessage(R.string.add_password_confirmation)
            EDIT_PASSWORD_RESULT_OK -> showPasswordSavedConfirmationMessage(R.string.edit_password_confirmation)
        }
    }

    private fun showPasswordSavedConfirmationMessage(message: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            passwordsEventChannel.send(PasswordEvent.ShowPasswordSavedConfirmationMessage(message))
        }

    fun onManualPasswordSync() {
        viewModelScope.launch {
            if (passwords.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DID)
            }
        }
    }

    enum class Refresh {
        DID, DIDNT
    }

    sealed class PasswordEvent {
        object NavigateToAddPasswordFragment : PasswordEvent()
        data class NavigateToEditPasswordFragment(val password: Password) : PasswordEvent()
        data class ShowUndoDeletePasswordMessage(val passwordList: ArrayList<Password>) :
            PasswordEvent()

        data class ShowPasswordSavedConfirmationMessage(val message: Int) : PasswordEvent()
        data class DoShowRefreshing(val visible: Boolean) : PasswordEvent()
    }
}