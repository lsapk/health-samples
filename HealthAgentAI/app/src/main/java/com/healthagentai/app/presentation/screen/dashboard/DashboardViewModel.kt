package com.healthagentai.app.presentation.screen.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.healthagentai.app.data.HealthDataRepository
import com.healthagentai.app.data.database.CaloriesEntity
import com.healthagentai.app.data.database.HeartRateEntity
import com.healthagentai.app.data.database.OxygenSaturationEntity
import com.healthagentai.app.data.database.SleepSessionEntity
import com.healthagentai.app.data.database.StepsEntity
import com.healthagentai.app.data.database.StressEntity
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: HealthDataRepository) : ViewModel() {

    val latestHeartRate: MutableState<HeartRateEntity?> = mutableStateOf(null)
    val latestSleepSession: MutableState<SleepSessionEntity?> = mutableStateOf(null)
    val latestSteps: MutableState<StepsEntity?> = mutableStateOf(null)
    val latestOxygenSaturation: MutableState<OxygenSaturationEntity?> = mutableStateOf(null)
    val latestCalories: MutableState<CaloriesEntity?> = mutableStateOf(null)
    val latestStress: MutableState<StressEntity?> = mutableStateOf(null)

    init {
        viewModelScope.launch {
            repository.sync()
            loadLatestData()
        }
    }

    private suspend fun loadLatestData() {
        latestHeartRate.value = repository.getLatestHeartRate()
        latestSleepSession.value = repository.getLatestSleepSession()
        latestSteps.value = repository.getLatestSteps()
        latestOxygenSaturation.value = repository.getLatestOxygenSaturation()
        latestCalories.value = repository.getLatestCalories()
        latestStress.value = repository.getLatestStress()
    }
}

class DashboardViewModelFactory(
    private val repository: HealthDataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository = repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
