package com.vaultsec.vaultsec.repository

import android.util.Log
import androidx.room.withTransaction
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.database.entity.PaymentCard
import com.vaultsec.vaultsec.network.PasswordManagerApi
import com.vaultsec.vaultsec.util.ErrorTypes
import com.vaultsec.vaultsec.util.Resource
import com.vaultsec.vaultsec.util.SyncType
import com.vaultsec.vaultsec.util.cipher.CipherManager
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject

class BottomNavigationRepository @Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences,
    private val cm: CipherManager
) {
    private val noteDao = db.noteDao()
    private val passwordDao = db.passwordDao()
    private val paymentCardDao = db.paymentCardDao()

    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.BottomNavigationRepository"
    }

    private fun getToken(): String {
        return encryptedSharedPrefs.getToken()!!
    }

    suspend fun postLogout(databaseDir: String): Resource<*> {
        try {
            val combinedNotes = arrayListOf<Note>()
            val combinedPasswords = arrayListOf<Password>()
            val combinedCards = arrayListOf<PaymentCard>()
            // Sync before logout
            val syncedButDeletedIds = noteDao.getSyncedDeletedNotesIds().first()
            if (syncedButDeletedIds.isNotEmpty()) {
                api.deleteNotes(syncedButDeletedIds as ArrayList<Int>, "Bearer ${getToken()}")
            }
            val syncedButDeletedPasswordIds = passwordDao.getSyncedDeletedPasswordsIds().first()
            if (syncedButDeletedPasswordIds.isNotEmpty()) {
                api.deletePasswords(
                    syncedButDeletedPasswordIds as ArrayList<Int>,
                    "Bearer ${getToken()}"
                )
            }
            val syncedButDeletedCardsIds = paymentCardDao.getSyncedDeletedPaymentCardsIds().first()
            if (syncedButDeletedCardsIds.isNotEmpty()) {
                api.deletePaymentCards(
                    syncedButDeletedCardsIds as ArrayList<Int>,
                    "Bearer ${getToken()}"
                )
            }

            combinedNotes.addAll(noteDao.getUnsyncedNotes().first())
            combinedNotes.addAll(noteDao.getSyncedUpdatedNotes().first())
            combinedNotes.map {
                it.title = cm.encrypt(it.title)
                it.text = cm.encrypt(it.text)!!
            }
            combinedPasswords.addAll(passwordDao.getUnsyncedPasswords().first())
            combinedPasswords.addAll(passwordDao.getSyncedUpdatedPasswords().first())
            combinedPasswords.map {
                it.title = cm.encrypt(it.title)
                it.login = cm.encrypt(it.login)
                it.password = cm.encrypt(it.password)!!
            }
            combinedCards.addAll(paymentCardDao.getUnsyncedPaymentCards().first())
            combinedCards.addAll(paymentCardDao.getSyncedUpdatedPaymentCards().first())
            combinedCards.map {
                it.cardNumber = cm.encrypt(it.cardNumber)!!
                it.mm = cm.encrypt(it.mm)!!
                it.yy = cm.encrypt(it.yy)!!
                it.cvv = cm.encrypt(it.cvv)!!
                it.pin = cm.encrypt(it.pin)!!
            }

            api.postStoreNotes(combinedNotes, "Bearer ${getToken()}")
            api.postStorePasswords(combinedPasswords, "Bearer ${getToken()}")
            api.postStorePaymentCards(combinedCards, "Bearer ${getToken()}")
            // Actually logout
            api.postLogout("Bearer ${getToken()}")
            // Empty the database
            db.clearAllTables()
            File(File(databaseDir), "vaultsec-database").delete()
            // Clear encrypted shared preferences
            encryptedSharedPrefs.emptyEncryptedSharedPrefs()
            return Resource.Success<Any>()
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.postLogout.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.postLogout.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.postLogout.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e("$TAG.postLogout.SOCKET", e.message.toString())
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.postLogout.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    suspend fun onLogIn(): Resource<*> {
        val notesResponse = getUserNotes(getToken())
        val passwordsResponse = getUserPasswords(getToken())
        val paymentCardsResponse = getUserPaymentCards(getToken())
        return if (notesResponse is Resource.Success
            && passwordsResponse is Resource.Success
            && paymentCardsResponse is Resource.Success
        ) {
            db.withTransaction {
                noteDao.deleteAll()
                passwordDao.deleteAll()
                paymentCardDao.deleteAll()
                noteDao.insertList(notesResponse.data as ArrayList<Note>)
                passwordDao.insertList(passwordsResponse.data as ArrayList<Password>)
                paymentCardDao.insertList(paymentCardsResponse.data as ArrayList<PaymentCard>)
            }
            Resource.Success<Any>()
        } else {
            notesResponse
        }
    }

    private suspend fun getUserNotes(token: String): Resource<*> {
        try {
            val notesResponse = api.getUserNotes("Bearer $token")
            val notes = arrayListOf<Note>()
            notesResponse.map {
                notes.add(
                    Note(
                        title = cm.decrypt(it.title),
                        text = cm.decrypt(it.text)!!,
                        color = it.color,
                        fontSize = it.font_size,
                        createdAt = it.created_at_device,
                        updatedAt = it.updated_at_device,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = it.id
                    )
                )
            }
            return Resource.Success(notes)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.getUserNotes.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.getUserNotes.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.getUserNotes.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "$TAG.getUserNotes.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.getUserNotes.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    private suspend fun getUserPasswords(token: String): Resource<*> {
        try {
            val passwordsResponse = api.getUserPasswords("Bearer $token")
            val passwords = arrayListOf<Password>()
            passwordsResponse.map {
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
            return Resource.Success(passwords)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.getUserPasswords.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.getUserPasswords.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.getUserPasswords.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "$TAG.getUserPasswords.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.getUserPasswords.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    private suspend fun getUserPaymentCards(token: String): Resource<*> {
        try {
            val paymentCardsResponse = api.getUserPaymentCards("Bearer $token")
            val cards = arrayListOf<PaymentCard>()
            paymentCardsResponse.map {
                cards.add(
                    PaymentCard(
                        title = it.title,
                        cardNumber = cm.decrypt(it.card_number)!!,
                        mm = cm.decrypt(it.expiration_mm)!!,
                        yy = cm.decrypt(it.expiration_yy)!!,
                        type = it.type,
                        cvv = cm.decrypt(it.cvv)!!,
                        pin = cm.decrypt(it.pin)!!,
                        updatedAt = it.updated_at_device,
                        createdAt = it.created_at_device,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = it.id
                    )
                )
            }
            return Resource.Success(cards)
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    apiError = JSONObject()
                    apiError = JSONObject(e.response()?.errorBody()?.charStream()!!.readText())
                    Log.e(
                        "errorBody",
                        apiError.toString()
                    )
                    Log.e(
                        "$TAG.getUserPaymentCards.HTTP",
                        apiError.getString("error")
                    )
                    return Resource.Error<Any>(ErrorTypes.HTTP, apiError.getString("error"))
                }
                is SocketTimeoutException -> {
                    Log.e(
                        "$TAG.getUserPaymentCards.TIMEOUT",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET_TIMEOUT)
                }
                is ConnectException -> {
                    Log.e(
                        "$TAG.getUserPaymentCards.CONNECTION",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.CONNECTION)
                }
                is SocketException -> {
                    Log.e(
                        "$TAG.getUserPaymentCards.SOCKET",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.SOCKET)
                }
                else -> {
                    Log.e(
                        "$TAG.getUserPaymentCards.GENERAL",
                        e.message.toString()
                    )
                    return Resource.Error<Any>(ErrorTypes.GENERAL)
                }
            }
        }
    }

    fun printTokenToConsoleForTesting() {
        Log.e("ACCESS TOKEN:::", getToken())
        Log.e(
            "Database password::: ",
            (encryptedSharedPrefs.getCredentials()!!.emailHash + encryptedSharedPrefs.getCredentials()!!.passwordHash)
        )
    }
}