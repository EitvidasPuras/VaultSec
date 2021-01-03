package com.vaultsec.vaultsec.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vaultsec.vaultsec.database.converter.DateConverter
import kotlinx.android.parcel.Parcelize
import java.sql.Timestamp

@Entity(tableName = "vault_notes")
@Parcelize // To be able to pass it to edit
data class Note(
    @ColumnInfo val title: String?,
    @ColumnInfo val text: String,
    @ColumnInfo val color: String,
    @ColumnInfo(name = "font_size") val fontSize: Int,
    @ColumnInfo(name = "created_at_local") @TypeConverters(DateConverter::class) val createdAt: Timestamp,
    @ColumnInfo(name = "updated_at_local") @TypeConverters(DateConverter::class) val updatedAt: Timestamp,
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Parcelable