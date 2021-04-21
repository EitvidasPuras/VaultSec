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

@Entity(tableName = "vault_passwords")
@Parcelize
data class Password(
    @ColumnInfo var title: String?, // encrypt
    @ColumnInfo val url: String?,
    @ColumnInfo var login: String?, // encrypt
    @ColumnInfo var password: String, // encrypt
    @ColumnInfo val category: String,
    @ColumnInfo val color: String,
    @SerializedName("created_at_device")
    @ColumnInfo(name = "created_at_local") @TypeConverters(DateConverter::class) val createdAt: Timestamp,
    @SerializedName("updated_at_device")
    @ColumnInfo(name = "updated_at_local") @TypeConverters(DateConverter::class) val updatedAt: Timestamp,
    @ColumnInfo(name = "sync_state") @Exclude var syncState: Int = 1,
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Parcelable