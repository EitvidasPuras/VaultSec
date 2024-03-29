package com.vaultsec.vaultsec.di

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.exclusion.AnnotationExclusionStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/*
* @Module is a way of telling Dagger how to provide dependencies from the dependency graph
* */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /*
    * @Provides indicates that this function has the instruction how to create something
    * */
    @Provides
    fun provideBaseUrl() = "http://192.168.0.104:8001/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, BASE_URL: String): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setExclusionStrategies(AnnotationExclusionStrategy())
            .serializeNulls()
            .create()
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): PasswordManagerApi =
        retrofit.create(PasswordManagerApi::class.java)

    @Provides
    @Singleton
    fun provideEncryptedSharedPrefs(application: Application): SharedPreferences =
        EncryptedSharedPreferences.create(
            application,
            "VaultSecESP",
            MasterKey.Builder(application).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}