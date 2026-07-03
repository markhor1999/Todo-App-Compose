package com.codingwithsalman.voicenotes.feature.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Text
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
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            SectionTitle("Murmur Pro")
            SettingsCard {
                if (isPro) {
                    Text(
                        text = "✓ Pro is active — unlimited transcription",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Unlimited transcription. Still 100% private.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Free includes 10 minutes a day. Pro removes the meter — monthly, or pay once for lifetime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = { showPaywall = true }) { Text("See Murmur Pro") }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = viewModel::restorePurchases),
                        )
                    }
                }
            }

            SectionTitle("Appearance")
            SettingsCard {
                ThemeMode.entries.forEach { mode ->
                    OptionRow(
                        title = when (mode) {
                            ThemeMode.SYSTEM -> "Match system"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                        },
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }

            SectionTitle("Transcription engine")
            SettingsCard {
                viewModel.models.forEach { spec ->
                    OptionRow(
                        title = spec.displayName,
                        subtitle = "${spec.approxSizeMb} MB · " +
                            if (spec.languages == listOf("en")) "English only, fastest"
                            else "99 languages incl. اردو / हिन्दी",
                        selected = selectedModel.id == spec.id,
                        onClick = { viewModel.selectModel(spec) },
                    )
                }

                when (val engine = engineState) {
                    is EngineState.NotInstalled -> {
                        Button(
                            onClick = viewModel::downloadSelected,
                            modifier = Modifier.padding(top = 10.dp),
                        ) { Text("Download ${engine.spec.displayName} · ${engine.spec.approxSizeMb} MB") }
                    }
                    is EngineState.Downloading -> {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            LinearProgressIndicator(
                                progress = { engine.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${(engine.progress * 100).toInt()}%",
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
                        ) { Text("Try again") }
                    }
                    is EngineState.Ready -> {
                        Text(
                            text = "✓ ${engine.spec.displayName} is installed and ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }

            SectionTitle("Privacy")
            SettingsCard {
                Text(
                    text = "Everything stays on this phone.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recordings, transcripts, and action items are stored only on this " +
                        "device. Transcription runs on your phone's own processor — audio is " +
                        "never uploaded, and the app works fully offline. The only network " +
                        "use is downloading a speech model, once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Murmur 2.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            )
        }
    }
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
