package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models

import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.toReadableTime
import java.time.Instant as JavaInstant

data class JotUi(
    val id: Int,
    val title: String,
    val mood: MoodUi,
    val addedAt: JavaInstant,
    val note: String?,
    val topics: List<String>,
    val isTodo: Boolean = false,
    val isDone: Boolean = false
) {
    val formattedRecordedAt = addedAt.toReadableTime()
}
