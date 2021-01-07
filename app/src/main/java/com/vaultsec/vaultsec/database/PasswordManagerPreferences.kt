package com.vaultsec.vaultsec.database

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class SortOrder {
    BY_TITLE, BY_DATE_CREATED, BY_DATE_UPDATED, BY_COLOR
}

class PasswordManagerPreferences(application: Application) {

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
            sortOrder
        }


    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit {
            it[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = preferencesKey<String>("sort_order")
    }


}