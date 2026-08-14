package com.codingwithsalman.voicenotes.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codingwithsalman.voicenotes.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val modelId = stringPreferencesKey("asr_model_id")
        val liveTranscription = booleanPreferencesKey("live_transcription_enabled")
        val autoTasks = booleanPreferencesKey("auto_tasks_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.themeMode]?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    /** Selected ASR model id; null until the user picks (callers fall back to the catalog default). */
    val modelId: Flow<String?> = context.settingsDataStore.data.map { prefs -> prefs[Keys.modelId] }

    suspend fun setModelId(id: String) {
        context.settingsDataStore.edit { it[Keys.modelId] = id }
    }

    /**
     * Live transcription (v2.1 #2) — **ON by default since 2.2.0.** The original OFF default dated
     * from #2's English-only draft, which needed its own streaming-model download; #2b replaced that
     * with VAD-chunked decoding on the offline model the user already has, so the feature is
     * multilingual and needs no extra download. It shipped hidden through 2.1.0–2.1.2 and was never
     * exposed, i.e. never seen by a single user.
     *
     * Turning it on switches the capture path from MediaRecorder to [PcmAudioRecorder] for everyone
     * whose offline model is installed. That is the real risk in this flip, so
     * `RecordingSessionManager.startRecording` now falls back to the MediaRecorder route whenever the
     * PCM path fails to start — a device whose AAC encoder rejects 16 kHz mono loses the live
     * preview, never the recording. Users who explicitly turned the toggle off keep it off (an
     * absent key is what defaults, and only the toggle writes one).
     */
    val liveTranscriptionEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[Keys.liveTranscription] ?: true }

    suspend fun setLiveTranscriptionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.liveTranscription] = enabled }
    }

    /**
     * Automatic action items + deadline reminders (2.3.0) — **OFF by default, opt-in.**
     *
     * Unlike live transcription, this one writes into the user's note and can raise a notification,
     * so it asks first. Extraction is heuristic and English-only; silently adding checkboxes nobody
     * asked for — or worse, buzzing a phone at 9am about a misheard date — is exactly the failure
     * this default avoids. The user turns it on knowing what it does.
     */
    val autoTasksEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[Keys.autoTasks] ?: false }

    suspend fun setAutoTasksEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoTasks] = enabled }
    }
}
