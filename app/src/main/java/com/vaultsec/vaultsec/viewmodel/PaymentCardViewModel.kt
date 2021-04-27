package com.vaultsec.vaultsec.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordManagerPreferences
import com.vaultsec.vaultsec.database.PaymentCardsSortOrder
import com.vaultsec.vaultsec.database.entity.PaymentCard
import com.vaultsec.vaultsec.repository.PaymentCardRepository
import com.vaultsec.vaultsec.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentCardViewModel
@Inject constructor(
    private val paymentCardRepository: PaymentCardRepository,
    private val prefsManager: PasswordManagerPreferences
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val preferencesFlow = prefsManager.preferencesFlow

    private val paymentCardsEventChannel = Channel<PaymentCardEvent>()
    val paymentCardsEvent = paymentCardsEventChannel.receiveAsFlow()

    private val deletePaymentCardsState = MutableStateFlow<Resource<*>>(Resource.Empty<Any>())
    private var deletionResponse: Resource<*> = Resource.Loading<Any>()

    private val refreshTriggerChannel = Channel<Refresh>()
    private val refreshTrigger = refreshTriggerChannel.receiveAsFlow()

    private val multiSelectedPaymentCards: ArrayList<PaymentCard> = arrayListOf()

    val paymentCards: LiveData<Resource<List<PaymentCard>>> = refreshTrigger.flatMapLatest {
        combine(
            searchQuery,
            preferencesFlow
        ) { query, prefs ->
            Pair(query, prefs)
        }.flatMapLatest { (query, prefs) ->
            paymentCardRepository.synchronizePaymentCards(
                didRefresh = (it == Refresh.DID),
                searchQuery = query,
                sortOrder = prefs.paymentCardsSortOrder,
                isAsc = prefs.isAscPaymentCards,
                onFetchComplete = {
                    viewModelScope.launch(Dispatchers.IO) {
                        refreshTriggerChannel.send(Refresh.DIDNT)
                    }
                }
            )
        }
    }.asLiveData()

    fun onStart() {
        viewModelScope.launch {
            if (paymentCards.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DIDNT)
            }
        }
    }

    fun onSortOrderSelected(sortOrder: PaymentCardsSortOrder) =
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.updateSortOrderForPaymentCards(sortOrder)
        }

    fun onSortDirectionSelected(isAsc: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        prefsManager.updateSortDirectionForPaymentCards(isAsc)
    }

    fun onCardSelection(card: PaymentCard) {
        multiSelectedPaymentCards.add(card)
    }

    fun onCardDeselection(card: PaymentCard) {
        multiSelectedPaymentCards.remove(card)
    }

    fun onDeleteSelectedCardsClick() = viewModelScope.launch {
        deletePaymentCardsState.value = Resource.Loading<Any>()
        if (multiSelectedPaymentCards.isNotEmpty()) {
            val multiSelectedPaymentCardsClone =
                multiSelectedPaymentCards.clone() as ArrayList<PaymentCard>
            viewModelScope.launch(Dispatchers.IO) {
                deletePaymentCardsState.value =
                    paymentCardRepository.deleteSelectedPaymentCards(multiSelectedPaymentCardsClone)
            }
            paymentCardsEventChannel.send(
                PaymentCardEvent.ShowUndoDeleteCardMessage(
                    multiSelectedPaymentCardsClone
                )
            )
            multiSelectedPaymentCards.clear()
        }
    }

    fun onMultiSelectActionModeClose() {
        multiSelectedPaymentCards.clear()
    }

    fun onUndoDeleteClick(cardList: ArrayList<PaymentCard>) =
        viewModelScope.launch(Dispatchers.IO) {
            paymentCardsEventChannel.send(PaymentCardEvent.DoShowRefreshing(true))

            if (deletePaymentCardsState.value is Resource.Loading) {
                viewModelScope.launch {
                    deletePaymentCardsState.collect {
                        when (it) {
                            is Resource.Success -> {
                                paymentCardRepository.undoDeletedPaymentCards(cardList)
                                deletePaymentCardsState.value = Resource.Empty<Any>()
                                paymentCardsEventChannel.send(
                                    PaymentCardEvent.DoShowRefreshing(
                                        false
                                    )
                                )
                                cancel()
                            }
                            is Resource.Error -> {
                                paymentCardRepository.undoDeletedPaymentCards(cardList)
                                deletePaymentCardsState.value = Resource.Empty<Any>()
                                paymentCardsEventChannel.send(
                                    PaymentCardEvent.DoShowRefreshing(
                                        false
                                    )
                                )
                                cancel()
                            }
                        }
                    }
                }
            } else {
                paymentCardRepository.undoDeletedPaymentCards(cardList)
                deletionResponse = Resource.Loading<Any>()
                paymentCardsEventChannel.send(PaymentCardEvent.DoShowRefreshing(false))
            }
        }

    fun onAddNewPaymentCardClick() = viewModelScope.launch(Dispatchers.IO) {
        paymentCardsEventChannel.send(PaymentCardEvent.NavigateToAddPaymentCardFragment)
    }

    fun onPaymentCardClicked(card: PaymentCard) = viewModelScope.launch(Dispatchers.IO) {
        paymentCardsEventChannel.send(PaymentCardEvent.NavigateToEditPaymentCardFragment(card))
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_PAYMENT_CARD_RESULT_OK -> showPaymentCardSavedConfirmationMessage(R.string.add_payment_card_confirmation)
            EDIT_PAYMENT_CARD_RESULT_OK -> showPaymentCardSavedConfirmationMessage(R.string.edit_payment_card_confirmation)
        }
    }

    private fun showPaymentCardSavedConfirmationMessage(message: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            paymentCardsEventChannel.send(PaymentCardEvent.ShowCardSavedConfimationMessage(message))
        }
    }

    fun onManualPaymentCardSync() {
        viewModelScope.launch {
            if (paymentCards.value !is Resource.Loading) {
                refreshTriggerChannel.send(Refresh.DID)
            }
        }
    }

    enum class Refresh {
        DID, DIDNT
    }

    sealed class PaymentCardEvent {
        object NavigateToAddPaymentCardFragment : PaymentCardEvent()
        data class NavigateToEditPaymentCardFragment(val card: PaymentCard) : PaymentCardEvent()
        data class ShowUndoDeleteCardMessage(val cardList: ArrayList<PaymentCard>) :
            PaymentCardEvent()

        data class ShowCardSavedConfimationMessage(val message: Int) : PaymentCardEvent()
        data class DoShowRefreshing(val visible: Boolean) : PaymentCardEvent()
    }
}