package com.healthagentai.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthDataDao {
    // Heart Rate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: HeartRateEntity)

    @Query("SELECT * FROM heart_rate ORDER BY time DESC LIMIT 1")
    suspend fun getLatestHeartRate(): HeartRateEntity?

    @Query("SELECT * FROM heart_rate WHERE time BETWEEN :startTime AND :endTime")
    suspend fun getHeartRateBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<HeartRateEntity>

    // Sleep Session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSession(sleepSession: SleepSessionEntity)

    @Query("SELECT * FROM sleep_session ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSleepSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getSleepSessionsBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<SleepSessionEntity>

    // Steps
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: StepsEntity)

    @Query("SELECT * FROM steps ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestSteps(): StepsEntity?

    @Query("SELECT * FROM steps WHERE endTime BETWEEN :startTime AND :endTime")
    suspend fun getStepsBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<StepsEntity>

    // Oxygen Saturation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOxygenSaturation(oxygenSaturation: OxygenSaturationEntity)

    @Query("SELECT * FROM oxygen_saturation ORDER BY time DESC LIMIT 1")
    suspend fun getLatestOxygenSaturation(): OxygenSaturationEntity?

    @Query("SELECT * FROM oxygen_saturation WHERE time BETWEEN :startTime AND :endTime")
    suspend fun getOxygenSaturationBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<OxygenSaturationEntity>

    // Calories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalories(calories: CaloriesEntity)

    @Query("SELECT * FROM calories_burned ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestCalories(): CaloriesEntity?

    @Query("SELECT * FROM calories_burned WHERE endTime BETWEEN :startTime AND :endTime")
    suspend fun getCaloriesBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<CaloriesEntity>

    // Stress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStress(stress: StressEntity)

    @Query("SELECT * FROM stress_level ORDER BY time DESC LIMIT 1")
    suspend fun getLatestStress(): StressEntity?

    @Query("SELECT * FROM stress_level WHERE time BETWEEN :startTime AND :endTime")
    suspend fun getStressBetween(startTime: java.time.Instant, endTime: java.time.Instant): List<StressEntity>
}
