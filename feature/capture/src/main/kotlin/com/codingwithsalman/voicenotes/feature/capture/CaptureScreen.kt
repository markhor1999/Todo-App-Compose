package com.codingwithsalman.voicenotes.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.codingwithsalman.voicenotes.core.designsystem.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.voicenotes.core.common.util.formatTimerMs
import com.codingwithsalman.voicenotes.core.designsystem.components.LiveWaveform
import com.codingwithsalman.voicenotes.core.designsystem.components.RecordButton
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme

@Composable
fun CaptureScreen(
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // POST_NOTIFICATIONS (13+): requested after mic so the recording notification
    // can show; recording proceeds either way.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.state.value.isRecording) return@LaunchedEffect // returning to a live session
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startRecording()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordingSessionEvent.Saved -> onSaved(event.noteId)
                RecordingSessionEvent.Discarded -> onClose()
            }
        }
    }

    // No BackHandler: back leaves the screen while the session keeps recording in
    // the foreground service. Cancel is the only way to discard a take.

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { viewModel.discard() }) {
                    Text(stringResource(R.string.vn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.vn_capture_hide), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.weight(0.8f))

            RecordingIndicator(visible = state.isRecording, paused = state.isPaused)

            Text(
                text = formatTimerMs(state.elapsedMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(36.dp))

            LiveWaveform(
                amplitudes = state.amplitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(120.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.pauseResume()
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Rounded.Mic else Icons.Rounded.Pause,
                        contentDescription = if (state.isPaused) stringResource(R.string.vn_cd_resume) else stringResource(R.string.vn_cd_pause),
                    )
                }
                RecordButton(
                    isRecording = state.isRecording && !state.isPaused,
                    amplitude = if (state.isPaused) 0f else state.amplitudes.lastOrNull() ?: 0f,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.stopAndSave()
                    },
                    size = 92.dp,
                )
                Spacer(modifier = Modifier.size(56.dp))
            }
            Text(
                text = if (state.isPaused) stringResource(R.string.vn_capture_hint_paused)
                else stringResource(R.string.vn_capture_hint_recording),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 40.dp),
            )
        }
    }
}

@Composable
private fun RecordingIndicator(visible: Boolean, paused: Boolean = false) {
    val blink by rememberInfiniteTransition(label = "recBlink").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recBlinkAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(bottom = 14.dp)
            .alpha(if (visible) 1f else 0f),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .alpha(if (paused) 0.4f else blink)
                .clip(CircleShape)
                .background(VnTheme.extended.record),
        )
        Text(
            text = if (paused) stringResource(R.string.vn_capture_paused) else stringResource(R.string.vn_capture_recording),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
