package com.healthagentai.app.data

import java.time.Instant
import java.time.temporal.ChronoUnit

class AIHealthCoach(private val repository: HealthDataRepository) {

    suspend fun getResponse(prompt: String): String {
        // Simple keyword-based intent detection
        return when {
            prompt.contains("week", ignoreCase = true) -> analyzeLastWeek()
            else -> "I can analyze your week's health data. Try asking 'Analyze my week'."
        }
    }

    private suspend fun analyzeLastWeek(): String {
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)

        val heartRateData = repository.getHeartRateBetween(sevenDaysAgo, now)
        val sleepData = repository.getSleepSessionsBetween(sevenDaysAgo, now)
        val stepsData = repository.getStepsBetween(sevenDaysAgo, now)

        // Simulate building a prompt for an LLM
        val dataSummary = """
            Here is the user's health data for the last 7 days:
            - Heart Rate records found: ${heartRateData.size}
            - Sleep sessions found: ${sleepData.size}
            - Daily steps records found: ${stepsData.size}

            The user wants an analysis of their week.
        """.trimIndent()

        // Simulate an LLM response based on the data
        val avgSleep = if (sleepData.isNotEmpty()) sleepData.mapNotNull { it.duration?.toHours() }.average() else 0.0
        val totalSteps = if (stepsData.isNotEmpty()) stepsData.sumOf { it.count } else 0

        return """
            Here is an analysis of your last week:

            - *Sleep:* You've had ${sleepData.size} sleep sessions, with an average of ${"%.1f".format(avgSleep)} hours per night.
            - *Activity:* You've taken a total of $totalSteps steps.

            *General Advice:* Based on this, maintaining a consistent sleep schedule could further improve your recovery. Keep up the great work on your daily steps!
        """.trimIndent()
    }
}
