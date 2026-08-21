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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.codingwithsalman.voicenotes.core.designsystem.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
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
    sharedAudioUris: List<android.net.Uri> = emptyList(),
    onSharedAudioConsumed: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val highlight by viewModel.highlight.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.vn_note_deleted)
    val undoLabel = stringResource(R.string.vn_undo)
    val sharedAddedMsg = stringResource(R.string.vn_shared_import_added)

    // Share-to-Murmur: audio handed in from another app → reuse the exact import path, then
    // confirm. Guarded by isNotEmpty so the post-consume recomposition is a no-op.
    LaunchedEffect(sharedAudioUris) {
        if (sharedAudioUris.isNotEmpty()) {
            sharedAudioUris.forEach(viewModel::importAudio)
            onSharedAudioConsumed()
            scope.launch { snackbarHostState.showSnackbar(sharedAddedMsg) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importAudio) }

    // Soft-delete + Undo: the note leaves the list at once; the snackbar's action restores it,
    // its timeout finalizes the purge. SnackbarDuration.Long ≈ the 10 s window the plan calls for.
    fun deleteWithUndo(note: Note) {
        viewModel.delete(note)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedMsg,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(note.id)
            else viewModel.finalizeDelete(note.id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = stringResource(R.string.vn_library_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                        )
                        Text(
                            text = when {
                                query.isNotBlank() ->
                                    pluralStringResource(R.plurals.vn_matches_count, notes.size, notes.size)
                                notes.isEmpty() -> stringResource(R.string.vn_library_empty_hint)
                                else -> pluralStringResource(R.plurals.vn_recordings_count, notes.size, notes.size)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    VnIconChip(
                        icon = VnIcons.ImportAudio,
                        contentDescription = stringResource(R.string.vn_cd_import_audio),
                        onClick = { importLauncher.launch(arrayOf("audio/*")) },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    VnIconChip(
                        icon = VnIcons.Tune,
                        contentDescription = stringResource(R.string.vn_cd_settings),
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
                            text = stringResource(R.string.vn_search_placeholder),
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
                                    contentDescription = stringResource(R.string.vn_cd_clear_search),
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

            if (highlight != LibraryViewModel.Highlight.Hidden) {
                item(key = "autotasks-highlight") {
                    ActionItemsHighlightCard(
                        state = highlight,
                        onRun = viewModel::runBackfill,
                        onDismiss = viewModel::dismissHighlight,
                    )
                }
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
                                title = stringResource(R.string.vn_empty_title),
                                body = stringResource(R.string.vn_empty_body),
                            )
                        } else {
                            EmptyState(
                                title = stringResource(R.string.vn_no_matches_title),
                                body = stringResource(R.string.vn_no_matches_body, query),
                            )
                        }
                    }
                }
            } else {
                items(notes, key = Note::id) { note ->
                    NoteCard(
                        title = note.title,
                        meta = "${formatNoteDate(context, note.createdAtMs)} · ${formatDurationMs(note.durationMs)}",
                        status = note.status,
                        waveform = note.waveform,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { deleteWithUndo(note) },
                        modifier = Modifier.animateItem(),
                        sharedKeyPrefix = "note-${note.id}",
                    )
                }
            }
        }
    }
}


/**
 * One-time card offering to run extraction over notes that predate the feature.
 *
 * It reports back in place rather than vanishing on tap: someone who just asked the app to read
 * their whole library deserves to be told what it found, and "Found 12 in 4 notes" is the only
 * moment where the feature demonstrates itself on the user's *own* content.
 */
@Composable
private fun ActionItemsHighlightCard(
    state: LibraryViewModel.Highlight,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.vn_autotasks_new_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.vn_autotasks_new_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is LibraryViewModel.Highlight.Done -> {
                    val result = state.result
                    Text(
                        text = if (result.itemCount == 0) {
                            stringResource(R.string.vn_autotasks_backfill_none)
                        } else {
                            stringResource(
                                R.string.vn_autotasks_backfill_done,
                                result.itemCount,
                                result.noteCount,
                            )
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                }

                LibraryViewModel.Highlight.Running -> {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onRun) {
                            Text(stringResource(R.string.vn_autotasks_new_cta))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.vn_autotasks_new_dismiss))
                        }
                    }
                }
            }
        }
    }
}
