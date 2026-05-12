package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.PlaybackState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.toReadableTime
import kotlin.time.Duration
import java.time.Instant as JavaInstant

data class JiveUi(
    val id: Int,
    val title: String,
    val mood: MoodUi,
    val recordedAt: JavaInstant,
    val note: String?,
    val topics: List<String>,
    val amplitudes: List<Float>,
    val playbackTotalDuration: Duration,
    val audioFilePath: String,
    val playbackCurrentDuration: Duration = Duration.ZERO,
    val playbackState: PlaybackState = PlaybackState.STOPPED
) {
    val formattedRecordedAt = recordedAt.toReadableTime()
    val playbackRatio = (playbackCurrentDuration / playbackTotalDuration).toFloat()
}
