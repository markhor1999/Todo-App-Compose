package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.PlaybackState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

data object PreviewModels {

    val jiveUi = JiveUi(
        id = 0,
        title = "My audio memo",
        mood = MoodUi.STRESSED,
        recordedAt = Instant.now(),
        note = (1..50).map { "Hello" }.joinToString(" "),
        topics = listOf("Love", "Work"),
        amplitudes = (1..30).map { Random.nextFloat() },
        playbackTotalDuration = 250.seconds,
        playbackCurrentDuration = 120.seconds,
        playbackState = PlaybackState.PAUSED,
        audioFilePath = ""
    )

    val jotUi = JotUi(
        id = 0,
        title = "My audio memo",
        mood = MoodUi.STRESSED,
        addedAt = Instant.now(),
        note = (1..50).map { "Hello" }.joinToString(" "),
        topics = listOf("Love", "Work"),
    )
}