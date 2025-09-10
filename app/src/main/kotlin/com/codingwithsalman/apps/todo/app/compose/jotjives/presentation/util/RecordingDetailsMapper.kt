package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util


import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation.NavigationRoute
import com.codingwithsalman.jotjive.core.domain.recording.RecordingDetails
import kotlin.time.Duration.Companion.milliseconds

fun RecordingDetails.toCreateJiveRoute(): NavigationRoute.CreateJive {
    return NavigationRoute.CreateJive(
        recordingPath = this.filePath ?: throw IllegalArgumentException(
            "Recording path can't be null."
        ),
        duration = this.duration.inWholeMilliseconds,
        amplitudes = this.amplitudes.joinToString(";")
    )
}

fun NavigationRoute.CreateJive.toRecordingDetails(): RecordingDetails {
    return RecordingDetails(
        duration = this.duration.milliseconds,
        amplitudes = this.amplitudes.split(";").map { it.toFloat() },
        filePath = recordingPath
    )
}