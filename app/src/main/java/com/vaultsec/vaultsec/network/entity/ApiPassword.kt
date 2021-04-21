package com.vaultsec.vaultsec.network.entity

import androidx.room.TypeConverters
import com.vaultsec.vaultsec.database.converter.DateConverter
import java.sql.Timestamp

data class ApiPassword(
    val title: String?,
    val url: String?,
    val login: String?,
    val password: String,
    val category: String,
    val color: String,
    @TypeConverters(DateConverter::class) val created_at_device: Timestamp,
    @TypeConverters(DateConverter::class) val updated_at_device: Timestamp,
    val id: Int
)
