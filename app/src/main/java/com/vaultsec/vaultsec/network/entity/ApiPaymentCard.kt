package com.vaultsec.vaultsec.network.entity

import androidx.room.TypeConverters
import com.vaultsec.vaultsec.database.converter.DateConverter
import java.sql.Timestamp

data class ApiPaymentCard(
    val title: String,
    val card_number: String,
    val expiration_mm: String,
    val expiration_yy: String,
    val type: String,
    val cvv: String,
    val pin: String,
    @TypeConverters(DateConverter::class) val created_at_device: Timestamp,
    @TypeConverters(DateConverter::class) val updated_at_device: Timestamp,
    val id: Int
)
