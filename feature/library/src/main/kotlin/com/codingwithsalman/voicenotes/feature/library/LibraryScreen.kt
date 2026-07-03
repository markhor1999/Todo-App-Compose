package com.codingwithsalman.voicenotes.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.voicenotes.core.common.util.formatDurationMs
import com.codingwithsalman.voicenotes.core.common.util.formatNoteDate
import com.codingwithsalman.voicenotes.core.designsystem.components.EmptyState
import com.codingwithsalman.voicenotes.core.designsystem.components.NoteCard
import com.codingwithsalman.voicenotes.core.designsystem.components.RecordButton
import com.codingwithsalman.voicenotes.core.designsystem.components.VnIconChip
import com.codingwithsalman.voicenotes.core.designsystem.components.VnIcons
import com.codingwithsalman.voicenotes.core.model.Note

@Composable
fun LibraryScreen(
    onRecord: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importAudio) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            RecordButton(
                isRecording = false,
                amplitude = 0f,
                onClick = onRecord,
                size = 68.dp,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = when {
                                query.isNotBlank() ->
                                    if (notes.size == 1) "1 match" else "${notes.size} matches"
                                notes.isEmpty() -> "Record or import to begin"
                                notes.size == 1 -> "1 recording"
                                else -> "${notes.size} recordings"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    VnIconChip(
                        icon = VnIcons.ImportAudio,
                        contentDescription = "Import audio",
                        onClick = { importLauncher.launch(arrayOf("audio/*")) },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    VnIconChip(
                        icon = VnIcons.Tune,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    placeholder = {
                        Text(
                            text = "Search your words…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = VnIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }

            if (notes.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.6f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (query.isBlank()) {
                            EmptyState(
                                title = "Nothing here yet",
                                body = "Tap record and just talk. Your words become searchable notes — right on this phone.",
                            )
                        } else {
                            EmptyState(
                                title = "No matches",
                                body = "Nothing in your titles or transcripts matches \"$query\".",
                            )
                        }
                    }
                }
            } else {
                items(notes, key = Note::id) { note ->
                    NoteCard(
                        title = note.title,
                        meta = "${formatNoteDate(note.createdAtMs)} · ${formatDurationMs(note.durationMs)}",
                        status = note.status,
                        waveform = note.waveform,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { pendingDelete = note },
                        modifier = Modifier.animateItem(),
                        sharedKeyPrefix = "note-${note.id}",
                    )
                }
            }
        }
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this note?") },
            text = { Text("\"${note.title}\" and its audio will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(note)
                        pendingDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}
