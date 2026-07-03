package com.codingwithsalman.voicenotes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.voicenotes.asr.api.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.EngineState
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
}
