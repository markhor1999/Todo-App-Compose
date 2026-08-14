@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tricodestudio.voicekit.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.LiveModelState
import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.tricodestudio.voicekit.ModelCatalog
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.billing.BillingRepository
import com.codingwithsalman.voicenotes.core.billing.ProPricing
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator,
    private val liveManager: LiveTranscriptionManager,
    private val billing: BillingRepository,
    entitlementStore: EntitlementStore,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = entitlementStore.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pricing: StateFlow<ProPricing> = billing.pricing

    fun launchWeekly(activity: android.app.Activity) = billing.launchWeekly(activity)

    fun launchMonthly(activity: android.app.Activity) = billing.launchMonthly(activity)

    fun launchLifetime(activity: android.app.Activity) = billing.launchLifetime(activity)

    fun restorePurchases() = billing.restorePurchases()

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val selectedModel: StateFlow<AsrModelSpec> = settings.modelId
        .map(ModelCatalog::byId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelCatalog.default)

    val engineState: StateFlow<EngineState> = transcriptionCoordinator.engineState

    val models: List<AsrModelSpec> = ModelCatalog.all

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun selectModel(spec: AsrModelSpec) {
        viewModelScope.launch { settings.setModelId(spec.id) }
    }

    fun downloadSelected() = transcriptionCoordinator.ensureModel()

    // --- Live transcription (v2.1 #2) — off by default, English-only, experimental ---

    val liveEnabled: StateFlow<Boolean> = settings.liveTranscriptionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val liveModelState: StateFlow<LiveModelState> = liveManager.modelState

    fun setLiveEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setLiveTranscriptionEnabled(enabled) }
        // The word-level streaming download is an ENGLISH latency upgrade — only fetch it when an
        // English-only model is selected. Multilingual users are instantly ready via the chunked
        // path on their installed offline model (no extra download).
        if (enabled && selectedModel.value.languages == listOf("en")) liveManager.ensureModelDownloaded()
    }

    fun retryLiveDownload() = liveManager.ensureModelDownloaded()

    // --- Action items & deadline reminders (2.3.0) — opt-in, English-only extraction ---

    val autoTasksEnabled: StateFlow<Boolean> = settings.autoTasksEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setAutoTasksEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoTasksEnabled(enabled) }
    }
}
