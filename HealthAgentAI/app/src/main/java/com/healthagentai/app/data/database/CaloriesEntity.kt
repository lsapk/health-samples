package com.healthagentai.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "calories_burned")
data class CaloriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Instant,
    val endTime: Instant,
    val calories: Double
)
