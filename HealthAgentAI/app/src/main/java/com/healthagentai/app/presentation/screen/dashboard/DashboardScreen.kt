package com.healthagentai.app.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthagentai.app.data.database.CaloriesEntity
import com.healthagentai.app.data.database.HeartRateEntity
import com.healthagentai.app.data.database.OxygenSaturationEntity
import com.healthagentai.app.data.database.SleepSessionEntity
import com.healthagentai.app.data.database.StepsEntity
import com.healthagentai.app.data.database.StressEntity
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val heartRate = viewModel.latestHeartRate.value
    val sleepSession = viewModel.latestSleepSession.value
    val steps = viewModel.latestSteps.value
    val spo2 = viewModel.latestOxygenSaturation.value
    val calories = viewModel.latestCalories.value
    val stress = viewModel.latestStress.value

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeartRateCard(heartRate = heartRate)
        }
        item {
            SleepSessionCard(sleepSession = sleepSession)
        }
        item {
            StepsCard(steps = steps)
        }
        item {
            OxygenSaturationCard(spo2 = spo2)
        }
        item {
            CaloriesCard(calories = calories)
        }
        item {
            StressCard(stress = stress)
        }
    }
}

@Composable
fun HeartRateCard(heartRate: HeartRateEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Heart Rate", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (heartRate != null) {
                Text("Latest: ${heartRate.beatsPerMinute} bpm")
                Text(
                    "At: ${heartRate.time.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun SleepSessionCard(sleepSession: com.healthagentai.app.data.database.SleepSessionEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sleep", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (sleepSession != null) {
                val duration = sleepSession.duration
                val hours = duration?.toHours()
                val minutes = duration?.toMinutes()?.rem(60)
                Text("Last Session: ${hours}h ${minutes}m")
                Text(
                    "Ended: ${sleepSession.endTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun StepsCard(steps: com.healthagentai.app.data.database.StepsEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Steps", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (steps != null) {
                Text("Total: ${steps.count}")
                Text(
                    "Until: ${steps.endTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun OxygenSaturationCard(spo2: com.healthagentai.app.data.database.OxygenSaturationEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("SpO2", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (spo2 != null) {
                Text("Latest: ${spo2.percentage}%")
                Text(
                    "At: ${spo2.time.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun CaloriesCard(calories: com.healthagentai.app.data.database.CaloriesEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Calories Burned", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (calories != null) {
                Text("Total: ${"%.0f".format(calories.calories)} kcal")
                Text(
                    "Until: ${calories.endTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}

@Composable
fun StressCard(stress: com.healthagentai.app.data.database.StressEntity?) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stress Level", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (stress != null) {
                Text("Latest: ${stress.level}/100")
                Text(
                    "At: ${stress.time.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.caption
                )
            } else {
                Text("Loading...")
            }
        }
    }
}
