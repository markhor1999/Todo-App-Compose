package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    title: String,
    meta: String,
    status: TranscriptionStatus,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    waveform: List<Float>? = null,
    sharedKeyPrefix: String? = null,
) {
    Surface(
        modifier = modifier
            .then(
                if (sharedKeyPrefix != null) Modifier.vnSharedBounds("$sharedKeyPrefix-card")
                else Modifier
            )
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                shape = RoundedCornerShape(20.dp),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = VnTheme.extended.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (sharedKeyPrefix != null) {
                    Modifier.vnSharedBounds("$sharedKeyPrefix-title")
                } else {
                    Modifier
                },
            )
            if (!waveform.isNullOrEmpty()) {
                StaticWaveform(
                    bars = waveform,
                    progress = 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    barWidth = 2.5.dp,
                    gap = 2.dp,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBadge(status = status)
            }
        }
    }
}

@Composable
fun StatusBadge(status: TranscriptionStatus, modifier: Modifier = Modifier) {
    val (label, container, content) = when (status) {
        TranscriptionStatus.RECORDED -> Triple(
            "Audio",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TranscriptionStatus.QUEUED,
        TranscriptionStatus.TRANSCRIBING -> Triple(
            "Transcribing…",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        TranscriptionStatus.DONE -> Triple(
            "Transcript",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
        )
        TranscriptionStatus.FAILED -> Triple(
            "Failed",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.error,
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
