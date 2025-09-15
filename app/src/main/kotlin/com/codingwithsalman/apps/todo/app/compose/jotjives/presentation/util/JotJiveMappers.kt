package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.PlaybackState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.jotjive.core.domain.jive.Jive
import com.codingwithsalman.jotjive.core.domain.jive.Mood
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import kotlin.time.Duration

fun Jive.toJiveUi(
    currentPlaybackDuration: Duration = Duration.ZERO,
    playbackState: PlaybackState = PlaybackState.STOPPED
): JiveUi {
    return JiveUi(
        id = id!!,
        title = title,
        mood = MoodUi.valueOf(mood.name),
        recordedAt = recordedAt,
        note = note,
        topics = topics,
        amplitudes = audioAmplitudes,
        playbackTotalDuration = audioPlaybackLength,
        audioFilePath = audioFilePath,
        playbackCurrentDuration = currentPlaybackDuration,
        playbackState = playbackState
    )
}

fun Jot.toJotUi(): JotUi {
    return JotUi(
        id = id!!,
        title = title,
        mood = MoodUi.valueOf(mood.name),
        addedAt = addedAt,
        note = note,
        topics = topics,
        isTodo = isTodo,
        isDone = isDone
    )
}

fun JotUi.toJot(): Jot {
    return Jot(
        id = id,
        title = title,
        mood = Mood.valueOf(mood.name),
        addedAt = addedAt,
        note = note,
        topics = topics,
        isTodo = isTodo,
        isDone = isDone
    )
}