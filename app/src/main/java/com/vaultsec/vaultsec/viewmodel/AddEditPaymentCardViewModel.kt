package com.vaultsec.vaultsec.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.PaymentCard
import com.vaultsec.vaultsec.repository.PaymentCardRepository
import com.vaultsec.vaultsec.util.SupportedPaymentCardTypes
import com.vaultsec.vaultsec.util.SyncType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp
import java.util.regex.Pattern
import javax.inject.Inject

const val ADD_PAYMENT_CARD_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_PAYMENT_CARD_RESULT_OK = Activity.RESULT_FIRST_USER + 1

const val CARD_NUMBER_CAMERA_BUTTON = Activity.RESULT_FIRST_USER + 2
const val CARD_PIN_CAMERA_BUTTON = Activity.RESULT_FIRST_USER + 3

@HiltViewModel
class AddEditPaymentCardViewModel @Inject constructor(
    private val paymentCardRepository: PaymentCardRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    val card = state.get<PaymentCard>("card")

    private val addEditPaymentCardEventChannel = Channel<AddEditPaymentCardEvent>()
    val addEditPaymentCardEvent = addEditPaymentCardEventChannel.receiveAsFlow()

    private val visaRegex = Pattern.compile("^4[0-9]{12}(?:[0-9]{3}){0,2}$")
    private val msRegex = Pattern.compile("^(?:5[1-5]|2(?!2([01]|20)|7(2[1-9]|3))[2-7])\\d{14}$")

    var whichTextScanned: Int = -1

    var paymentCardTitle: String? = state.get<String>("paymentCardTitle") ?: card?.title ?: ""
        set(value) {
            field = value
            state.set("paymentCardTitle", value)
        }

    var paymentCardNumber: String = state.get<String>("paymentCardNumber") ?: card?.cardNumber ?: ""
        set(value) {
            field = value
            state.set("paymentCardNumber", value)
        }

    var paymentCardMM: String = state.get<String>("paymentCardMM") ?: card?.mm ?: ""
        set(value) {
            field = value
            state.set("paymentCardMM", value)
        }

    var paymentCardYY: String = state.get<String>("paymentCardYY") ?: card?.yy ?: ""
        set(value) {
            field = value
            state.set("paymentCardYY", value)
        }

    var paymentCardCVV: String = state.get<String>("paymentCardCVV") ?: card?.cvv ?: ""
        set(value) {
            field = value
            state.set("paymentCardCVV", value)
        }

    var paymentCardPIN: String = state.get<String>("paymentCardPIN") ?: card?.pin ?: ""
        set(value) {
            field = value
            state.set("paymentCardPIN", value)
        }

    var paymentCardType: String = state.get<String>("paymentCardType") ?: card?.type ?: ""
        set(value) {
            field = value
            state.set("paymentCardType", value)
        }

    var paymentCardDateCreated =
        state.get<Timestamp>("paymentCardDateCreated") ?: card?.createdAt ?: Timestamp(
            System.currentTimeMillis()
        )
        set(value) {
            field = value
            state.set("paymentCardDateCreated", value)
        }

    var paymentCardDateUpdated =
        state.get<Timestamp>("paymentCardDateUpdated") ?: card?.updatedAt ?: Timestamp(
            System.currentTimeMillis()
        )
        set(value) {
            field = value
            state.set("paymentCardDateUpdated", value)
        }

    var paymentCardSyncStateInt =
        state.get<Int>("paymentCardSyncStateInt") ?: card?.syncState ?: SyncType.CREATE_REQUIRED
        set(value) {
            field = value
            state.set("paymentCardSyncStateInt", value)
        }

    private fun areCardsTheSame(): Boolean {
        return (card!!.title == paymentCardTitle
                && card.mm == paymentCardMM
                && card.yy == paymentCardYY
                && card.cvv == paymentCardCVV
                && card.pin == paymentCardPIN)
    }

    fun onSavePaymentCardClick() {
        val digit = Pattern.compile("[0-9]+")
        if (paymentCardNumber.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_number_input_error_empty
                    )
                )
            }
            return
        }
        if (!digit.matcher(paymentCardNumber).matches() || !isPaymentCardNumberValid(
                paymentCardNumber
            )
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_number_input_error_invalid
                    )
                )
            }
            return
        }
        if (paymentCardMM.isBlank() || paymentCardYY.isBlank()
            || !digit.matcher(paymentCardMM).matches() || !digit.matcher(paymentCardYY).matches()
            || (paymentCardMM.toInt() !in 1..12)
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_expiration_input_error_invalid
                    )
                )
            }
            return
        }
        if (paymentCardCVV.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_cvv_input_error_empty
                    )
                )
            }
            return
        }
        if (!digit.matcher(paymentCardCVV).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_cvv_input_error_invalid
                    )
                )
            }
            return
        }
        if (paymentCardPIN.isBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_pin_input_error_empty
                    )
                )
            }
            return
        }
        if (!digit.matcher(paymentCardPIN).matches()) {
            viewModelScope.launch(Dispatchers.IO) {
                addEditPaymentCardEventChannel.send(
                    AddEditPaymentCardEvent.ShowInvalidInputMessage(
                        R.string.add_edit_card_pin_input_error_invalid
                    )
                )
            }
            return
        }

        if (card != null) {
            if (areCardsTheSame()) {
                viewModelScope.launch(Dispatchers.IO) {
                    addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.NavigateBackWithoutResult)
                }
                return
            } else {
                val updatedPaymentCard = card.copy(
                    title = paymentCardTitle,
                    cardNumber = paymentCardNumber,
                    mm = paymentCardMM,
                    yy = paymentCardYY,
                    type = determineCardType(paymentCardNumber),
                    cvv = paymentCardCVV,
                    pin = paymentCardPIN,
                    updatedAt = paymentCardDateCreated,
                    createdAt = paymentCardDateUpdated,
                    syncState = paymentCardSyncStateInt
                )
                updatePaymentCard(updatedPaymentCard)
            }
        } else {
            val newPaymentCard = PaymentCard(
                title = paymentCardTitle,
                cardNumber = paymentCardNumber,
                mm = paymentCardMM,
                yy = paymentCardYY,
                type = determineCardType(paymentCardNumber),
                cvv = paymentCardCVV,
                pin = paymentCardPIN,
                updatedAt = paymentCardDateCreated,
                createdAt = paymentCardDateUpdated
            )
            createPaymentCard(newPaymentCard)
        }
    }

    private fun createPaymentCard(newCard: PaymentCard) = viewModelScope.launch(Dispatchers.IO) {
        addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.DoShowLoading(true))
        paymentCardRepository.insert(newCard)
        addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.DoShowLoading(false))
        addEditPaymentCardEventChannel.send(
            AddEditPaymentCardEvent.NavigateBackWithResult(ADD_PAYMENT_CARD_RESULT_OK)
        )
    }

    private fun updatePaymentCard(updatedCard: PaymentCard) =
        viewModelScope.launch(Dispatchers.IO) {
            addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.DoShowLoading(true))
            paymentCardRepository.update(updatedCard)
            addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.DoShowLoading(false))
            addEditPaymentCardEventChannel.send(
                AddEditPaymentCardEvent.NavigateBackWithResult(EDIT_PASSWORD_RESULT_OK)
            )
        }

    fun onOpenCamera() = viewModelScope.launch(Dispatchers.IO) {
        addEditPaymentCardEventChannel.send(AddEditPaymentCardEvent.NavigateToCameraFragment)
    }

    fun determineCardType(text: CharSequence): String {
        Log.e("isCardNumberValid?", "${isPaymentCardNumberValid(text.toString())}")
        return when {
            visaRegex.matcher(text).matches() -> {
                SupportedPaymentCardTypes.VISA
            }
            msRegex.matcher(text).matches() -> {
                SupportedPaymentCardTypes.MasterCard
            }
            else -> {
                SupportedPaymentCardTypes.Other
            }
        }
    }

    /*
    * Luhn algorithm is a checksum formula used to validate a variety of identification numbers
    * such as credit card numbers, IMEI numbers, etc.
    *
    * Every payment card has a 'check digit' at the end. Luhn algorithm's formula verifies a number
    * against its included check digit
    * */
    private fun isPaymentCardNumberValid(cardNumber: String): Boolean {
        try {
            var sum = 0
            var alternate = false
            for (i in cardNumber.length - 1 downTo 0) {
                var n = Integer.parseInt(cardNumber.substring(i, i + 1))
                if (alternate) {
                    n *= 2
                    if (n > 9) {
                        n = (n % 10) + 1
                    }
                }
                sum += n
                alternate = !alternate
            }
            return sum % 10 == 0
        } catch (e: Exception) {
            return false
        }
    }

    sealed class AddEditPaymentCardEvent {
        data class ShowInvalidInputMessage(val message: Int) : AddEditPaymentCardEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditPaymentCardEvent()
        object NavigateBackWithoutResult : AddEditPaymentCardEvent()
        data class DoShowLoading(val visible: Boolean) : AddEditPaymentCardEvent()
        object NavigateToCameraFragment : AddEditPaymentCardEvent()
    }
}