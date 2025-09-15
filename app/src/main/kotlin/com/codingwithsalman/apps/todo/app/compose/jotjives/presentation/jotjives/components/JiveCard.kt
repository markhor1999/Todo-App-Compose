@file:OptIn(ExperimentalLayoutApi::class)

package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.components.JiveMoodPlayer
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview.PreviewModels
import com.codingwithsalman.jotjive.core.presentation.designsystem.chips.HashtagChip
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme
import com.codingwithsalman.jotjive.core.presentation.util.defaultShadow

@Composable
fun JiveCard(
    jiveUi: JiveUi,
    onTrackSizeAvailable: (TrackSizeInfo) -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .defaultShadow(shape = RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = jiveUi.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = jiveUi.formattedRecordedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            JiveMoodPlayer(
                moodUi = jiveUi.mood,
                playbackState = jiveUi.playbackState,
                playerProgress = { jiveUi.playbackRatio },
                durationPlayed = jiveUi.playbackCurrentDuration,
                totalPlaybackDuration = jiveUi.playbackTotalDuration,
                powerRatios = jiveUi.amplitudes,
                onPlayClick = onPlayClick,
                onPauseClick = onPauseClick,
                onTrackSizeAvailable = onTrackSizeAvailable
            )

            if (!jiveUi.note.isNullOrBlank()) {
                JiveExpandableText(jiveUi.note)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                jiveUi.topics.forEach { topic ->
                    HashtagChip(text = topic)
                }
            }
        }
    }
}

@Preview
@Composable
private fun JiveCardPreview() {
    JotJiveTheme {
        JiveCard(
            jiveUi = PreviewModels.jiveUi.copy(
                mood = MoodUi.EXCITED
            ),
            onTrackSizeAvailable = {},
            onPauseClick = {},
            onPlayClick = {}
        )
    }
}