package com.vaultsec.vaultsec.database.converter

import androidx.room.TypeConverter
import java.sql.Timestamp
import java.util.*

class DateConverter {
    @TypeConverter
    fun toDate(value: Long?): Timestamp? {
        return if (value == null) null else Timestamp(value)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }
}