package com.vaultsec.vaultsec.database

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder {
    BY_TITLE, BY_DATE_CREATED, BY_DATE_UPDATED, BY_COLOR, BY_FONT_SIZE
}

data class FilterPreferences(val sortOrder: SortOrder, val isAsc: Boolean)

@Singleton
class PasswordManagerPreferences
@Inject constructor(
    @ApplicationContext application: Context
) {

    private val dataStore = application.createDataStore("user_preferences")

    val preferencesFlow = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e("com.vaultsec.vaultsec.database.PasswordManagerPreferences", it.message!!)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            val sortOrder = SortOrder.valueOf(
                it[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_TITLE.name
            )
            val isAsc = it[PreferencesKeys.SORT_DIR] ?: true
            FilterPreferences(sortOrder, isAsc)
        }


    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit {
            it[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateSortDirection(isAsc: Boolean) {
        dataStore.edit {
            it[PreferencesKeys.SORT_DIR] = isAsc
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = preferencesKey<String>("so")
        val SORT_DIR = preferencesKey<Boolean>("sd")
    }


}