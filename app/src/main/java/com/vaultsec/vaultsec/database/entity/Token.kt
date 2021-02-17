package com.vaultsec.vaultsec.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose

@Entity(tableName = "access_tokens")
data class Token(
    @ColumnInfo val token: String,
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)