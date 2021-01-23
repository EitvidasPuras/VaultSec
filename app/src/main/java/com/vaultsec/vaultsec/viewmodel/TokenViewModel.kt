package com.vaultsec.vaultsec.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.repository.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TokenViewModel
@ViewModelInject constructor(
    private val tokenRepository: TokenRepository,
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

    fun postRegister(user: ApiUser): LiveData<ApiResponse> {
        return liveData(Dispatchers.IO) {
            val response = tokenRepository.postRegister(user)
            emit(response)
        }
    }

    fun postLogin(user: ApiUser): LiveData<ApiResponse> {
        return liveData(Dispatchers.IO) {
            val response = tokenRepository.postLogin(user)
            emit(response)
        }
    }

    fun postLogout(): LiveData<ApiResponse> {
        return liveData(Dispatchers.IO) {
            val response = tokenRepository.postLogout("Bearer ${getToken().token}")
            emit(response)
        }
    }

    fun onCreateAccountClick() = viewModelScope.launch(Dispatchers.IO) {
        tokenEventChannel.send(TokenEvent.NavigateToRegistrationFragment)
    }

    fun onRegistrationResult(result: Boolean) {
        if (result){
            showSuccessfulRegistrationMessage(R.string.successful_registration)
        }
    }

    private fun showSuccessfulRegistrationMessage(message: Int) = viewModelScope.launch(Dispatchers.IO) {
        tokenEventChannel.send(TokenEvent.ShowSuccessfulRegistrationMessage(message))
    }

    sealed class TokenEvent {
        object NavigateToRegistrationFragment : TokenEvent()
        data class ShowSuccessfulRegistrationMessage(val message: Int) : TokenEvent()
    }

}