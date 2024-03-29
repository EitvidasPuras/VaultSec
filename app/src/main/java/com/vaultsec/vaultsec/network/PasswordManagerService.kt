package com.vaultsec.vaultsec.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/*
* This class is no longer needed cause of Dagger Hilt.
* That's how the class should be created without Dagger Hilt
* */
class PasswordManagerService {

    companion object {
        private const val BASE_URL = "http://192.168.0.104:8001/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: PasswordManagerApi = retrofit.create(PasswordManagerApi::class.java)

}