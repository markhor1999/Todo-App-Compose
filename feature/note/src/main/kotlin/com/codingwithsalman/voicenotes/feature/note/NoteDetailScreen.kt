package com.codingwithsalman.voicenotes.feature.note

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.codingwithsalman.voicenotes.core.designsystem.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.core.common.util.formatDurationMs
import com.codingwithsalman.voicenotes.core.common.util.formatNoteDate
import com.codingwithsalman.voicenotes.core.designsystem.components.ProPaywallSheet
import com.codingwithsalman.voicenotes.core.designsystem.components.StaticWaveform
import com.codingwithsalman.voicenotes.core.designsystem.components.StatusBadge
import com.codingwithsalman.voicenotes.core.designsystem.components.vnSharedBounds
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme
import com.codingwithsalman.voicenotes.core.model.ActionItem
import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import com.codingwithsalman.voicenotes.core.model.TranscriptionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val actionItems by viewModel.actionItems.collectAsStateWithLifecycle()
    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val durationMs by viewModel.player.durationMs.collectAsStateWithLifecycle()
    val positionMs by viewModel.player.positionMs.collectAsStateWithLifecycle(initialValue = 0L)
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val transcriptionProgress by viewModel.transcriptionProgress.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val remainingTodayMs by viewModel.remainingTodayMs.collectAsStateWithLifecycle()
    val pricing by viewModel.pricing.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val screenContext = androidx.compose.ui.platform.LocalContext.current

    var showRename by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }

    val exportTxt = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.TXT.mimeType)
    ) { uri -> uri?.let { viewModel.exportTo(it, ExportFormat.TXT) } }
    val exportMd = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.MARKDOWN.mimeType)
    ) { uri -> uri?.let { viewModel.exportTo(it, ExportFormat.MARKDOWN) } }
    val exportSrt = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.SRT.mimeType)
    ) { uri -> uri?.let { viewModel.exportTo(it, ExportFormat.SRT) } }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        val current = note ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.vn_cd_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.vn_cd_more),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        val hasTranscript = segments.isNotEmpty()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_rename)) },
                            onClick = { showMenu = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_share_transcript)) },
                            enabled = hasTranscript,
                            onClick = { showMenu = false; viewModel.shareTranscript() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_share_audio)) },
                            onClick = { showMenu = false; viewModel.shareAudio() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_export_txt)) },
                            enabled = hasTranscript,
                            onClick = {
                                showMenu = false
                                exportTxt.launch(viewModel.exportFileName(ExportFormat.TXT))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_export_md)) },
                            enabled = hasTranscript,
                            onClick = {
                                showMenu = false
                                exportMd.launch(viewModel.exportFileName(ExportFormat.MARKDOWN))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vn_menu_export_srt)) },
                            enabled = hasTranscript,
                            onClick = {
                                showMenu = false
                                exportSrt.launch(viewModel.exportFileName(ExportFormat.SRT))
                            },
                        )
                    }
                }
            }

            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .vnSharedBounds("note-${current.id}-title")
                    .clickable { showRename = true },
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatNoteDate(screenContext, current.createdAtMs)} · ${formatDurationMs(current.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = current.status)
            }

            Spacer(modifier = Modifier.height(20.dp))

            PlayerCard(
                modifier = Modifier.vnSharedBounds("note-${current.id}-card"),
                bars = current.waveform ?: remember(current.id) { pseudoBars(current.id) },
                progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                positionLabel = "${formatDurationMs(positionMs)} / ${formatDurationMs(
                    if (durationMs > 0) durationMs else current.durationMs
                )}",
                isPlaying = isPlaying,
                speedLabel = when (playbackSpeed) {
                    1.5f -> "1.5×"
                    2f -> "2×"
                    else -> "1×"
                },
                onPlayPause = viewModel::playPause,
                onSeek = viewModel::seekToFraction,
                onCycleSpeed = viewModel::cyclePlaybackSpeed,
            )

            Spacer(modifier = Modifier.height(28.dp))

            TranscriptSection(
                status = current.status,
                segments = segments,
                engineState = engineState,
                transcriptionProgress = transcriptionProgress,
                currentPositionMs = positionMs,
                meterBlocked = !isPro && remainingTodayMs <= 0L,
                remainingTodayMs = remainingTodayMs,
                isPro = isPro,
                onDownloadModel = viewModel::downloadModel,
                onTranscribe = viewModel::transcribe,
                onSeekToSegment = viewModel::seekToMs,
                onOpenPaywall = { showPaywall = true },
            )

            Spacer(modifier = Modifier.height(28.dp))

            ActionItemsSection(
                items = actionItems,
                onAdd = viewModel::addActionItem,
                onToggle = viewModel::toggleActionItem,
                onDelete = viewModel::deleteActionItem,
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showPaywall) {
        ProPaywallSheet(
            isPro = isPro,
            monthlyPrice = pricing.monthlyPrice,
            lifetimePrice = pricing.lifetimePrice,
            onBuyMonthly = { activity?.let(viewModel::launchMonthly) },
            onBuyLifetime = { activity?.let(viewModel::launchLifetime) },
            onRestore = viewModel::restorePurchases,
            onDismiss = { showPaywall = false },
        )
    }

    if (showRename) {
        RenameDialog(
            initial = note?.title.orEmpty(),
            onConfirm = { viewModel.rename(it); showRename = false },
            onDismiss = { showRename = false },
        )
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vn_rename_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(stringResource(R.string.vn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.vn_cancel)) }
        },
    )
}

