package com.vaultsec.vaultsec.viewmodel

import android.app.Application
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.repository.NoteRepository
import com.vaultsec.vaultsec.repository.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TokenViewModel
@ViewModelInject constructor(
    private val tokenRepository: TokenRepository,
) : ViewModel()
{
//    private val tokenRepository: TokenRepository = TokenRepository(application)

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

}