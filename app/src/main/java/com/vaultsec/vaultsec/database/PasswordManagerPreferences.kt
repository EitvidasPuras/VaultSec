package com.vaultsec.vaultsec.database

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.createDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class NotesSortOrder {
    BY_TITLE, BY_DATE_CREATED, BY_DATE_UPDATED, BY_COLOR, BY_FONT_SIZE
}

enum class PasswordsSortOrder {
    BY_TITLE, BY_DATE_CREATED, BY_DATE_UPDATED, BY_CATEGORY, BY_COLOR
}

data class FilterPreferencesNotes(
    val notesSortOrder: NotesSortOrder,
    val isAscNotes: Boolean, val passwordsSortOrder: PasswordsSortOrder, val isAscPasswords: Boolean
)
//data class FilterPreferencesPasswords(val passwordsSortOrder: PasswordsSortOrder, val isAsc: Boolean)

@Singleton
class PasswordManagerPreferences
@Inject constructor(
    @ApplicationContext application: Context
) {
    companion object {
        private const val TAG = "com.vaultsec.vaultsec.database.PasswordManagerPreferences"
    }

    private val dataStore = application.createDataStore("user_preferences")

    val preferencesFlow = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e("$TAG.data", it.message!!)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            val sortOrderNotes = NotesSortOrder.valueOf(
                it[PreferencesKeys.SORT_ORDER_NOTES] ?: NotesSortOrder.BY_TITLE.name
            )
            val isAscNotes = it[PreferencesKeys.SORT_DIR_NOTES] ?: true

            val sortOrderPasswords = PasswordsSortOrder.valueOf(
                it[PreferencesKeys.SORT_ORDER_PASSES] ?: PasswordsSortOrder.BY_TITLE.name
            )
            val isAscPasswords = it[PreferencesKeys.SORT_DIR_PASSES] ?: true
            FilterPreferencesNotes(sortOrderNotes, isAscNotes, sortOrderPasswords, isAscPasswords)
        }


    suspend fun updateSortOrderForNotes(notesSortOrder: NotesSortOrder) {
        dataStore.edit {
            it[PreferencesKeys.SORT_ORDER_NOTES] = notesSortOrder.name
        }
    }

    suspend fun updateSortDirectionForNotes(isAsc: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SORT_DIR_NOTES] = isAsc
        }
    }

    suspend fun updateSortOrderForPasswords(passwordsSortOrder: PasswordsSortOrder) {
        dataStore.edit {
            it[PreferencesKeys.SORT_ORDER_PASSES] = passwordsSortOrder.name
        }
    }

    suspend fun updateSortDirectionForPasswords(isAsc: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SORT_DIR_PASSES] = isAsc
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER_NOTES = stringPreferencesKey("son")
        val SORT_DIR_NOTES = booleanPreferencesKey("sdn")
        val SORT_ORDER_PASSES = stringPreferencesKey("sop")
        val SORT_DIR_PASSES = booleanPreferencesKey("sdp")
    }
}