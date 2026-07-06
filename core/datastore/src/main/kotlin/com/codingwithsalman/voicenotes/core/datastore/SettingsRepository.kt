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

    /** Live transcription (v2.1 #2) — OFF by default; English-only, experimental, needs its own
     *  streaming-model download. Gated here so the capture path stays on the proven MediaRecorder
     *  route for everyone who hasn't opted in. */
    val liveTranscriptionEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[Keys.liveTranscription] ?: false }

    suspend fun setLiveTranscriptionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.liveTranscription] = enabled }
    }
}
