package com.vaultsec.vaultsec.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.database.entity.Token
import com.vaultsec.vaultsec.network.entity.ApiResponse
import com.vaultsec.vaultsec.network.entity.ApiUser
import com.vaultsec.vaultsec.repository.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TokenViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenRepository: TokenRepository = TokenRepository(application)
//    private var token: Token
//
//    init {
//        viewModelScope.launch(Dispatchers.IO) {
//            token = tokenRepository.getToken()
//        }
//    }


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

    fun postLogout(header: String): LiveData<ApiResponse> {
        return liveData(Dispatchers.IO) {
            val response = tokenRepository.postLogout(header)
            emit(response)
        }
    }

}