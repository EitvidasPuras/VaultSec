package com.vaultsec.vaultsec.database.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.vaultsec.vaultsec.database.converter.DateConverter
import com.vaultsec.vaultsec.util.exclusion.Exclude
import kotlinx.parcelize.Parcelize
import java.sql.Timestamp

@Entity(tableName = "vault_notes")
@Parcelize // To be able to pass it to edit
data class Note(
    @ColumnInfo var title: String?,
    @ColumnInfo var text: String,
    @ColumnInfo val color: String,
    @SerializedName("font_size")
    @ColumnInfo(name = "font_size") val fontSize: Int,
    @SerializedName("created_at_device")
    @ColumnInfo(name = "created_at_local") @TypeConverters(DateConverter::class) val createdAt: Timestamp,
    @SerializedName("updated_at_device")
    @ColumnInfo(name = "updated_at_local") @TypeConverters(DateConverter::class) val updatedAt: Timestamp,
    @ColumnInfo(name = "sync_state") @Exclude var syncState: Int = 1,
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Parcelable