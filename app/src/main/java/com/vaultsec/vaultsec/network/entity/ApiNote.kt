package com.vaultsec.vaultsec.network.entity

import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import com.google.gson.annotations.Expose
import com.vaultsec.vaultsec.database.converter.DateConverter
import java.sql.Timestamp

data class ApiNote(
    val title: String?,
    val text: String,
    val color: String,
    val font_size: Int,
    @TypeConverters(DateConverter::class) val created_at_device: Timestamp,
    @TypeConverters(DateConverter::class) val updated_at_device: Timestamp,
    val id: Int
)