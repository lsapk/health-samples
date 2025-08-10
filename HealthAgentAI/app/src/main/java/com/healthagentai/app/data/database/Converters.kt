package com.healthagentai.app.data.database

import androidx.room.TypeConverter
import java.time.Duration
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromDurationString(value: String?): Duration? {
        return value?.let { Duration.parse(it) }
    }

    @TypeConverter
    fun durationToString(duration: Duration?): String? {
        return duration?.toString()
    }
}
