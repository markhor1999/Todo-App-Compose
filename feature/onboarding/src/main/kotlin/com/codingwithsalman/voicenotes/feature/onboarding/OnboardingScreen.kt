package com.codingwithsalman.voicenotes.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.codingwithsalman.voicenotes.core.designsystem.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.core.designsystem.components.DecorativeWaveform
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { scope.launch { pagerState.animateScrollToPage(2) } }

    fun finish() = viewModel.complete(onDone)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> {
                            DecorativeWaveform(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(56.dp),
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Text(
                                text = stringResource(R.string.vn_onb1_title),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.vn_onb1_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        1 -> {
                            Text(
                                text = "🎙️",
                                style = MaterialTheme.typography.displayMedium,
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Text(
                                text = stringResource(R.string.vn_onb2_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.vn_onb2_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) scope.launch { pagerState.animateScrollToPage(2) }
                                else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }) { Text(stringResource(R.string.vn_allow_mic)) }
                        }

                        2 -> {
                            Text(
                                text = "🔒",
                                style = MaterialTheme.typography.displayMedium,
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Text(
                                text = stringResource(R.string.vn_onb3_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.vn_onb3_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            when (val engine = engineState) {
                                is EngineState.NotInstalled -> Button(onClick = viewModel::downloadModel) {
                                    Text(stringResource(R.string.vn_download_size, engine.spec.approxSizeMb))
                                }
                                is EngineState.Downloading -> Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    LinearProgressIndicator(
                                        progress = { engine.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.vn_onb_dl_progress, (engine.progress * 100).toInt()),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                is EngineState.DownloadFailed -> Button(onClick = viewModel::downloadModel) {
                                    Text(stringResource(R.string.vn_try_again))
                                }
                                is EngineState.Ready -> Text(
                                    text = stringResource(R.string.vn_onb_ready),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                when {
                    pagerState.currentPage < 2 -> {
                        TextButton(onClick = { finish() }) {
                            Text(stringResource(R.string.vn_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) { Text(stringResource(R.string.vn_next)) }
                    }
                    else -> Button(onClick = { finish() }) {
                        Text(if (engineState is EngineState.Ready) stringResource(R.string.vn_start) else stringResource(R.string.vn_start_anyway))
                    }
                }
            }
        }
    }
}
