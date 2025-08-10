/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.healthagentai.app.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.healthagentai.app.data.HealthConnectManager
import com.healthagentai.app.presentation.screen.SettingsScreen
import com.healthagentai.app.presentation.screen.WelcomeScreen
import com.healthagentai.app.presentation.screen.changes.DifferentialChangesScreen
import com.healthagentai.app.presentation.screen.changes.DifferentialChangesViewModel
import com.healthagentai.app.presentation.screen.changes.DifferentialChangesViewModelFactory
import com.healthagentai.app.presentation.screen.exercisesession.ExerciseSessionScreen
import com.healthagentai.app.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.healthagentai.app.presentation.screen.exercisesession.ExerciseSessionViewModelFactory
import com.healthagentai.app.presentation.screen.exercisesessiondetail.ExerciseSessionDetailScreen
import com.healthagentai.app.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModel
import com.healthagentai.app.presentation.screen.exercisesessiondetail.ExerciseSessionDetailViewModelFactory
import com.healthagentai.app.presentation.screen.inputreadings.InputReadingsScreen
import com.healthagentai.app.presentation.screen.inputreadings.InputReadingsViewModel
import com.healthagentai.app.presentation.screen.inputreadings.InputReadingsViewModelFactory
import com.healthagentai.app.presentation.screen.privacypolicy.PrivacyPolicyScreen
import com.healthagentai.app.presentation.screen.recordlist.RecordType
import com.healthagentai.app.presentation.screen.recordlist.RecordListScreen
import com.healthagentai.app.presentation.screen.recordlist.RecordListScreenViewModel
import com.healthagentai.app.presentation.screen.recordlist.RecordListViewModelFactory
import com.healthagentai.app.presentation.screen.recordlist.SeriesRecordsType
import com.healthagentai.app.presentation.screen.sleepsession.SleepSessionScreen
import com.healthagentai.app.presentation.screen.sleepsession.SleepSessionViewModel
import com.healthagentai.app.presentation.screen.sleepsession.SleepSessionViewModelFactory
import com.healthagentai.app.showExceptionSnackbar
import kotlinx.coroutines.launch

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
    navController: NavHostController,
    healthConnectManager: HealthConnectManager,
    scaffoldState: ScaffoldState
) {
    val scope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        val availability by healthConnectManager.availability

        composable(Screen.Dashboard.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel: com.healthagentai.app.presentation.screen.dashboard.DashboardViewModel = viewModel(
                factory = com.healthagentai.app.presentation.screen.dashboard.DashboardViewModelFactory(
                    repository = (context.applicationContext as com.healthagentai.app.presentation.BaseApplication).healthDataRepository
                )
            )
            com.healthagentai.app.presentation.screen.dashboard.DashboardScreen(viewModel = viewModel)
        }

        composable(Screen.Chat.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val application = context.applicationContext as com.healthagentai.app.presentation.BaseApplication
            val viewModel: com.healthagentai.app.presentation.screen.chat.ChatViewModel = viewModel(
                factory = com.healthagentai.app.presentation.screen.chat.ChatViewModelFactory(
                    repository = application.healthDataRepository,
                    aiHealthCoach = application.aiHealthCoach
                )
            )
            com.healthagentai.app.presentation.screen.chat.ChatScreen(viewModel = viewModel)
        }

        composable(Screen.WelcomeScreen.route) {
            WelcomeScreen(
                healthConnectAvailability = availability,
                onResumeAvailabilityCheck = {
                    healthConnectManager.checkAvailability()
                }
            )
        }
        composable(
            route = Screen.PrivacyPolicy.route,
            deepLinks = listOf(
                navDeepLink {
                    action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
                }
            )
        ) {
            PrivacyPolicyScreen()
        }
        composable(Screen.SettingsScreen.route){
            SettingsScreen { scope.launch { healthConnectManager.revokeAllPermissions() } }
        }
        composable(Screen.ExerciseSessions.route) {
            val viewModel: ExerciseSessionViewModel = viewModel(
                factory = ExerciseSessionViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val permissions = viewModel.permissions
            val backgroundReadAvailable by viewModel.backgroundReadAvailable
            val backgroundReadGranted by viewModel.backgroundReadGranted
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            ExerciseSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = backgroundReadGranted,
                sessionsList = sessionsList,
                uiState = viewModel.uiState,
                onInsertClick = {
                    viewModel.insertExerciseSession()
                },
                onDetailsClick = { uid ->
                    navController.navigate(Screen.ExerciseSessionDetail.route + "/" + uid)
                },
                onDeleteClick = { uid ->
                    viewModel.deleteExerciseSession(uid)
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.ExerciseSessionDetail.route + "/{$UID_NAV_ARGUMENT}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val viewModel: ExerciseSessionDetailViewModel = viewModel(
                factory = ExerciseSessionDetailViewModelFactory(
                    uid = uid,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionMetrics by viewModel.sessionMetrics
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            ExerciseSessionDetailScreen(
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                sessionMetrics = sessionMetrics,
                uiState = viewModel.uiState,
                onDetailsClick = { recordType, recordId, seriesRecordsType ->
                    navController.navigate(Screen.RecordListScreen.route + "/" + recordType + "/"+ recordId + "/" + seriesRecordsType)
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.RecordListScreen.route + "/{$RECORD_TYPE}" + "/{$UID_NAV_ARGUMENT}" + "/{$SERIES_RECORDS_TYPE}") {
            val uid = it.arguments?.getString(UID_NAV_ARGUMENT)!!
            val recordTypeString = it.arguments?.getString(RECORD_TYPE)!!
            val seriesRecordsTypeString = it.arguments?.getString(SERIES_RECORDS_TYPE)!!
            val viewModel: RecordListScreenViewModel = viewModel(
                factory = RecordListViewModelFactory(
                    uid = uid,
                    recordTypeString = recordTypeString,
                    seriesRecordsTypeString = seriesRecordsTypeString,
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val recordList = viewModel.recordList
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                    onPermissionsResult()}
            RecordListScreen(
                uid = uid,
                permissions = permissions,
                permissionsGranted = permissionsGranted,
                recordType = RecordType.valueOf(recordTypeString),
                seriesRecordsType = SeriesRecordsType.valueOf(seriesRecordsTypeString),
                recordList = recordList,
                uiState = viewModel.uiState,
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.SleepSessions.route) {
            val viewModel: SleepSessionViewModel = viewModel(
                factory = SleepSessionViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val sessionsList by viewModel.sessionsList
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            SleepSessionScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                sessionsList = sessionsList,
                uiState = viewModel.uiState,
                onInsertClick = {
                    viewModel.generateSleepData()
                },
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.InputReadings.route) {
            val viewModel: InputReadingsViewModel = viewModel(
                factory = InputReadingsViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val permissionsGranted by viewModel.permissionsGranted
            val readingsList by viewModel.readingsList
            val permissions = viewModel.permissions
            val weeklyAvg by viewModel.weeklyAvg
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            InputReadingsScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,

                uiState = viewModel.uiState,
                onInsertClick = { weightInput ->
                    viewModel.inputReadings(weightInput)
                },
                weeklyAvg = weeklyAvg,
                onDeleteClick = { uid ->
                    viewModel.deleteWeightInput(uid)
                },
                readingsList = readingsList,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                },
                onPermissionsLaunch = { values ->
                    permissionsLauncher.launch(values)}
            )
        }
        composable(Screen.DifferentialChanges.route) {
            val viewModel: DifferentialChangesViewModel = viewModel(
                factory = DifferentialChangesViewModelFactory(
                    healthConnectManager = healthConnectManager
                )
            )
            val changesToken by viewModel.changesToken
            val permissionsGranted by viewModel.permissionsGranted
            val permissions = viewModel.permissions
            val onPermissionsResult = {viewModel.initialLoad()}
            val permissionsLauncher =
                rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
                onPermissionsResult()}
            DifferentialChangesScreen(
                permissionsGranted = permissionsGranted,
                permissions = permissions,
                changesEnabled = changesToken != null,
                onChangesEnable = { enabled ->
                    viewModel.enableOrDisableChanges(enabled)
                },
                changes = viewModel.changes,
                changesToken = changesToken,
                onGetChanges = {
                    viewModel.getChanges()
                },
                uiState = viewModel.uiState,
                onError = { exception ->
                    showExceptionSnackbar(scaffoldState, scope, exception)
                },
                onPermissionsResult = {
                    viewModel.initialLoad()
                }
            ) { values ->
                permissionsLauncher.launch(values)
            }
        }
    }
}
