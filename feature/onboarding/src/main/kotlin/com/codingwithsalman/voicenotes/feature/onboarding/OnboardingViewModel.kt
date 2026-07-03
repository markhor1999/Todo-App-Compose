package com.codingwithsalman.voicenotes.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithsalman.voicenotes.asr.api.EngineState
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val entitlementStore: EntitlementStore,
    private val transcriptionCoordinator: TranscriptionCoordinator,
) : ViewModel() {

    val engineState: StateFlow<EngineState> = transcriptionCoordinator.engineState

    fun downloadModel() = transcriptionCoordinator.ensureModel()

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            entitlementStore.setOnboardingDone()
            onDone()
        }
    }
}
