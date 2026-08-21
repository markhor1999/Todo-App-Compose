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
        val autoTasksHighlightSeen = booleanPreferencesKey("auto_tasks_highlight_seen")
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
     * Automatic action items + deadline reminders (2.3.0) — **ON by default since 2.3.1.**
     *
     * It shipped opt-in, on the argument that this one writes into the user's note and can raise a
     * notification, so it should ask first. That reasoning was written for a store listing that
     * sold "offline voice to text". The 2.3.1 listing sells meeting minutes in the *title and short
     * description*, and a promise the median user never finds is worse than one never made — the
     * feature sat behind a switch in section five of Settings with nothing anywhere pointing at it,
     * which is precisely how live transcription reached zero users across 2.1.0–2.1.2.
     *
     * The specific fear the old default guarded has since been retired: [TaskExtractor] requires an
     * explicit commitment cue and is tuned to under-report, the one false positive found on a real
     * device was fixed in `d8b711e` and re-verified at 4/4 with none, a deadline is only attached
     * behind a preposition or an inherently forward-looking phrase, and stale dates are dropped by
     * the scheduler rather than fired late. Non-English transcripts still yield nothing, so eight of
     * the nine shipped locales see exactly what they saw before.
     *
     * Extracted items are labelled as Murmur's work in the note (`vn_autotasks_found`), so the user
     * can tell them from their own, and the Settings switch still turns the whole thing off.
     */
    val autoTasksEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[Keys.autoTasks] ?: true }

    suspend fun setAutoTasksEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoTasks] = enabled }
    }

    /**
     * Whether the one-time "action items are new" card in the library has been dealt with.
     *
     * Exists because the 2.3.1 default flip is invisible to the people who most need to see it:
     * extraction runs when a transcript is produced, so an existing user's whole library — already
     * transcribed under an older build — stays empty until they record something new. The card is
     * what offers them [ActionItemBackfiller] instead of leaving them to notice by accident.
     */
    val autoTasksHighlightSeen: Flow<Boolean> =
        context.settingsDataStore.data.map { prefs -> prefs[Keys.autoTasksHighlightSeen] ?: false }

    suspend fun setAutoTasksHighlightSeen() {
        context.settingsDataStore.edit { it[Keys.autoTasksHighlightSeen] = true }
    }
}
