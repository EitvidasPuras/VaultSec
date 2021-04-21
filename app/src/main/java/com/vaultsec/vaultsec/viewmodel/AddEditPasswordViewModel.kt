package com.vaultsec.vaultsec.viewmodel

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.repository.PasswordRepository
import com.vaultsec.vaultsec.util.SyncType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

const val ADD_PASSWORD_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_PASSWORD_RESULT_OK = Activity.RESULT_FIRST_USER + 1

const val LOGIN_CAMERA_BUTTON = Activity.RESULT_FIRST_USER + 2
const val PASS_CAMERA_BUTTON = Activity.RESULT_FIRST_USER + 3

@HiltViewModel
class AddEditPasswordViewModel
@Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    val password = state.get<Password>("password")

    private val addEditPasswordEventChannel = Channel<AddEditPasswordEvent>()
    val addEditTaskEvent = addEditPasswordEventChannel.receiveAsFlow()

    val URLS = arrayOf(
        "Youtube.com",
        "Facebook.com",
        "Snapchat.com",
        "Instagram.com",
        "Amazon.com",
        "Netflix.com",
        "PayPal.com"
    )
    val CATEGORIES = arrayOf("Unassigned", "Finance", "Social media", "Shopping", "Work")

    var whichTextScanned: Int = -1

    var passwordTitle: String? = state.get<String>("passwordTitle") ?: password?.title ?: ""
        set(value) {
            field = value
            state.set("passwordTitle", value)
        }

    var passwordLogin: String? = state.get<String>("passwordLogin") ?: password?.login ?: ""
        set(value) {
            field = value
            state.set("passwordLogin", value)
        }

    var passwordPassword: String = state.get<String>("passwordPassword") ?: password?.password ?: ""
        set(value) {
            field = value
            state.set("passwordPassword", value)
        }

    var passwordURL: String? = state.get<String>("passwordURL") ?: password?.url ?: ""
        set(value) {
            field = value
            state.set("passwordURL", value)
        }

    var passwordCategory: String =
        state.get<String>("passwordCategory") ?: password?.category ?: "Unassigned"
        set(value) {
            field = value
            state.set("passwordCategory", value)
        }

//    var passwordColor: String = state.get<String>("passwordColor") ?: password?.color ?: ""

    var passwordDateCreated =
        state.get<Timestamp>("passwordDateCreated") ?: password?.createdAt ?: Timestamp(
            System.currentTimeMillis()
        )
        set(value) {
            field = value
            state.set("passwordDateCreated", value)
        }

    var passwordDateUpdated =
        state.get<Timestamp>("passwordDateUpdated") ?: password?.updatedAt ?: Timestamp(
            System.currentTimeMillis()
        )
        set(value) {
            field = value
            state.set("passwordDateUpdated", value)
        }

    var passwordSyncStateInt =
        state.get<Int>("passwordSyncStateInt") ?: password?.syncState ?: SyncType.CREATE_REQUIRED
        set(value) {
            field = value
            state.set("passwordSyncStateInt", value)
        }

    private fun arePasswordsTheSame(): Boolean {
        return (password!!.title == passwordTitle
                && password.login == passwordLogin
                && password.password == passwordPassword
                && password.url == passwordURL
                && password.category == passwordCategory)
    }

    fun onSavePasswordClick() {
        if (passwordPassword.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPasswordEventChannel.send(AddEditPasswordEvent.ShowInvalidInputMessage(R.string.add_edit_password_password_input_error))
            }
            return
        }

        if (password != null) {
            if (arePasswordsTheSame()) {
                viewModelScope.launch(Dispatchers.IO) {
                    addEditPasswordEventChannel.send(AddEditPasswordEvent.NavigateBackWithoutResult)
                }
                return
            } else {
                val updatedPassword = password.copy(
                    title = passwordTitle,
                    login = passwordLogin,
                    password = passwordPassword,
                    url = passwordURL,
                    category = passwordCategory,
                    color = determinePasswordColor()[0],
                    createdAt = passwordDateCreated,
                    updatedAt = passwordDateUpdated,
                    syncState = passwordSyncStateInt
                )
                updatePassword(updatedPassword)
            }
        } else {
            val newPassword = Password(
                title = passwordTitle,
                login = passwordLogin,
                password = passwordPassword,
                url = passwordURL,
                category = passwordCategory,
                color = determinePasswordColor()[0],
                createdAt = passwordDateCreated,
                updatedAt = passwordDateUpdated
            )
            createPassword(newPassword)
        }
    }

    private fun createPassword(newPassword: Password) = viewModelScope.launch(Dispatchers.IO) {
        addEditPasswordEventChannel.send(AddEditPasswordEvent.DoShowLoading(true))
        passwordRepository.insert(newPassword)
        addEditPasswordEventChannel.send(AddEditPasswordEvent.DoShowLoading(false))
        addEditPasswordEventChannel.send(
            AddEditPasswordEvent.NavigateBackWithResult(
                ADD_PASSWORD_RESULT_OK
            )
        )
    }

    private fun updatePassword(updatedPassword: Password) = viewModelScope.launch(Dispatchers.IO) {
        addEditPasswordEventChannel.send(AddEditPasswordEvent.DoShowLoading(true))
        passwordRepository.update(updatedPassword)
        addEditPasswordEventChannel.send(AddEditPasswordEvent.DoShowLoading(false))
        addEditPasswordEventChannel.send(
            AddEditPasswordEvent.NavigateBackWithResult(
                EDIT_PASSWORD_RESULT_OK
            )
        )
    }

    fun onOpenCamera() = viewModelScope.launch(Dispatchers.IO) {
        addEditPasswordEventChannel.send(AddEditPasswordEvent.NavigateToCameraFragment)
    }

    private fun determinePasswordColor(): ArrayList<String> {
        val colorCodes = arrayListOf<String>()

        if (passwordURL?.contains("youtube", true) == true)
            colorCodes.add("#ff0000")
        if (passwordURL?.contains("facebook", true) == true)
            colorCodes.add("#4267b2")
        if (passwordURL?.contains("snapchat", true) == true)
            colorCodes.add("#fffc00")
        if (passwordURL?.contains("instagram", true) == true)
            colorCodes.add("#c13584")
        if (passwordURL?.contains("netflix", true) == true)
            colorCodes.add("#e50914")
        if (passwordURL?.contains("amazon", true) == true)
            colorCodes.add("#ff9900")
        if (passwordURL?.contains("paypal", true) == true)
            colorCodes.add("#0079c1")

        if (colorCodes.isEmpty() || colorCodes.size > 1) {
            colorCodes.clear()
            colorCodes.add("#ffffff")
        }

        return colorCodes
    }

    sealed class AddEditPasswordEvent {
        data class ShowInvalidInputMessage(val message: Int) : AddEditPasswordEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditPasswordEvent()
        object NavigateBackWithoutResult : AddEditPasswordEvent()
        data class DoShowLoading(val visible: Boolean) : AddEditPasswordEvent()
        object NavigateToCameraFragment : AddEditPasswordEvent()
    }
}