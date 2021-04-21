package com.vaultsec.vaultsec.di

import android.app.Application
import androidx.room.Room
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import net.sqlcipher.database.SupportFactory

@Module
@InstallIn(ViewModelComponent::class)
object DatabaseModule {

    @Provides
    @ViewModelScoped
    fun provideDatabase(
        application: Application, callback: PasswordManagerDatabase.Callback,
        encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences
    ) =
        Room.databaseBuilder(
            application, PasswordManagerDatabase::class.java, "vaultsec-database"
        )
            .fallbackToDestructiveMigration()
            .openHelperFactory(
                SupportFactory(
                    (encryptedSharedPrefs.getCredentials()!!.emailHash + encryptedSharedPrefs.getCredentials()!!.passwordHash).toByteArray()
                )
            )
//            .addCallback(callback)
            .build()

    @Provides
    fun provideNoteDao(database: PasswordManagerDatabase) = database.noteDao()

    @Provides
    fun providePasswordDao(database: PasswordManagerDatabase) = database.passwordDao()
}