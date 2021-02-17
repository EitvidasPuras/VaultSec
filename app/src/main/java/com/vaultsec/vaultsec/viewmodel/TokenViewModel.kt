package com.vaultsec.vaultsec.viewmodel

import android.util.Patterns
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.network.entity.ErrorTypes
import com.vaultsec.vaultsec.repository.TokenRepository
import com.vaultsec.vaultsec.util.hashString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.regex.Pattern

const val HTTP_NONE_ERROR = 0
const val HTTP_FIRST_NAME_ERROR = 1
const val HTTP_LAST_NAME_ERROR = 2
const val HTTP_EMAIL_ERROR = 3
const val HTTP_PASSWORD_ERROR = 4
const val HTTP_PASSWORD_RE_ERROR = 5

class TokenViewModel
@ViewModelInject constructor(
    private val tokenRepository: TokenRepository,
    private val prefsManager: PasswordManagerPreferences
) : ViewModel() {
//    private val tokenRepository: TokenRepository = TokenRepository(application)

    private val tokenEventChannel = Channel<TokenEvent>()
    val tokenEvent = tokenEventChannel.receiveAsFlow()

    fun insert(token: Token) = viewModelScope.launch(Dispatchers.IO) {
        tokenRepository.insert(token)
    }

    fun delete(token: Token) = viewModelScope.launch(Dispatchers.IO) {
        tokenRepository.delete(token)
    }

    fun getToken(): Token {
        return runBlocking {
            tokenRepository.getToken()
        }
    }

    fun onCreateAccountClick() = viewModelScope.launch(Dispatchers.IO) {
        tokenEventChannel.send(TokenEvent.NavigateToRegistrationFragment)
    }

    fun onRegistrationResult(result: Boolean) {
        if (result) {
            showSuccessfulRegistrationMessage(R.string.successful_registration)
        }
    }

    private fun showSuccessfulRegistrationMessage(message: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            tokenEventChannel.send(TokenEvent.ShowSuccessfulRegistrationMessage(message))
        }

    fun onLoginClick(emailInput: String, passInput: String) {
        if (isLoginInputValid(emailInput, passInput)) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowProgressBar(true))
                val user = ApiUser(
                    email = emailInput,
                    password = hashString(passInput)
                )
                val response: ApiResponse<*> = tokenRepository.postLogin(user)
                tokenEventChannel.send(TokenEvent.ShowProgressBar(false))
                if (response is ApiResponse.Success) {
                    tokenEventChannel.send(TokenEvent.SuccessfulLogin)
                } else {
                    when (response.type) {
                        ErrorTypes.HTTP -> tokenEventChannel.send(
                            TokenEvent.ShowHttpError(
                                response.message!!,
                                whereToDisplayHttpError(response)
                            )
                        )
                        ErrorTypes.SOCKET_TIMEOUT -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.CONNECTION -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.SOCKET -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_lost)
                        )
                        ErrorTypes.GENERAL -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_generic_connection)
                        )
                    }
                }
            }
        } else {
            return
        }
    }

    private fun whereToDisplayHttpError(response: ApiResponse<*>): Int {
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
                tokenEventChannel.send(TokenEvent.ShowEmailInputError(R.string.error_email_required))
            }
            noErrors = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowEmailInputError(R.string.error_email_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsEmail)
            }
        }
        if (passInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordInputError(R.string.error_password_required))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsPassword)
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
                tokenEventChannel.send(TokenEvent.ShowProgressBar(true))
                val user = ApiUser(
                    first_name = firstNameInput,
                    last_name = lastNameInput,
                    email = emailInput,
                    password = hashString(passwordInput),
                    password_confirmation = hashString(passwordRetypeInput)
                )
                val response: ApiResponse<*> = tokenRepository.postRegister(user)
                tokenEventChannel.send(TokenEvent.ShowProgressBar(false))
                if (response is ApiResponse.Success) {
                    tokenEventChannel.send(TokenEvent.SuccessfulRegistration)
                } else {
                    when (response.type) {
                        ErrorTypes.HTTP -> tokenEventChannel.send(
                            TokenEvent.ShowHttpError(
                                response.message!!,
                                whereToDisplayHttpError(response)
                            )
                        )
                        ErrorTypes.SOCKET_TIMEOUT -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.CONNECTION -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                        )
                        ErrorTypes.SOCKET -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_connection_lost)
                        )
                        ErrorTypes.GENERAL -> tokenEventChannel.send(
                            TokenEvent.ShowRequestError(R.string.error_generic_connection)
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
                tokenEventChannel.send(TokenEvent.ShowFirstNameInputError(R.string.error_first_name_required))
            }
            noErrors = false
        } else if (!firstNameInput.chars().allMatch(Character::isLetter)) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowFirstNameInputError(R.string.error_first_name_format))
            }
            noErrors = false
        } else if (firstNameInput.first().isLowerCase()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowFirstNameInputError(R.string.error_name_first_letter))
            }
            noErrors = false
        } else if (firstNameInput.length > 30) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowFirstNameInputError(R.string.error_first_name_length))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsFirstName)
            }
        }

        if (lastNameInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowLastNameInputError(R.string.error_last_name_required))
            }
            noErrors = false
        } else if (lastNameInput.length > 30) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowLastNameInputError(R.string.error_last_name_length))
            }
            noErrors = false
        } else if (lastNameInput.first().isLowerCase()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowLastNameInputError(R.string.error_name_first_letter))
            }
            noErrors = false
        } else if (!lastNameInput.matches(lastNameRegex.toRegex())) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowLastNameInputError(R.string.error_last_name_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsLastName)
            }
        }

        if (emailInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowEmailInputError(R.string.error_email_required))
            }
            noErrors = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowEmailInputError(R.string.error_email_format))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsEmail)
            }
        }

        if (passwordInput.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordInputError(R.string.error_password_required))
            }
            noErrors = false
        } else if (passwordInput.length < 10) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordInputError(R.string.error_password_length_short))
            }
            noErrors = false
        } else if (!letterLowercase.matcher(passwordInput).find()
            || !letterUppercase.matcher(passwordInput).find()
            || !digit.matcher(passwordInput).find()
            || !specialChar.matcher(passwordInput).find()
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordInputError(R.string.error_password_format))
            }
            noErrors = false
        } else if (passwordInput.length > 50) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordInputError(R.string.error_password_length_long))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsPassword)
            }
        }

        if (passwordRetypeInput != passwordInput && passwordInput.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ShowPasswordRetypeInputError(R.string.error_password_match))
            }
            noErrors = false
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                tokenEventChannel.send(TokenEvent.ClearErrorsPasswordRetype)
            }
        }
        return noErrors
    }

    fun onLogoutClick() = viewModelScope.launch(Dispatchers.IO) {
        tokenEventChannel.send(TokenEvent.ShowProgressBar(true))
        val response: ApiResponse<*> = tokenRepository.postLogout("Bearer ${getToken().token}")
        tokenEventChannel.send(TokenEvent.ShowProgressBar(false))
        if (response is ApiResponse.Success) {
            prefsManager.updateSortOrder(SortOrder.BY_TITLE) // Reset to default
            prefsManager.updateSortDirection(true) // Reset to default
            tokenEventChannel.send(TokenEvent.SuccessfulLogout)
        } else {
            when (response.type) {
                ErrorTypes.HTTP -> tokenEventChannel.send(
                    TokenEvent.ShowHttpError(
                        response.message!!,
                        whereToDisplayHttpError(response)
                    )
                )
                ErrorTypes.SOCKET_TIMEOUT -> tokenEventChannel.send(
                    TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                )
                ErrorTypes.CONNECTION -> tokenEventChannel.send(
                    TokenEvent.ShowRequestError(R.string.error_connection_timed_out)
                )
                ErrorTypes.SOCKET -> tokenEventChannel.send(
                    TokenEvent.ShowRequestError(R.string.error_connection_lost)
                )
                ErrorTypes.GENERAL -> tokenEventChannel.send(
                    TokenEvent.ShowRequestError(R.string.error_generic_connection)
                )
            }
        }

    }

    sealed class TokenEvent {
        // Handled in LoginFragment
        object NavigateToRegistrationFragment : TokenEvent()

        // Handled in RegistrationFragment
        object ClearErrorsFirstName : TokenEvent()

        // Handled in RegistrationFragment
        object ClearErrorsLastName : TokenEvent()

        // Handled in LoginFragment and RegistrationFragment
        object ClearErrorsEmail : TokenEvent()

        // Handled in LoginFragment and RegistrationFragment
        object ClearErrorsPassword : TokenEvent()

        // Handled in RegistrationFragment
        object ClearErrorsPasswordRetype : TokenEvent()

        // Handled in LoginFragment
        object SuccessfulLogin : TokenEvent()

        // Handled in RegistrationFragment
        object SuccessfulRegistration : TokenEvent()

        // Handled in BottomNavigationActivity
        object SuccessfulLogout : TokenEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowProgressBar(val doShow: Boolean) : TokenEvent()

        // Handled in LoginFragment
        data class ShowSuccessfulRegistrationMessage(val message: Int) : TokenEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowHttpError(val message: String, val whereToDisplay: Int) : TokenEvent()

        // Handled in LoginFragment, RegistrationFragment and BottomNavigationActivity
        data class ShowRequestError(val message: Int) : TokenEvent()

        // Handled in RegistrationFragment
        data class ShowFirstNameInputError(val message: Int) : TokenEvent()

        // Handled in RegistrationFragment
        data class ShowLastNameInputError(val message: Int) : TokenEvent()

        // Handled in LoginFragment and RegistrationFragment
        data class ShowEmailInputError(val message: Int) : TokenEvent()

        // Handled in LoginFragment and RegistrationFragment
        data class ShowPasswordInputError(val message: Int) : TokenEvent()

        // Handled in RegistrationFragment
        data class ShowPasswordRetypeInputError(val message: Int) : TokenEvent()
    }

}