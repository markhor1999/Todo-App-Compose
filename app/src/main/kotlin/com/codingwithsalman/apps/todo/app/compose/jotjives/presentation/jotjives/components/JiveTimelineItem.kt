package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.RelativePosition
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.models.TrackSizeInfo
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JiveUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview.PreviewModels
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme

private val noVerticalLineAboveIconModifier = Modifier.padding(top = 16.dp)
private val noVerticalLineBelowIconModifier = Modifier.height(8.dp)

@Composable
fun JiveTimelineItem(
    jiveUi: JiveUi,
    relativePosition: RelativePosition,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onTrackSizeAvailable: (TrackSizeInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (relativePosition != RelativePosition.SINGLE_ENTRY) {
                VerticalDivider(
                    modifier = when (relativePosition) {
                        RelativePosition.FIRST -> noVerticalLineAboveIconModifier
                        RelativePosition.LAST -> noVerticalLineBelowIconModifier
                        RelativePosition.IN_BETWEEN -> Modifier
                        else -> Modifier
                    }
                )
            }

            Image(
                imageVector = ImageVector.vectorResource(jiveUi.mood.iconSet.fill),
                contentDescription = jiveUi.title,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        JiveCard(
            jiveUi = jiveUi,
            onTrackSizeAvailable = onTrackSizeAvailable,
            onPlayClick = onPlayClick,
            onPauseClick = onPauseClick,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
    }
}

@Preview
@Composable
private fun JiveTimelineItemPreview() {
    JotJiveTheme {
        JiveTimelineItem(
            jiveUi = PreviewModels.jiveUi,
            relativePosition = RelativePosition.IN_BETWEEN,
            onPlayClick = {},
            onPauseClick = {},
            onTrackSizeAvailable = {},
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}