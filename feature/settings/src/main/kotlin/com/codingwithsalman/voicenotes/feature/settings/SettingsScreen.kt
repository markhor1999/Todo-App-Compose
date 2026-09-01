@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.feature.settings

import com.tricodestudio.voicekit.ModelCatalog
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.codingwithsalman.voicenotes.core.designsystem.R
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
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.LiveModelState
import com.codingwithsalman.voicenotes.core.designsystem.components.ProPaywallSheet
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme
import com.codingwithsalman.voicenotes.core.model.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val liveEnabled by viewModel.liveEnabled.collectAsStateWithLifecycle()
    val autoTasksEnabled by viewModel.autoTasksEnabled.collectAsStateWithLifecycle()
    val liveModelState by viewModel.liveModelState.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val pricing by viewModel.pricing.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var showPaywall by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
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
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.vn_cd_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Text(
                text = stringResource(R.string.vn_settings_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            SectionTitle(stringResource(R.string.vn_pro_section))
            SettingsCard {
                if (isPro) {
                    Text(
                        text = stringResource(R.string.vn_pro_active),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.vn_pro_pitch_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.vn_pro_pitch_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = { showPaywall = true }) { Text(stringResource(R.string.vn_see_pro)) }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.vn_restore),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = viewModel::restorePurchases),
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.vn_appearance))
            SettingsCard {
                ThemeMode.entries.forEach { mode ->
                    OptionRow(
                        title = when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.vn_theme_system)
                            ThemeMode.DARK -> stringResource(R.string.vn_theme_dark)
                            ThemeMode.LIGHT -> stringResource(R.string.vn_theme_light)
                        },
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.vn_engine_section))
            SettingsCard {
                viewModel.models.forEach { spec ->
                    OptionRow(
                        // Three models now share two language buckets, so key the label off the id:
                        // "All languages" and "All languages · Compact" are both multilingual and
                        // would otherwise render identically (MUR-17).
                        title = stringResource(
                            when (spec.id) {
                                ModelCatalog.whisperTinyMultilingual.id ->
                                    R.string.vn_model_name_all_compact
                                else ->
                                    if (spec.languages == listOf("en")) R.string.vn_model_name_fast
                                    else R.string.vn_model_name_all
                            }
                        ),
                        subtitle = stringResource(
                            if (spec.languages == listOf("en")) R.string.vn_model_sub_fast
                            else R.string.vn_model_sub_all,
                            spec.approxSizeMb,
                        ),
                        selected = selectedModel.id == spec.id,
                        onClick = { viewModel.selectModel(spec) },
                    )
                }

                when (val engine = engineState) {
                    is EngineState.NotInstalled -> {
                        Button(
                            onClick = viewModel::downloadSelected,
                            modifier = Modifier.padding(top = 10.dp),
                        ) { Text(stringResource(R.string.vn_download_size, engine.spec.approxSizeMb)) }
                    }
                    is EngineState.Downloading -> {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            LinearProgressIndicator(
                                progress = { engine.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(R.string.vn_percent_value, (engine.progress * 100).toInt()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    is EngineState.DownloadFailed -> {
                        Text(
                            text = engine.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Button(
                            onClick = viewModel::downloadSelected,
                            modifier = Modifier.padding(top = 6.dp),
                        ) { Text(stringResource(R.string.vn_try_again)) }
                    }
                    is EngineState.Ready -> {
                        Text(
                            text = stringResource(
                                R.string.vn_engine_installed,
                                stringResource(
                                    if (engine.spec.languages == listOf("en")) R.string.vn_model_name_fast
                                    else R.string.vn_model_name_all
                                ),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.vn_live_section))
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vn_live_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.vn_live_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = liveEnabled, onCheckedChange = viewModel::setLiveEnabled)
                }
                if (liveEnabled) {
                    // Chunked (any language) rides on the installed offline model; the streaming
                    // download is only the optional word-by-word English upgrade.
                    val chunkedReady = engineState is EngineState.Ready
                    when (val live = liveModelState) {
                        is LiveModelState.Downloading -> {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                LinearProgressIndicator(
                                    progress = { live.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = stringResource(R.string.vn_live_downloading, (live.progress * 100).toInt()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        is LiveModelState.Failed -> {
                            if (chunkedReady) {
                                Text(
                                    text = stringResource(R.string.vn_live_ready),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            Text(
                                text = live.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Button(
                                onClick = viewModel::retryLiveDownload,
                                modifier = Modifier.padding(top = 6.dp),
                            ) { Text(stringResource(R.string.vn_try_again)) }
                        }
                        else -> {
                            if (chunkedReady || live is LiveModelState.Ready) {
                                Text(
                                    text = stringResource(R.string.vn_live_ready),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.vn_live_needs_download),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.vn_autotasks_section))
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vn_autotasks_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.vn_autotasks_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = autoTasksEnabled, onCheckedChange = viewModel::setAutoTasksEnabled)
                }
            }

            SectionTitle(stringResource(R.string.vn_privacy_section))
            SettingsCard {
                Text(
                    text = stringResource(R.string.vn_privacy_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.vn_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showPaywall) {
                ProPaywallSheet(
                    isPro = isPro,
                    weeklyPrice = pricing.weeklyPrice,
                    monthlyPrice = pricing.monthlyPrice,
                    lifetimePrice = pricing.lifetimePrice,
                    weeklyTrialDays = pricing.weeklyTrialDays,
                    monthlyTrialDays = pricing.monthlyTrialDays,
                    onBuyWeekly = { activity?.let(viewModel::launchWeekly) },
                    onBuyMonthly = { activity?.let(viewModel::launchMonthly) },
                    onBuyLifetime = { activity?.let(viewModel::launchLifetime) },
                    onRestore = viewModel::restorePurchases,
                    onDismiss = { showPaywall = false },
                )
            }

            SectionTitle(stringResource(R.string.vn_share_section))
            val shareContext = LocalContext.current
            ActionCard(
                title = stringResource(R.string.vn_tell_friend_title),
                subtitle = stringResource(R.string.vn_tell_friend_subtitle),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareContext.getString(R.string.vn_tell_friend_text))
                    }
                    shareContext.startActivity(Intent.createChooser(send, null))
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The in-app review card (MUR-07) is quota-limited by Play and may simply never appear;
            // it produced zero ratings in its first three weeks live. This is the path that always
            // works — the user asks for it, so there is no quota and no silent no-op.
            ActionCard(
                title = stringResource(R.string.vn_rate_title),
                subtitle = stringResource(R.string.vn_rate_subtitle),
                onClick = { shareContext.openPlayStoreListing() },
            )

            Spacer(modifier = Modifier.height(24.dp))
            // Read from the package, not a literal: this line shipped stale in both 2.1.1 and 2.1.2.
            val versionName = remember(shareContext) {
                runCatching {
                    shareContext.packageManager.getPackageInfo(shareContext.packageName, 0).versionName
                }.getOrNull().orEmpty()
            }
            Text(
                text = stringResource(R.string.vn_version_line, versionName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            )
        }
    }
}

/** A tappable title/subtitle card — the shape the "Spread the word" section's rows share. */
@Composable
private fun ActionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Open the Play listing on its review surface. The `market://` scheme goes straight to the installed
 * Play app; devices without it (or with Play disabled) fall back to the browser, and a device with
 * neither simply does nothing rather than throwing [android.content.ActivityNotFoundException].
 */
private fun android.content.Context.openPlayStoreListing() {
    val market = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { startActivity(market) }.isSuccess) return
    val web = Intent(
        Intent.ACTION_VIEW,
        android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(web) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(1.dp, VnTheme.extended.cardStroke),
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
