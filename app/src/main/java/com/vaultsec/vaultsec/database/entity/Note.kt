package com.vaultsec.vaultsec.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vaultsec.vaultsec.database.converter.DateConverter
import java.util.*

@Entity(tableName = "vault_notes")
data class Note(
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo val title: String?,
    @ColumnInfo val text: String,
    @ColumnInfo val color: String,
    @ColumnInfo(name = "font_size") val fontSize: Int,
    @ColumnInfo(name = "created_at_local") @TypeConverters(DateConverter::class) val createdAt: Date,
    @ColumnInfo(name = "updated_at_local") @TypeConverters(DateConverter::class) val updatedAt: Date
)