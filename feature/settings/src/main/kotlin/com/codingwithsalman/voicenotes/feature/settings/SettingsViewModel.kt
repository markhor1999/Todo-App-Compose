package com.codingwithsalman.voicenotes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.voicenotes.asr.api.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.LiveModelState
import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.codingwithsalman.voicenotes.asr.api.ModelCatalog
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

    /** Streaming model size for the download label. */
    val liveModelSizeMb: Int = ModelCatalog.liveEnStreaming.approxSizeMb

    fun setLiveEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setLiveTranscriptionEnabled(enabled) }
        // Enabling triggers the on-demand streaming-model download (no-op if already installed).
        if (enabled) liveManager.ensureModelDownloaded()
    }

    fun retryLiveDownload() = liveManager.ensureModelDownloaded()
}
