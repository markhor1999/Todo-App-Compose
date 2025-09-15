package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.JotUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.models.MoodUi
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.preview.PreviewModels
import com.codingwithsalman.jotjive.core.presentation.designsystem.chips.HashtagChip
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme
import com.codingwithsalman.jotjive.core.presentation.util.defaultShadow


@Composable
fun JotCard(
    modifier: Modifier = Modifier,
    jotUi: JotUi,
    markDone: (JotUi) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .defaultShadow(shape = RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = jotUi.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (jotUi.isTodo && jotUi.isDone) {
                        TextDecoration.LineThrough
                    } else null
                )

                if (jotUi.isTodo)
                    Checkbox(
                        checked = jotUi.isDone,
                        onCheckedChange = { markDone(jotUi) }
                    )
                else
                    Text(
                        text = jotUi.formattedRecordedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }

            if (jotUi.isTodo)
                Text(
                    text = jotUi.formattedRecordedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            if (!jotUi.note.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                JiveExpandableText(jotUi.note)
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                jotUi.topics.forEach { topic ->
                    HashtagChip(text = topic)
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    JotJiveTheme {
        JotCard(
            jotUi = PreviewModels.jotUi.copy(
                mood = MoodUi.EXCITED
            ),
            markDone = {

            }
        )
    }
}