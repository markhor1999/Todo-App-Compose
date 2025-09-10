package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JiveFilterRow
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JiveList
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JiveQuickRecordFloatingActionButton
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JiveRecordingSheet
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JivesEmptyBackground
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components.JivesTopBar
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.AudioCaptureMethod
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.RecordingState
import com.codingwithsalman.jotjive.core.domain.recording.RecordingDetails
import com.codingwithsalman.jotjive.core.presentation.util.ObserveAsEvents
import com.codingwithsalman.jotjive.core.presentation.util.isAppInForeground
import org.koin.androidx.compose.koinViewModel

@Composable
fun JotJiveRoot(
    onNavigateToCreateJive: (RecordingDetails) -> Unit,
    onNavigateToCreateJot: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: JotJivesViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && state.currentCaptureMethod == AudioCaptureMethod.STANDARD) {
            viewModel.onAction(JotJivesAction.OnAudioPermissionGranted)
        }
    }

    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is JotJivesEvent.RequestAudioPermission -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            is JotJivesEvent.RecordingTooShort -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.audio_recording_was_too_short),
                    Toast.LENGTH_LONG
                ).show()
            }

            is JotJivesEvent.OnDoneRecording -> {
                onNavigateToCreateJive(event.details)
            }

            is JotJivesEvent.AddJot -> {
                onNavigateToCreateJot()
            }
        }
    }

    val isAppInForeground by isAppInForeground()
    LaunchedEffect(isAppInForeground, state.recordingState) {
        if (state.recordingState == RecordingState.NORMAL_CAPTURE && !isAppInForeground) {
            viewModel.onAction(JotJivesAction.OnPauseRecordingClick)
        }
    }

    JotJivesScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is JotJivesAction.OnSettingsClick -> onNavigateToSettings()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
private fun JotJivesScreen(
    state: JotJivesState,
    onAction: (JotJivesAction) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = {
                        onAction(JotJivesAction.OnAddJotClick)
                    },
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.add_notes)
                    )
                }

                JiveQuickRecordFloatingActionButton(
                    onClick = {
                        onAction(JotJivesAction.OnRecordFabClick)
                    },
                    isQuickRecording = state.recordingState == RecordingState.QUICK_CAPTURE,
                    onLongPressEnd = { cancelledRecording ->
                        if (cancelledRecording) {
                            onAction(JotJivesAction.OnCancelRecording)
                        } else {
                            onAction(JotJivesAction.OnCompleteRecording)
                        }
                    },
                    onLongPressStart = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            onAction(JotJivesAction.OnRecordButtonLongClick)
                        } else {
                            onAction(JotJivesAction.OnRequestPermissionQuickRecording)
                        }
                    }
                )
            }
        },
        topBar = {
            JivesTopBar(
                onSettingsClick = {
                    onAction(JotJivesAction.OnSettingsClick)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            JiveFilterRow(
                moodChipContent = state.moodChipContent,
                hasActiveMoodFilters = state.hasActiveMoodFilters,
                selectedJiveFilterChip = state.selectedJotJiveFilterChip,
                moods = state.moods,
                topicChipTitle = state.topicChipTitle,
                hasActiveTopicFilters = state.hasActiveTopicFilters,
                topics = state.topics,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
            )
            when {
                state.isLoadingData -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .wrapContentSize(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                !state.hasJotJivesRecorded -> {
                    JivesEmptyBackground(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                else -> {
                    JiveList(
                        sections = state.jotJiveDaySections,
                        onPlayClick = {
                            onAction(JotJivesAction.OnPlayJiveClick(it))
                        },
                        onPauseClick = {
                            onAction(JotJivesAction.OnPauseAudioClick)
                        },
                        onTrackSizeAvailable = { trackSize ->
                            onAction(JotJivesAction.OnTrackSizeAvailable(trackSize))
                        }
                    )
                }
            }
        }

        if (state.recordingState in listOf(RecordingState.NORMAL_CAPTURE, RecordingState.PAUSED)) {
            JiveRecordingSheet(
                formattedRecordDuration = state.formattedRecordDuration,
                isRecording = state.recordingState == RecordingState.NORMAL_CAPTURE,
                onDismiss = { onAction(JotJivesAction.OnCancelRecording) },
                onPauseClick = { onAction(JotJivesAction.OnPauseRecordingClick) },
                onResumeClick = { onAction(JotJivesAction.OnResumeRecordingClick) },
                onCompleteRecording = { onAction(JotJivesAction.OnCompleteRecording) },
            )
        }
    }
}