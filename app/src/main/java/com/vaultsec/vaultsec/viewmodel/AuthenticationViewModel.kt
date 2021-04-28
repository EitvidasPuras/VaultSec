package com.vaultsec.vaultsec.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.repository.AuthenticationRepository
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.hashString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

const val HTTP_NONE_ERROR = 0
const val HTTP_FIRST_NAME_ERROR = 1
const val HTTP_LAST_NAME_ERROR = 2
const val HTTP_EMAIL_ERROR = 3
const val HTTP_PASSWORD_ERROR = 4
const val HTTP_PASSWORD_RE_ERROR = 5

@HiltViewModel
class AuthenticationViewModel
@Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val prefsManager: PasswordManagerPreferences
) : ViewModel() {
//    private val authenticationRepository: AuthenticationRepository = AuthenticationRepository(application)

    private val authenticationEventChannel = Channel<SessionEvent>()
    val authenticationEvent = authenticationEventChannel.receiveAsFlow()

    fun onCreateAccountClick() = viewModelScope.launch(Dispatchers.IO) {
        authenticationEventChannel.send(SessionEvent.NavigateToRegistrationFragment)
    }

    fun onRegistrationResult(result: Boolean) {
        if (result) {
            showSuccessfulRegistrationMessage(R.string.successful_registration)
        }
    }

    private fun showSuccessfulRegistrationMessage(message: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            authenticationEventChannel.send(SessionEvent.ShowSuccessfulRegistrationMessage(message))
        }

    fun onLoginClick(emailInput: String, passInput: String) {
        if (isLoginInputValid(emailInput, passInput)) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowProgressBar(true))
                val user = ApiUser(
                    email = emailInput,
                    password = hashString(passInput, 1)
                )
                Log.e("PASSWORD FOR TESTING PURPOSES", user.password)
                val response: Resource<*> = authenticationRepository.postLogin(user)
                authenticationEventChannel.send(SessionEvent.ShowProgressBar(false))
                if (response is Resource.Success) {
                    authenticationEventChannel.send(SessionEvent.SuccessfulLogin)
                } else {
                    when (response.type) {
                        ErrorTypes.HTTP -> authenticationEventChannel.send(
                            SessionEvent.ShowHttpError(
                                response.message!!,
                                whereToDisplayHttpError(response)
                            )
                        )
                        ErrorTypes.SOCKET_TIMEOUT -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.CONNECTION -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.SOCKET -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_lost)
                        )
                        ErrorTypes.GENERAL -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_generic_connection)
                        )
                    }
                }
            }
        } else {
            return
        }
    }

    private fun whereToDisplayHttpError(response: Resource<*>): Int {
        return when {
            response.message!!.contains("first name", ignoreCase = true) -> {
                HTTP_FIRST_NAME_ERROR
            }
            response.message.contains("last name", ignoreCase = true) -> {
                HTTP_LAST_NAME_ERROR
            }
            response.message.contains("email", ignoreCase = true) -> {
                HTTP_EMAIL_ERROR
            }
            response.message.contains("password confirmation", ignoreCase = true) -> {
                HTTP_PASSWORD_RE_ERROR
            }
            response.message.contains("password format", ignoreCase = true) ->
                HTTP_PASSWORD_ERROR
            else -> {
                HTTP_NONE_ERROR
            }
        }
    }

    private fun isLoginInputValid(emailInput: String, passInput: String): Boolean {
        var noErrors = true
        if (emailInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowEmailInputError(R.string.error_email_required))
            }
            noErrors = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowEmailInputError(R.string.error_email_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsEmail)
            }
        }
        if (passInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordInputError(R.string.error_password_required))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsPassword)
            }
        }
        return noErrors
    }

    fun onRegisterClick(
        firstNameInput: String,
        lastNameInput: String,
        emailInput: String,
        passwordInput: String,
        passwordRetypeInput: String
    ) {
        if (isRegistryInputValid(
                firstNameInput,
                lastNameInput,
                emailInput,
                passwordInput,
                passwordRetypeInput
            )
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowProgressBar(true))
                val user = ApiUser(
                    first_name = firstNameInput,
                    last_name = lastNameInput,
                    email = emailInput,
                    password = hashString(passwordInput, 1),
                    password_confirmation = hashString(passwordRetypeInput, 1)
                )
                val response: Resource<*> = authenticationRepository.postRegister(user)
                authenticationEventChannel.send(SessionEvent.ShowProgressBar(false))
                if (response is Resource.Success) {
                    authenticationEventChannel.send(SessionEvent.SuccessfulRegistration)
                } else {
                    when (response.type) {
                        ErrorTypes.HTTP -> authenticationEventChannel.send(
                            SessionEvent.ShowHttpError(
                                response.message!!,
                                whereToDisplayHttpError(response)
                            )
                        )
                        ErrorTypes.SOCKET_TIMEOUT -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.CONNECTION -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.SOCKET -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_connection_lost)
                        )
                        ErrorTypes.GENERAL -> authenticationEventChannel.send(
                            SessionEvent.ShowRequestError(R.string.error_generic_connection)
                        )
                    }
                }
            }
        } else {
            return
        }
    }

    private fun isRegistryInputValid(
        firstNameInput: String,
        lastNameInput: String,
        emailInput: String,
        passwordInput: String,
        passwordRetypeInput: String
    ): Boolean {
        var noErrors = true

        val letterLowercase = Pattern.compile("[a-z]")
        val letterUppercase = Pattern.compile("[A-Z]")
        val digit = Pattern.compile("[0-9]")
        val specialChar = Pattern.compile("[!@#$%^&*.,()_+=|<>?{}\\[\\]~`-]")

        val lastNameRegex = Pattern.compile("([A-Z][-,a-z. ']+[ ]*)+")

        if (firstNameInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowFirstNameInputError(R.string.error_first_name_required))
            }
            noErrors = false
        } else if (!firstNameInput.chars().allMatch(Character::isLetter)) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowFirstNameInputError(R.string.error_first_name_format))
            }
            noErrors = false
        } else if (firstNameInput.first().isLowerCase()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowFirstNameInputError(R.string.error_name_first_letter))
            }
            noErrors = false
        } else if (firstNameInput.length > 30) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowFirstNameInputError(R.string.error_first_name_length))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsFirstName)
            }
        }

        if (lastNameInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowLastNameInputError(R.string.error_last_name_required))
            }
            noErrors = false
        } else if (lastNameInput.length > 30) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowLastNameInputError(R.string.error_last_name_length))
            }
            noErrors = false
        } else if (lastNameInput.first().isLowerCase()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowLastNameInputError(R.string.error_name_first_letter))
            }
            noErrors = false
        } else if (!lastNameInput.matches(lastNameRegex.toRegex())) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowLastNameInputError(R.string.error_last_name_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsLastName)
            }
        }

        if (emailInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowEmailInputError(R.string.error_email_required))
            }
            noErrors = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowEmailInputError(R.string.error_email_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsEmail)
            }
        }

        if (passwordInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordInputError(R.string.error_password_required))
            }
            noErrors = false
        } else if (passwordInput.length < 10) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordInputError(R.string.error_password_length_short))
            }
            noErrors = false
        } else if (!letterLowercase.matcher(passwordInput).find()
            || !letterUppercase.matcher(passwordInput).find()
            || !digit.matcher(passwordInput).find()
            || !specialChar.matcher(passwordInput).find()
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordInputError(R.string.error_password_format))
            }
            noErrors = false
        } else if (passwordInput.length > 50) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordInputError(R.string.error_password_length_long))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsPassword)
            }
        }

        if (passwordRetypeInput != passwordInput && passwordInput.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ShowPasswordRetypeInputError(R.string.error_password_match))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                authenticationEventChannel.send(SessionEvent.ClearErrorsPasswordRetype)
            }
        }
        return noErrors
    }

    fun isUserLoggedIn() = viewModelScope.launch {
        if (authenticationRepository.isUserLoggedIn()) {
            authenticationEventChannel.send(SessionEvent.CurrentlyLoggedIn)
        }
    }

    sealed class SessionEvent {
        // Handled in LoginFragment
        object NavigateToRegistrationFragment : SessionEvent()

        // Handled in RegistrationFragment
        object ClearErrorsFirstName : SessionEvent()

        // Handled in RegistrationFragment
        object ClearErrorsLastName : SessionEvent()

        // Handled in LoginFragment and RegistrationFragment
        object ClearErrorsEmail : SessionEvent()

        // Handled in LoginFragment and RegistrationFragment
        object ClearErrorsPassword : SessionEvent()

        // Handled in RegistrationFragment
        object ClearErrorsPasswordRetype : SessionEvent()

        // Handled in LoginFragment
        object SuccessfulLogin : SessionEvent()

        // Handled in RegistrationFragment
        object SuccessfulRegistration : SessionEvent()

        // Handled in BottomNavigationActivity
        object SuccessfulLogout : SessionEvent()

        // Handled in StartActivity
        object CurrentlyLoggedIn : SessionEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowProgressBar(val doShow: Boolean) : SessionEvent()

        // Handled in LoginFragment
        data class ShowSuccessfulRegistrationMessage(val message: Int) : SessionEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowHttpError(val message: String, val whereToDisplay: Int) : SessionEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowRequestError(val message: Int) : SessionEvent()

        // Handled in RegistrationFragment
        data class ShowFirstNameInputError(val message: Int) : SessionEvent()

        // Handled in RegistrationFragment
        data class ShowLastNameInputError(val message: Int) : SessionEvent()

        // Handled in LoginFragment and RegistrationFragment
        data class ShowEmailInputError(val message: Int) : SessionEvent()

        // Handled in LoginFragment and RegistrationFragment
        data class ShowPasswordInputError(val message: Int) : SessionEvent()

        // Handled in RegistrationFragment
        data class ShowPasswordRetypeInputError(val message: Int) : SessionEvent()
    }

}