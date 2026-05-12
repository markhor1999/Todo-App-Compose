package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives

import com.codingwithsalman.apps.todo.app.compose.R
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.AudioCaptureMethod
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.JotJiveDaySection
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.JotJiveFilterChip
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.MoodChipContent
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.RecordingState
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotJiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.jotjive.core.presentation.designsystem.dropdowns.Selectable
import com.codingwithsalman.jotjive.core.presentation.util.UiText
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

data class JotJivesState(
//    val jives: Map<UiText, List<JiveUi>> = emptyMap(),
//    val jots: Map<UiText, List<JotUi>> = emptyMap(),
    val jotJives: Map<UiText, List<JotJiveUi>> = emptyMap(),
    val currentCaptureMethod: AudioCaptureMethod? = null,
    val recordingElapsedDuration: Duration = Duration.ZERO,
    val hasJivesRecorded: Boolean = false,
    val hasJotsAdded: Boolean = false,
    val hasActiveTopicFilters: Boolean = false,
    val hasActiveMoodFilters: Boolean = false,
    val isLoadingData: Boolean = true,
    val isLoadingJotsData: Boolean = true,
    val recordingState: RecordingState = RecordingState.NOT_RECORDING,
    val moods: List<Selectable<MoodUi>> = emptyList(),
    val topics: List<Selectable<String>> = emptyList(),
    val moodChipContent: MoodChipContent = MoodChipContent(),
    val selectedJotJiveFilterChip: JotJiveFilterChip? = null,
    val topicChipTitle: UiText = UiText.StringResource(R.string.all_topics)
) {
    val jotJiveDaySections = jotJives
        .toList()
        .map { (dateHeader, jives) ->
            JotJiveDaySection(dateHeader, jives)
        }

    val formattedRecordDuration: String
        get() {
            val minutes = (recordingElapsedDuration.inWholeMinutes % 60).toInt()
            val seconds = (recordingElapsedDuration.inWholeSeconds % 60).toInt()
            val centiseconds =
                ((recordingElapsedDuration.inWholeMilliseconds % 1000) / 10.0).roundToInt()

            return String.format(
                locale = Locale.US,
                format = "%02d:%02d:%02d",
                minutes, seconds, centiseconds
            )
        }
}
