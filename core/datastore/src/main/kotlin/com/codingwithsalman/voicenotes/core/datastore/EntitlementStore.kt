package com.codingwithsalman.voicenotes.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.entitlementDataStore: DataStore<Preferences> by preferencesDataStore(name = "entitlement")

/** Pro entitlement + the free tier's daily transcription meter. */
@Singleton
class EntitlementStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val isPro = booleanPreferencesKey("is_pro")
        val meterDate = stringPreferencesKey("meter_date")
        val meterUsedMs = longPreferencesKey("meter_used_ms")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val transcriptionSuccessCount = longPreferencesKey("transcription_success_count")
        val reviewRequested = booleanPreferencesKey("review_requested")
    }

    val isPro: Flow<Boolean> = context.entitlementDataStore.data.map { it[Keys.isPro] ?: false }

    suspend fun setPro(pro: Boolean) {
        context.entitlementDataStore.edit { it[Keys.isPro] = pro }
    }

    val onboardingDone: Flow<Boolean> =
        context.entitlementDataStore.data.map { it[Keys.onboardingDone] ?: false }

    suspend fun setOnboardingDone() {
        context.entitlementDataStore.edit { it[Keys.onboardingDone] = true }
    }

    /** Milliseconds of free transcription left today; [FREE_DAILY_MS] when Pro (untracked). */
    val remainingTodayMs: Flow<Long> = context.entitlementDataStore.data.map { prefs ->
        if (prefs[Keys.isPro] == true) return@map FREE_DAILY_MS
        val usedToday = if (prefs[Keys.meterDate] == today()) prefs[Keys.meterUsedMs] ?: 0L else 0L
        (FREE_DAILY_MS - usedToday).coerceAtLeast(0L)
    }

    /**
     * Free-tier policy: a job may START whenever any allowance remains (so one long
     * note isn't unstartable); its full duration then counts against the meter.
     */
    suspend fun canStartTranscription(): Boolean =
        isPro.first() || remainingTodayMs.first() > 0L

    suspend fun consume(durationMs: Long) {
        if (isPro.first()) return
        context.entitlementDataStore.edit { prefs ->
            val date = today()
            val used = if (prefs[Keys.meterDate] == date) prefs[Keys.meterUsedMs] ?: 0L else 0L
            prefs[Keys.meterDate] = date
            prefs[Keys.meterUsedMs] = used + durationMs.coerceAtLeast(0L)
        }
    }

    /**
     * Bump the lifetime count of successful transcriptions. Its only use is to gate the one-time
     * in-app review request ([reviewDue]).
     */
    suspend fun recordTranscriptionSuccess() {
        context.entitlementDataStore.edit { prefs ->
            prefs[Keys.transcriptionSuccessCount] = (prefs[Keys.transcriptionSuccessCount] ?: 0L) + 1L
        }
    }

    /**
     * True once the user has completed [REVIEW_AFTER_SUCCESSES] transcriptions and we have not yet
     * asked for a Play review. Play itself further decides whether the review card actually shows.
     */
    val reviewDue: Flow<Boolean> = context.entitlementDataStore.data.map { prefs ->
        val count = prefs[Keys.transcriptionSuccessCount] ?: 0L
        val alreadyAsked = prefs[Keys.reviewRequested] ?: false
        !alreadyAsked && count >= REVIEW_AFTER_SUCCESSES
    }

    /** Remember we've asked, so the review request fires at most once per install. */
    suspend fun markReviewRequested() {
        context.entitlementDataStore.edit { it[Keys.reviewRequested] = true }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        /** Free tier: 10 minutes of transcription per day. */
        const val FREE_DAILY_MS = 10 * 60 * 1000L

        /**
         * Ask for a Play in-app review after this many successful transcriptions (once per install).
         *
         * **1 since 2.2.0, was 2.** MUR-07 shipped in 2.1.1 and produced zero ratings in three
         * weeks; Play still generates no ratings file at all for the package. With ~23 active
         * devices, requiring a *second* completed transcription shrank an already tiny eligible pool
         * to roughly nobody. One finished transcript is the app's whole value proposition delivered,
         * which makes it a fair moment to ask.
         */
        const val REVIEW_AFTER_SUCCESSES = 1L
    }
}
