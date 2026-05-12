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
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview.PreviewModels
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme


private val noVerticalLineAboveIconModifier = Modifier.padding(top = 16.dp)
private val noVerticalLineBelowIconModifier = Modifier.height(8.dp)

@Composable
fun JotTimelineItem(
    modifier: Modifier = Modifier,
    jotUi: JotUi,
    relativePosition: RelativePosition,
    markJotDone: (JotUi) -> Unit
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
                imageVector = ImageVector.vectorResource(jotUi.mood.iconSet.fill),
                contentDescription = jotUi.title,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        JotCard(
            jotUi = jotUi,
            modifier = Modifier
                .padding(vertical = 8.dp),
            markDone = markJotDone
        )
    }
}

@Preview
@Composable
private fun Preview() {
    JotJiveTheme {
        JotTimelineItem(
            jotUi = PreviewModels.jotUi,
            relativePosition = RelativePosition.IN_BETWEEN,
            modifier = Modifier
                .fillMaxWidth(),
            markJotDone = {}
        )
    }
}