@Composable
private fun PlayerCard(
    modifier: Modifier = Modifier,
    bars: List<Float>,
    progress: Float,
    positionLabel: String,
    isPlaying: Boolean,
    speedLabel: String,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
) {
    val glow by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(600),
        label = "playerGlow",
    )
    val glowColor = VnTheme.extended.waveActive
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = VnTheme.extended.cardElevation,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (glow > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.16f * glow),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.42f),
                            radius = size.width * 0.75f,
                        ),
                        radius = size.width * 0.75f,
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                    )
                }
            }
            .border(
                border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                shape = RoundedCornerShape(24.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            StaticWaveform(
                bars = bars,
                progress = progress,
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.vn_cd_pause) else stringResource(R.string.vn_cd_play),
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = speedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onCycleSpeed)
                        .border(
                            border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = positionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Fade+rise once on first composition — the transcript-arrival moment. */
@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val rise = remember { Animatable(14f) }
    LaunchedEffect(Unit) {
        delay(min(index, 12) * 45L)
        launch { alpha.animateTo(1f, tween(240)) }
        launch { rise.animateTo(0f, tween(280)) }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = rise.value.dp.toPx()
        }
    ) { content() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranscriptSection(
    status: TranscriptionStatus,
    segments: List<TranscriptSegment>,
    engineState: EngineState,
    transcriptionProgress: Float?,
    currentPositionMs: Long,
    meterBlocked: Boolean,
    remainingTodayMs: Long,
    isPro: Boolean,
    onDownloadModel: () -> Unit,
    onTranscribe: () -> Unit,
    onSeekToSegment: (Long) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Text(
        text = stringResource(R.string.vn_note_transcript),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))

    when {
        segments.isNotEmpty() -> {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val currentSegmentId = segments.firstOrNull { segment ->
                currentPositionMs in segment.startMs until segment.endMs
            }?.id
            LaunchedEffect(currentSegmentId) {
                if (currentSegmentId != null) {
                    runCatching { bringIntoViewRequester.bringIntoView() }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                segments.forEachIndexed { index, segment ->
                    val isCurrent = segment.id == currentSegmentId
                    StaggeredIn(index) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCurrent) {
                                        Modifier.bringIntoViewRequester(bringIntoViewRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekToSegment(segment.startMs)
                                },
                        ) {
                            Text(
                                text = formatDurationMs(segment.startMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, end = 12.dp),
                            )
                            Text(
                                text = segment.text,
                                // Transcripts are user content: let the text pick its own
                                // direction (English stays LTR inside an RTL UI and vice versa).
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Content,
                                ),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        status == TranscriptionStatus.QUEUED || status == TranscriptionStatus.TRANSCRIBING -> {
            InfoCard {
                Text(
                    text = if (status == TranscriptionStatus.QUEUED) stringResource(R.string.vn_note_queued)
                    else stringResource(R.string.vn_note_transcribing_body),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                val progress = transcriptionProgress
                if (progress != null && progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.vn_percent_value, (progress * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        else -> when (val engine = engineState) {
            is EngineState.NotInstalled -> {
                InfoCard {
                    Text(
                        text = stringResource(R.string.vn_engine_get_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.vn_engine_get_body, engine.spec.approxSizeMb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = onDownloadModel) {
                        Text(
                            stringResource(
                                R.string.vn_engine_download_btn,
                                stringResource(
                                    if (engine.spec.languages == listOf("en")) R.string.vn_model_name_fast
                                    else R.string.vn_model_name_all
                                ),
                            )
                        )
                    }
                }
            }

            is EngineState.Downloading -> {
                InfoCard {
                    Text(
                        text = stringResource(R.string.vn_engine_downloading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { engine.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.vn_engine_progress, (engine.progress * 100).toInt(), engine.spec.approxSizeMb),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is EngineState.DownloadFailed -> {
                InfoCard {
                    Text(
                        text = stringResource(R.string.vn_engine_failed_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = engine.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = onDownloadModel) { Text(stringResource(R.string.vn_try_again)) }
                }
            }

            is EngineState.Ready -> {
                if (meterBlocked) {
                    InfoCard {
                        Text(
                            text = stringResource(R.string.vn_meter_used_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.vn_meter_used_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onOpenPaywall) { Text(stringResource(R.string.vn_see_pro)) }
                    }
                } else {
                    InfoCard {
                        Text(
                            text = when (status) {
                                TranscriptionStatus.FAILED ->
                                    stringResource(R.string.vn_failed_body)
                                TranscriptionStatus.DONE ->
                                    stringResource(R.string.vn_no_speech_body)
                                else ->
                                    stringResource(R.string.vn_ready_body)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!isPro) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Text(
                                    text = stringResource(R.string.vn_meter_left, remainingTodayMs / 60_000) + " · ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.vn_go_unlimited),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onOpenPaywall),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onTranscribe) {
                            Text(
                                when (status) {
                                    TranscriptionStatus.FAILED, TranscriptionStatus.DONE -> stringResource(R.string.vn_retry)
                                    else -> stringResource(R.string.vn_transcribe)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionItemsSection(
    items: List<ActionItem>,
    onAdd: (String) -> Unit,
    onToggle: (ActionItem) -> Unit,
    onDelete: (ActionItem) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var draft by remember { mutableStateOf("") }

    Text(
        text = stringResource(R.string.vn_actions_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))

    InfoCard {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggle(item)
                        },
                        onLongClick = { onDelete(item) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(if (item.done) 0.7f else 1f),
                )
            }
        }
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.vn_actions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.vn_actions_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onAdd(draft)
                        draft = ""
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.vn_cd_add_action),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = VnTheme.extended.cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

/** Deterministic decorative bars for legacy notes without a stored envelope. */
private fun pseudoBars(seed: Long, count: Int = 56): List<Float> =
    List(count) { i -> 0.22f + 0.72f * abs(sin(seed * 0.7 + i * 1.35)).toFloat() }
