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

@Entity(tableName = "vault_cards")
@Parcelize
data class PaymentCard(
    @ColumnInfo var title: String?,
    @SerializedName("card_number")
    @ColumnInfo(name = "card_number") var cardNumber: String, // encrypt
    @SerializedName("expiration_mm")
    @ColumnInfo(name = "expiration_mm") var mm: String, // encrypt
    @SerializedName("expiration_yy")
    @ColumnInfo(name = "expiration_yy") var yy: String, // encrypt
    @ColumnInfo val type: String,
    @ColumnInfo var cvv: String, // encrypt
    @ColumnInfo var pin: String, // encrypt
    @SerializedName("created_at_device")
    @ColumnInfo(name = "created_at_local") @TypeConverters(DateConverter::class) val createdAt: Timestamp,
    @SerializedName("updated_at_device")
    @ColumnInfo(name = "updated_at_local") @TypeConverters(DateConverter::class) val updatedAt: Timestamp,
    @ColumnInfo(name = "sync_state") @Exclude var syncState: Int = 1,
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Parcelable
