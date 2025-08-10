package com.healthagentai.app.data

import com.healthagentai.app.data.database.HealthDataDao
import java.time.Instant

class HealthDataRepository(
    private val healthConnectManager: HealthConnectManager,
    private val healthDataDao: HealthDataDao
) {

    suspend fun getLatestHeartRate() = healthDataDao.getLatestHeartRate()
    suspend fun getLatestSleepSession() = healthDataDao.getLatestSleepSession()
    suspend fun getLatestSteps() = healthDataDao.getLatestSteps()
    suspend fun getLatestOxygenSaturation() = healthDataDao.getLatestOxygenSaturation()
    suspend fun getLatestCalories() = healthDataDao.getLatestCalories()
    suspend fun getLatestStress() = healthDataDao.getLatestStress()

    suspend fun getHeartRateBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getHeartRateBetween(startTime, endTime)
    suspend fun getSleepSessionsBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getSleepSessionsBetween(startTime, endTime)
    suspend fun getStepsBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getStepsBetween(startTime, endTime)
    suspend fun getOxygenSaturationBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getOxygenSaturationBetween(startTime, endTime)
    suspend fun getCaloriesBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getCaloriesBetween(startTime, endTime)
    suspend fun getStressBetween(startTime: java.time.Instant, endTime: java.time.Instant) = healthDataDao.getStressBetween(startTime, endTime)

    suspend fun sync() {
        // Heart Rate
        val latestHeartRate = healthDataDao.getLatestHeartRate()
        val hrStartTime = latestHeartRate?.time ?: Instant.EPOCH
        val heartRateRecords = healthConnectManager.readHeartRateRecords(hrStartTime, Instant.now())
        heartRateRecords.forEach { record ->
            healthDataDao.insertHeartRate(
                com.healthagentai.app.data.database.HeartRateEntity(
                    time = record.time,
                    beatsPerMinute = record.beatsPerMinute
                )
            )
        }

        // Sleep Session
        val latestSleepSession = healthDataDao.getLatestSleepSession()
        val sleepStartTime = latestSleepSession?.startTime ?: Instant.EPOCH
        val sleepRecords = healthConnectManager.readSleepSessions(sleepStartTime, Instant.now())
        sleepRecords.forEach { record ->
            healthDataDao.insertSleepSession(
                com.healthagentai.app.data.database.SleepSessionEntity(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = java.time.Duration.between(record.startTime, record.endTime),
                    notes = record.notes
                )
            )
        }

        // Steps
        val latestSteps = healthDataDao.getLatestSteps()
        val stepsStartTime = latestSteps?.endTime ?: Instant.EPOCH
        val stepsRecords = healthConnectManager.readStepsRecords(stepsStartTime, Instant.now())
        stepsRecords.forEach { record ->
            healthDataDao.insertSteps(
                com.healthagentai.app.data.database.StepsEntity(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    count = record.count
                )
            )
        }

        // Oxygen Saturation
        val latestOxygen = healthDataDao.getLatestOxygenSaturation()
        val oxygenStartTime = latestOxygen?.time ?: Instant.EPOCH
        val oxygenRecords = healthConnectManager.readOxygenSaturationRecords(oxygenStartTime, Instant.now())
        oxygenRecords.forEach { record ->
            healthDataDao.insertOxygenSaturation(
                com.healthagentai.app.data.database.OxygenSaturationEntity(
                    time = record.time,
                    percentage = record.percentage.value
                )
            )
        }

        // Calories
        val latestCalories = healthDataDao.getLatestCalories()
        val caloriesStartTime = latestCalories?.endTime ?: Instant.EPOCH
        val caloriesRecords = healthConnectManager.readCaloriesRecords(caloriesStartTime, Instant.now())
        caloriesRecords.forEach { record ->
            healthDataDao.insertCalories(
                com.healthagentai.app.data.database.CaloriesEntity(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    calories = record.energy.inCalories
                )
            )
        }

        // Stress
        val latestStress = healthDataDao.getLatestStress()
        val stressStartTime = latestStress?.time ?: Instant.EPOCH
        val stressRecords = healthConnectManager.readStressRecords(stressStartTime, Instant.now())
        stressRecords.forEach { record ->
            healthDataDao.insertStress(
                com.healthagentai.app.data.database.StressEntity(
                    time = record.time,
                    level = record.level.value
                )
            )
        }
    }
}
