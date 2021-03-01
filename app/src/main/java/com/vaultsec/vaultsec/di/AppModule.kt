package com.vaultsec.vaultsec.di

import android.app.Application
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.exclusion.AnnotationExclusionStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/*
* @Module is a way of telling Dagger how to provide dependencies from the dependency graph
* */
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    /*
    * @Provides indicates that this function has the instruction how to create something
    * */
    @Provides
    @Singleton
    fun provideDatabase(application: Application, callback: PasswordManagerDatabase.Callback) =
        Room.databaseBuilder(
            application, PasswordManagerDatabase::class.java, "vaultsec-database"
        )
            .fallbackToDestructiveMigration()
//            .addCallback(callback)
            .build()

    @Provides
    fun provideTokenDao(database: PasswordManagerDatabase) = database.tokenDao()

    @Provides
    fun provideNoteDao(database: PasswordManagerDatabase) = database.noteDao()

    @Provides
    fun provideBaseUrl() = "http://192.168.0.104:8001/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
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
}