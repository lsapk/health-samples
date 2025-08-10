package com.healthagentai.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "oxygen_saturation")
data class OxygenSaturationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val percentage: Double
)
