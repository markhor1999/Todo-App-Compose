@file:OptIn(ExperimentalFoundationApi::class)

package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.JotJiveDaySection
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.RelativePosition
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview.PreviewModels
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme
import com.codingwithsalman.jotjive.core.presentation.util.UiText
import java.time.Instant
import java.time.ZonedDateTime

@Composable
fun JiveList(
    sections: List<JotJiveDaySection>,
    onPlayClick: (jiveId: Int) -> Unit,
    onPauseClick: () -> Unit,
    onTrackSizeAvailable: (TrackSizeInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        sections.forEachIndexed { sectionIndex, (dateHeader, jives) ->
            stickyHeader {
                if (sectionIndex > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = dateHeader.asString().uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(
                items = jives,
                key = { _, jive -> jive.id }
            ) { index, jive ->
                JiveTimelineItem(
                    jiveUi = jive,
                    relativePosition = when {
                        index == 0 && jives.size == 1 -> RelativePosition.SINGLE_ENTRY
                        index == 0 -> RelativePosition.FIRST
                        jives.lastIndex == index -> RelativePosition.LAST
                        else -> RelativePosition.IN_BETWEEN
                    },
                    onPlayClick = { onPlayClick(jive.id) },
                    onPauseClick = onPauseClick,
                    onTrackSizeAvailable = onTrackSizeAvailable
                )
            }
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
private fun JiveListPreview() {
    JotJiveTheme {
        val todaysEchos = remember {
            (1..3).map {
                PreviewModels.jiveUi.copy(
                    id = it,
                    recordedAt = Instant.now()
                )
            }
        }
        val yesterdaysEchos = remember {
            (4..6).map {
                PreviewModels.jiveUi.copy(
                    id = it,
                    recordedAt = ZonedDateTime.now().minusDays(1).toInstant()
                )
            }
        }
        val jivesFrom2DaysAgo = remember {
            (7..9).map {
                PreviewModels.jiveUi.copy(
                    id = it,
                    recordedAt = ZonedDateTime.now().minusDays(2).toInstant()
                )
            }
        }
        val sections = remember {
            listOf(
                JotJiveDaySection(
                    dateHeader = UiText.Dynamic("Today"),
                    jives = todaysEchos
                ),
                JotJiveDaySection(
                    dateHeader = UiText.Dynamic("Yesterday"),
                    jives = yesterdaysEchos
                ),
                JotJiveDaySection(
                    dateHeader = UiText.Dynamic("2025/04/25"),
                    jives = jivesFrom2DaysAgo
                ),
            )
        }

        JiveList(
            sections = sections,
            onPauseClick = {},
            onPlayClick = {},
            onTrackSizeAvailable = {}
        )
    }
}