package com.codingwithsalman.voicenotes.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.codingwithsalman.voicenotes.core.billing.BillingRepository
import com.codingwithsalman.voicenotes.core.reminders.ReminderWorker
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.codingwithsalman.voicenotes.core.designsystem.theme.VoiceNotesTheme
import com.codingwithsalman.voicenotes.core.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var entitlementStore: EntitlementStore

    @Inject
    lateinit var billing: BillingRepository

    /** Audio Uris handed to us by a share/view intent, awaiting import. Observed by Compose. */
    private val sharedAudioUris = mutableStateOf<List<Uri>>(emptyList())

    /** Note to open straight away, set when a reminder notification launched us. */
    private val openNoteId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billing.connect()
        observeInAppReview()
        // Only process the launch intent on a fresh start — on a config-change recreation
        // savedInstanceState is non-null, and re-reading getIntent() would double-import.
        if (savedInstanceState == null) {
            sharedAudioUris.value = extractAudioUris(intent)
            openNoteId.value = extractNoteId(intent)
        }
        val launchedFromShare = sharedAudioUris.value.isNotEmpty()
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            val onboardingDone by remember {
                entitlementStore.onboardingDone.map { done -> done as Boolean? }
            }.collectAsStateWithLifecycle(initialValue = null)
            LaunchedEffect(darkTheme) {
                // Re-style system bars when the in-app theme diverges from the system's.
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                )
            }
            VoiceNotesTheme(darkTheme = darkTheme) {
                // Hold rendering one frame until the flag loads so we start at the right screen.
                onboardingDone?.let { done ->
                    VoiceNotesNavHost(
                        // A share/view launch skips onboarding so the imported note lands right away.
                        startAtOnboarding = !done && !launchedFromShare,
                        openNoteId = openNoteId.value,
                        onNoteOpened = { openNoteId.value = null },
                        sharedAudioUris = sharedAudioUris.value,
                        onSharedAudioConsumed = { sharedAudioUris.value = emptyList() },
                    )
                }
            }
        }
    }

    /**
     * After [EntitlementStore.REVIEW_AFTER_SUCCESSES] successful transcriptions, ask for a Play
     * in-app review — once per install, at a natural post-success moment (the flag flips right after
     * a transcription finishes). Play rate-limits whether the card actually shows and never reports
     * the outcome, so this is fire-and-forget with no analytics (Murmur's MUR-03 no-analytics stance).
     * The "asked" flag is set only on a successful request, so an offline / no-Play-Store device
     * simply retries the next time it's in the foreground.
     */
    private fun observeInAppReview() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                entitlementStore.reviewDue.distinctUntilChanged().collect { due ->
                    if (due && runCatching { launchInAppReview() }.isSuccess) {
                        entitlementStore.markReviewRequested()
                    }
                }
            }
        }
    }

    /** A share/view arriving while we're already running (app in the back stack). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uris = extractAudioUris(intent)
        if (uris.isNotEmpty()) sharedAudioUris.value = uris
        extractNoteId(intent)?.let { openNoteId.value = it }
    }

    /** The note a reminder notification is pointing at, or null for an ordinary launch. */
    private fun extractNoteId(intent: Intent?): Long? =
        intent?.getLongExtra(ReminderWorker.EXTRA_NOTE_ID, -1L)?.takeIf { it > 0 }

    /** Pull audio Uri(s) out of a SEND / SEND_MULTIPLE / VIEW intent. */
    private fun extractAudioUris(intent: Intent?): List<Uri> = when (intent?.action) {
        Intent.ACTION_SEND ->
            listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        else -> emptyList()
    }
}
