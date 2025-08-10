package com.healthagentai.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "stress_level")
data class StressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val level: Long
)
