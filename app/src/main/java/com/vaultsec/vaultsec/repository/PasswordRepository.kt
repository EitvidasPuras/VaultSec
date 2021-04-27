package com.vaultsec.vaultsec.repository

import android.util.Log
import androidx.room.withTransaction
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.PasswordsSortOrder
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import com.vaultsec.vaultsec.util.cipher.CipherManager
import com.vaultsec.vaultsec.util.isNetworkAvailable
import com.vaultsec.vaultsec.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class PasswordRepository @Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences,
    private val cm: CipherManager
) {
    private val passwordDao = db.passwordDao()

    private var didPerformDeletionAPICall: Boolean = false
    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.PasswordRepository"
    }

    suspend fun insert(password: Password) {
        if (isNetworkAvailable) {
            try {
                var newPassword = password.copy(
                    title = cm.encrypt(password.title),
                    url = password.url,
                    login = cm.encrypt(password.login),
                    password = cm.encrypt(password.password)!!,
                    category = password.category,
                    color = password.color,
                    createdAt = password.createdAt,
                    updatedAt = password.updatedAt
                )
                val id =
                    api.postSinglePassword(newPassword, "Bearer ${encryptedSharedPrefs.getToken()}")
                newPassword = password.copy(
                    title = password.title,
                    url = password.url,
                    login = password.login,
                    password = password.password,
                    category = password.category,
                    color = password.color,
                    createdAt = password.createdAt,
                    updatedAt = password.updatedAt,
                    syncState = SyncType.NOTHING_REQUIRED,
                    id = id
                )
                passwordDao.insert(newPassword)
            } catch (e: Exception) {
                passwordDao.insert(password)
                when (e) {
                    is HttpException -> {
                        apiError = JSONObject()
                        apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                        Log.e(
                            "errorBody",
                            apiError.toString()
                        )
                        Log.e(
                            "$TAG.insert.HTTP",
                            apiError.getString("error")
                        )
                    }
                    else -> {
                        Log.e(
                            "$TAG.insert.ELSE",
                            e.localizedMessage!!
                        )
                    }
                }
            }
        } else {
            passwordDao.insert(password)
        }
    }

    suspend fun update(password: Password) {
        if (isNetworkAvailable) {
            try {
                if (password.syncState == SyncType.NOTHING_REQUIRED || password.syncState == SyncType.UPDATE_REQUIRED) {
                    var newPassword = password.copy(
                        title = cm.encrypt(password.title),
                        url = password.url,
                        login = cm.encrypt(password.login),
                        password = cm.encrypt(password.password)!!,
                        category = password.category,
                        color = password.color,
                        createdAt = password.createdAt,
                        updatedAt = password.updatedAt
                    )
                    val id = api.putPasswordUpdate(
                        password.id,
                        newPassword,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    newPassword = password.copy(
                        title = password.title,
                        url = password.url,
                        login = password.login,
                        password = password.password,
                        category = password.category,
                        color = password.color,
                        createdAt = password.createdAt,
                        updatedAt = password.updatedAt,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = id
                    )
                    passwordDao.update(newPassword)
                } else {
                    insert(password)
                }
            } catch (e: Exception) {
                if (password.syncState == SyncType.NOTHING_REQUIRED || password.syncState == SyncType.UPDATE_REQUIRED) {
                    password.syncState = SyncType.UPDATE_REQUIRED
                    passwordDao.update(password)
                } else {
                    insert(password)
                }
                when (e) {
                    is HttpException -> {
                        apiError = JSONObject()
                        apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                        Log.e(
                            "errorBody",
                            apiError.toString()
                        )
                        Log.e(
                            "$TAG.update.HTTP",
                            apiError.getString("error")
                        )
                    }
                    else -> {
                        Log.e(
                            "$TAG.update.ELSE",
                            e.localizedMessage!!
                        )
                    }
                }
            }
        } else {
            if (password.syncState == SyncType.NOTHING_REQUIRED || password.syncState == SyncType.UPDATE_REQUIRED) {
                password.syncState = SyncType.UPDATE_REQUIRED
                passwordDao.update(password)
            } else {
                insert(password)
            }
        }
    }

    suspend fun deleteAll() {
        passwordDao.deleteAll()
    }

    fun synchronizePasswords(
        didRefresh: Boolean, searchQuery: String, sortOrder: PasswordsSortOrder, isAsc: Boolean,
        onFetchComplete: () -> Unit
    ): Flow<Resource<List<Password>>> =
        networkBoundResource(
            query = {
                passwordDao.getPasswords(searchQuery, sortOrder, isAsc)
            },
            fetch = {
                if (passwordDao.getSyncedDeletedPasswordsIds().first().isNotEmpty()) {
                    api.deletePasswords(
                        passwordDao.getSyncedDeletedPasswordsIds().first() as ArrayList<Int>,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                }
                val combinedPasswords = arrayListOf<Password>()
                combinedPasswords.addAll(passwordDao.getSyncedUpdatedPasswords().first())
                combinedPasswords.addAll(passwordDao.getUnsyncedPasswords().first())
                combinedPasswords.map {
                    it.title = cm.encrypt(it.title)
                    it.login = cm.encrypt(it.login)
                    it.password = cm.encrypt(it.password)!!
                }

                val notes = api.postStorePasswords(
                    combinedPasswords,
                    "Bearer ${encryptedSharedPrefs.getToken()}"
                )
                notes
            },
            saveFetchResult = { passwordsApi ->
                val passwords = arrayListOf<Password>()
                Log.e("passwordsApi", "$passwordsApi")
                passwordsApi.map {
                    passwords.add(
                        Password(
                            title = cm.decrypt(it.title),
                            url = it.url,
                            login = cm.decrypt(it.login),
                            password = cm.decrypt(it.password)!!,
                            category = it.category,
                            color = it.color,
                            updatedAt = it.updated_at_device,
                            createdAt = it.created_at_device,
                            syncState = SyncType.NOTHING_REQUIRED,
                            id = it.id
                        )
                    )
                }
                db.withTransaction {
                    passwordDao.deleteAll()
                    passwordDao.insertList(passwords)
                }
            },
            shouldFetch = {
                if (didRefresh) {
                    if (isNetworkAvailable) {
                        passwordDao.getUnsyncedPasswords().first().isNotEmpty() ||
                                passwordDao.getSyncedDeletedPasswordsIds().first().isNotEmpty() ||
                                passwordDao.getSyncedUpdatedPasswords().first().isNotEmpty()
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
            onFetchSuccess = onFetchComplete,
            onFetchFailed = onFetchComplete
        )

    suspend fun deleteSelectedPasswords(passList: ArrayList<Password>): Resource<Any> {
        didPerformDeletionAPICall = false

        val passListCopy = arrayListOf<Password>()
        with(passList.iterator()) {
            forEach {
                passListCopy.add(it.copy())
            }
        }

        val unsyncedPassesIds = arrayListOf<Int>()
        val syncedPassesIds = arrayListOf<Int>()
        val syncedPasses = arrayListOf<Password>()

        passListCopy.map {
            if (it.syncState == SyncType.NOTHING_REQUIRED || it.syncState == SyncType.UPDATE_REQUIRED) {
                it.syncState = SyncType.DELETE_REQUIRED
                syncedPassesIds.add(it.id)
                syncedPasses.add(it)
            } else {
                unsyncedPassesIds.add(it.id)
            }
        }
        passwordDao.deleteSelectedPasswords(unsyncedPassesIds)
        passwordDao.insertList(syncedPasses)

        if (isNetworkAvailable) {
            if (syncedPassesIds.isNotEmpty()) {
                try {
                    api.deletePasswords(
                        syncedPassesIds,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    didPerformDeletionAPICall = true
                    passwordDao.deleteSelectedPasswords(syncedPassesIds)
                    return Resource.Success()
                } catch (e: Exception) {
                    when (e) {
                        is HttpException -> {
                            apiError = JSONObject()
                            apiError =
                                JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                            Log.e(
                                "errorBody",
                                apiError.toString()
                            )
                            Log.e(
                                "$TAG.deleteSelectedPasswords.HTTP",
                                apiError.getString("error")
                            )
                            return Resource.Error()
                        }
                        else -> {
                            Log.e(
                                "$TAG.deleteSelectedPasswords.ELSE",
                                e.localizedMessage!!
                            )
                            return Resource.Error()
                        }
                    }
                }
            } else {
                return Resource.Success()
            }
        } else {
            return Resource.Success()
        }
    }

    suspend fun undoDeletedPasswords(passList: ArrayList<Password>) {
        val unsyncedPasses = arrayListOf<Password>()
        val syncedPasses = arrayListOf<Password>()
        val bothPassesCombinedForSmoothInsertion = arrayListOf<Password>()

        passList.map {
            when (it.syncState) {
                SyncType.UPDATE_REQUIRED, SyncType.NOTHING_REQUIRED -> {
                    syncedPasses.add(it)
                }
                SyncType.CREATE_REQUIRED -> {
                    unsyncedPasses.add(it)
                }
                else -> {
                    Log.e(
                        "$TAG.undoDeletedPasswords.WHEN",
                        "Invalid operation"
                    )
                }
            }
        }

        if (isNetworkAvailable) {
            if (syncedPasses.isNotEmpty()) {
                if (didPerformDeletionAPICall) {
                    try {
                        bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                        bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                        bothPassesCombinedForSmoothInsertion.map {
                            it.title = cm.encrypt(it.title)
                            it.login = cm.encrypt(it.login)
                            it.password = cm.encrypt(it.password)!!
                        }
                        val passwordsApi = api.postRecoverPasswords(
                            bothPassesCombinedForSmoothInsertion,
                            "Bearer ${encryptedSharedPrefs.getToken()}"
                        )
                        val passwords = arrayListOf<Password>()
                        passwordsApi.map {
                            passwords.add(
                                Password(
                                    title = cm.decrypt(it.title),
                                    url = it.url,
                                    login = cm.decrypt(it.login),
                                    password = cm.decrypt(it.password)!!,
                                    category = it.category,
                                    color = it.color,
                                    updatedAt = it.updated_at_device,
                                    createdAt = it.created_at_device,
                                    syncState = SyncType.NOTHING_REQUIRED,
                                    id = it.id
                                )
                            )
                        }
                        passwordDao.insertList(passwords)
                    } catch (e: Exception) {
                        if (didPerformDeletionAPICall) {
                            syncedPasses.map {
                                it.syncState = SyncType.CREATE_REQUIRED
                            }
                            bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                            bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                            passwordDao.insertList(bothPassesCombinedForSmoothInsertion)
                            didPerformDeletionAPICall = false
                        } else {
                            bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                            bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                            passwordDao.insertList(bothPassesCombinedForSmoothInsertion)
                        }
                        when (e) {
                            is HttpException -> {
                                apiError = JSONObject()
                                apiError =
                                    JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                                Log.e(
                                    "errorBody",
                                    apiError.toString()
                                )
                                Log.e(
                                    "$TAG.undoDeletedPasswords.HTTP",
                                    apiError.getString("error")
                                )
                            }
                            else -> {
                                Log.e(
                                    "$TAG.undoDeletedPasswords.ELSE",
                                    e.localizedMessage!!
                                )
                            }
                        }
                    }
                } else {
                    bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                    bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                    passwordDao.insertList(bothPassesCombinedForSmoothInsertion)
                }
            } else {
                if (unsyncedPasses.isNotEmpty()) {
                    unsyncedPasses.map {
                        it.title = cm.encrypt(it.title)
                        it.login = cm.encrypt(it.login)
                        it.password = cm.encrypt(it.password)!!
                    }
                    val passwordsApi = api.postRecoverPasswords(
                        unsyncedPasses,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    val passwords = arrayListOf<Password>()
                    passwordsApi.map {
                        passwords.add(
                            Password(
                                title = cm.decrypt(it.title),
                                url = it.url,
                                login = cm.decrypt(it.login),
                                password = cm.decrypt(it.password)!!,
                                category = it.category,
                                color = it.color,
                                updatedAt = it.updated_at_device,
                                createdAt = it.created_at_device,
                                syncState = SyncType.NOTHING_REQUIRED,
                                id = it.id
                            )
                        )
                    }
                    passwordDao.insertList(passwords)
                }
            }
        } else {
            if (syncedPasses.isNotEmpty()) {
                if (didPerformDeletionAPICall) {
                    syncedPasses.map {
                        it.syncState = SyncType.CREATE_REQUIRED
                    }
                    bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                    bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                    passwordDao.insertList(bothPassesCombinedForSmoothInsertion)
                    didPerformDeletionAPICall = false
                } else {
                    bothPassesCombinedForSmoothInsertion.addAll(unsyncedPasses)
                    bothPassesCombinedForSmoothInsertion.addAll(syncedPasses)
                    passwordDao.insertList(bothPassesCombinedForSmoothInsertion)
                }
            } else {
                passwordDao.insertList(unsyncedPasses)
            }
        }
    }
}