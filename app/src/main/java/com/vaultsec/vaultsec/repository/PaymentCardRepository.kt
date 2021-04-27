package com.vaultsec.vaultsec.repository

import android.util.Log
import androidx.room.withTransaction
import com.vaultsec.vaultsec.database.PasswordManagerDatabase
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.database.PaymentCardsSortOrder
import com.vaultsec.vaultsec.database.entity.PaymentCard
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

class PaymentCardRepository @Inject constructor(
    private val db: PasswordManagerDatabase,
    private val api: PasswordManagerApi,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences,
    private val cm: CipherManager
) {
    private val paymentCardDao = db.paymentCardDao()

    private var didPerformDeletionAPICall: Boolean = false
    private var apiError = JSONObject()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.repository.PaymentCardRepository"
    }

    suspend fun insert(card: PaymentCard) {
        if (isNetworkAvailable) {
            try {
                var newPaymentCard = card.copy(
                    title = card.title,
                    cardNumber = cm.encrypt(card.cardNumber)!!,
                    mm = cm.encrypt(card.mm)!!,
                    yy = cm.encrypt(card.yy)!!,
                    type = card.type,
                    cvv = cm.encrypt(card.cvv)!!,
                    pin = cm.encrypt(card.pin)!!,
                    updatedAt = card.updatedAt,
                    createdAt = card.createdAt
                )
                val id =
                    api.postSinglePaymentCard(
                        newPaymentCard,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                newPaymentCard = card.copy(
                    title = card.title,
                    cardNumber = card.cardNumber,
                    mm = card.mm,
                    yy = card.yy,
                    type = card.type,
                    cvv = card.cvv,
                    pin = card.pin,
                    updatedAt = card.updatedAt,
                    createdAt = card.createdAt,
                    syncState = SyncType.NOTHING_REQUIRED,
                    id = id
                )
                paymentCardDao.insert(newPaymentCard)
            } catch (e: Exception) {
                paymentCardDao.insert(card)
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
            paymentCardDao.insert(card)
        }
    }

    suspend fun update(card: PaymentCard) {
        if (isNetworkAvailable) {
            try {
                if (card.syncState == SyncType.NOTHING_REQUIRED || card.syncState == SyncType.UPDATE_REQUIRED) {
                    var newPaymentCard = card.copy(
                        title = card.title,
                        cardNumber = cm.encrypt(card.cardNumber)!!,
                        mm = cm.encrypt(card.mm)!!,
                        yy = cm.encrypt(card.yy)!!,
                        type = card.type,
                        cvv = cm.encrypt(card.cvv)!!,
                        pin = cm.encrypt(card.pin)!!,
                        updatedAt = card.updatedAt,
                        createdAt = card.createdAt
                    )
                    val id = api.putPaymentCardUpdate(
                        card.id,
                        newPaymentCard,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    newPaymentCard = card.copy(
                        title = card.title,
                        cardNumber = card.cardNumber,
                        mm = card.mm,
                        yy = card.yy,
                        type = card.type,
                        cvv = card.cvv,
                        pin = card.pin,
                        updatedAt = card.updatedAt,
                        createdAt = card.createdAt,
                        syncState = SyncType.NOTHING_REQUIRED,
                        id = id
                    )
                    paymentCardDao.update(newPaymentCard)
                } else {
                    insert(card)
                }
            } catch (e: Exception) {
                if (card.syncState == SyncType.NOTHING_REQUIRED || card.syncState == SyncType.UPDATE_REQUIRED) {
                    card.syncState = SyncType.UPDATE_REQUIRED
                    paymentCardDao.update(card)
                } else {
                    insert(card)
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
            if (card.syncState == SyncType.NOTHING_REQUIRED || card.syncState == SyncType.UPDATE_REQUIRED) {
                card.syncState = SyncType.UPDATE_REQUIRED
                paymentCardDao.update(card)
            } else {
                insert(card)
            }
        }
    }

    suspend fun synchronizePaymentCards(
        didRefresh: Boolean, searchQuery: String, sortOrder: PaymentCardsSortOrder, isAsc: Boolean,
        onFetchComplete: () -> Unit
    ): Flow<Resource<List<PaymentCard>>> =
        networkBoundResource(
            query = {
                paymentCardDao.getPaymentCards(searchQuery, sortOrder, isAsc)
            },
            fetch = {
                if (paymentCardDao.getSyncedDeletedPaymentCardsIds().first().isNotEmpty()) {
                    api.deletePaymentCards(
                        paymentCardDao.getSyncedDeletedPaymentCardsIds().first() as ArrayList<Int>,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                }
                val combinedCards = arrayListOf<PaymentCard>()
                combinedCards.addAll(paymentCardDao.getSyncedUpdatedPaymentCards().first())
                combinedCards.addAll(paymentCardDao.getUnsyncedPaymentCards().first())
                combinedCards.map {
                    it.cardNumber = cm.encrypt(it.cardNumber)!!
                    it.mm = cm.encrypt(it.mm)!!
                    it.yy = cm.encrypt(it.yy)!!
                    it.cvv = cm.encrypt(it.cvv)!!
                    it.pin = cm.encrypt(it.pin)!!
                }

                val cards = api.postStorePaymentCards(
                    combinedCards,
                    "Bearer ${encryptedSharedPrefs.getToken()}"
                )
                cards
            },
            saveFetchResult = { cardsApi ->
                val cards = arrayListOf<PaymentCard>()
                cardsApi.map {
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
                db.withTransaction {
                    paymentCardDao.deleteAll()
                    paymentCardDao.insertList(cards)
                }
            },
            shouldFetch = {
                if (didRefresh) {
                    if (isNetworkAvailable) {
                        paymentCardDao.getUnsyncedPaymentCards().first().isNotEmpty() ||
                                paymentCardDao.getSyncedDeletedPaymentCardsIds().first()
                                    .isNotEmpty() ||
                                paymentCardDao.getSyncedUpdatedPaymentCards().first().isNotEmpty()
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

    suspend fun deleteSelectedPaymentCards(cardsList: ArrayList<PaymentCard>): Resource<Any> {
        didPerformDeletionAPICall = false

        val cardsListCopy = arrayListOf<PaymentCard>()
        with(cardsList.iterator()) {
            forEach {
                cardsListCopy.add(it.copy())
            }
        }

        val unsyncedCardsIds = arrayListOf<Int>()
        val syncedCardsIds = arrayListOf<Int>()
        val syncedCards = arrayListOf<PaymentCard>()

        cardsListCopy.map {
            if (it.syncState == SyncType.NOTHING_REQUIRED || it.syncState == SyncType.UPDATE_REQUIRED) {
                it.syncState = SyncType.DELETE_REQUIRED
                syncedCardsIds.add(it.id)
                syncedCards.add(it)
            } else {
                unsyncedCardsIds.add(it.id)
            }
        }
        paymentCardDao.deleteSelectedPaymentCards(unsyncedCardsIds)
        paymentCardDao.insertList(syncedCards)

        if (isNetworkAvailable) {
            if (syncedCardsIds.isNotEmpty()) {
                try {
                    api.deletePaymentCards(
                        syncedCardsIds,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    didPerformDeletionAPICall = true
                    paymentCardDao.deleteSelectedPaymentCards(syncedCardsIds)
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
                                "$TAG.deleteSelectedPaymentCards.HTTP",
                                apiError.getString("error")
                            )
                            return Resource.Error()
                        }
                        else -> {
                            Log.e(
                                "$TAG.deleteSelectedPaymentCards.ELSE",
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

    suspend fun undoDeletedPaymentCards(cardsList: ArrayList<PaymentCard>) {
        val unsyncedCards = arrayListOf<PaymentCard>()
        val syncedCards = arrayListOf<PaymentCard>()
        val bothCardsCombinedForSmoothInsertion = arrayListOf<PaymentCard>()

        cardsList.map {
            when (it.syncState) {
                SyncType.UPDATE_REQUIRED, SyncType.NOTHING_REQUIRED -> {
                    syncedCards.add(it)
                }
                SyncType.CREATE_REQUIRED -> {
                    unsyncedCards.add(it)
                }
                else -> {
                    Log.e(
                        "$TAG.undoDeletedPaymentCards.WHEN",
                        "Invalid operation"
                    )
                }
            }
        }

        if (isNetworkAvailable) {
            if (syncedCards.isNotEmpty()) {
                if (didPerformDeletionAPICall) {
                    try {
                        bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                        bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                        bothCardsCombinedForSmoothInsertion.map {
                            it.cardNumber = cm.encrypt(it.cardNumber)!!
                            it.mm = cm.encrypt(it.mm)!!
                            it.yy = cm.encrypt(it.yy)!!
                            it.cvv = cm.encrypt(it.cvv)!!
                            it.pin = cm.encrypt(it.pin)!!
                        }
                        val cardsApi = api.postRecoverPaymentCards(
                            bothCardsCombinedForSmoothInsertion,
                            "Bearer ${encryptedSharedPrefs.getToken()}"
                        )
                        val cards = arrayListOf<PaymentCard>()
                        cardsApi.map {
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
                        paymentCardDao.insertList(cards)
                    } catch (e: Exception) {
                        if (didPerformDeletionAPICall) {
                            syncedCards.map {
                                it.syncState = SyncType.CREATE_REQUIRED
                            }
                            bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                            bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                            paymentCardDao.insertList(bothCardsCombinedForSmoothInsertion)
                            didPerformDeletionAPICall = false
                        } else {
                            bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                            bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                            paymentCardDao.insertList(bothCardsCombinedForSmoothInsertion)
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
                                    "$TAG.undoDeletedPaymentCards.HTTP",
                                    apiError.getString("error")
                                )
                            }
                            else -> {
                                Log.e(
                                    "$TAG.undoDeletedPaymentCards.ELSE",
                                    e.localizedMessage!!
                                )
                            }
                        }
                    }
                } else {
                    bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                    bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                    paymentCardDao.insertList(bothCardsCombinedForSmoothInsertion)
                }
            } else {
                if (unsyncedCards.isNotEmpty()) {
                    unsyncedCards.map {
                        it.cardNumber = cm.encrypt(it.cardNumber)!!
                        it.mm = cm.encrypt(it.mm)!!
                        it.yy = cm.encrypt(it.yy)!!
                        it.cvv = cm.encrypt(it.cvv)!!
                        it.pin = cm.encrypt(it.pin)!!
                    }
                    val cardsApi = api.postRecoverPaymentCards(
                        unsyncedCards,
                        "Bearer ${encryptedSharedPrefs.getToken()}"
                    )
                    val cards = arrayListOf<PaymentCard>()
                    cardsApi.map {
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
                    paymentCardDao.insertList(cards)
                }
            }
        } else {
            if (syncedCards.isNotEmpty()) {
                if (didPerformDeletionAPICall) {
                    syncedCards.map {
                        it.syncState = SyncType.CREATE_REQUIRED
                    }
                    bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                    bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                    paymentCardDao.insertList(bothCardsCombinedForSmoothInsertion)
                    didPerformDeletionAPICall = false
                } else {
                    bothCardsCombinedForSmoothInsertion.addAll(unsyncedCards)
                    bothCardsCombinedForSmoothInsertion.addAll(syncedCards)
                    paymentCardDao.insertList(bothCardsCombinedForSmoothInsertion)
                }
            } else {
                paymentCardDao.insertList(unsyncedCards)
            }
        }
    }
}