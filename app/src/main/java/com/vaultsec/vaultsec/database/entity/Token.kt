package com.vaultsec.vaultsec.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_tokens")
data class Token(
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo val token: String
)