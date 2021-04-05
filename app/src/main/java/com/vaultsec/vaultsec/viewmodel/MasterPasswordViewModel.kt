package com.vaultsec.vaultsec.viewmodel

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.util.hashString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MasterPasswordViewModel
@Inject constructor(
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences
) : ViewModel() {

    private val masterPasswordEventChannel = Channel<MasterPasswordEvent>()
    val masterPasswordEvent = masterPasswordEventChannel.receiveAsFlow()

    fun onMasterUnlockClick(passwordInput: String) {
        viewModelScope.launch {
            masterPasswordEventChannel.send(MasterPasswordEvent.ShowProgressBar(true))
            if (isMasterPasswordValid(passwordInput)) {
                viewModelScope.launch {
                    masterPasswordEventChannel.send(MasterPasswordEvent.ShowProgressBar(false))
                    masterPasswordEventChannel.send(MasterPasswordEvent.SuccessfulUnlock)
                }
            } else {
                masterPasswordEventChannel.send(MasterPasswordEvent.ShowProgressBar(false))
                return@launch
            }
        }
    }

    private fun isMasterPasswordValid(passwordInput: String): Boolean {
        var noErrors = true

        if (passwordInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                masterPasswordEventChannel.send(MasterPasswordEvent.ShowMasterPasswordInputError(R.string.error_password_required))
            }
            noErrors = false
        } else if (!doPasswordsMatch(hashString(passwordInput, 2))) {
            viewModelScope.launch(Dispatchers.IO) {
                masterPasswordEventChannel.send(MasterPasswordEvent.ShowMasterPasswordInputError(R.string.error_master_password_incorrect))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                masterPasswordEventChannel.send(MasterPasswordEvent.ClearErrors)
            }
        }
        return noErrors
    }

    private fun doPasswordsMatch(passwordHash: String): Boolean {
        val passwordFromEncryptedSharedPrefs =
            encryptedSharedPrefs.getCredentials()?.passwordHash ?: return false
        return passwordFromEncryptedSharedPrefs == passwordHash
    }

    sealed class MasterPasswordEvent {
        object ClearErrors : MasterPasswordEvent()
        object SuccessfulUnlock : MasterPasswordEvent()
        data class ShowMasterPasswordInputError(val message: Int) : MasterPasswordEvent()
        data class ShowProgressBar(val doShow: Boolean) : MasterPasswordEvent()

    }
}