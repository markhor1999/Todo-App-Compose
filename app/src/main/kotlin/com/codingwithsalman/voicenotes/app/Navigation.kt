package com.codingwithsalman.voicenotes.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codingwithsalman.voicenotes.core.designsystem.components.LocalNavAnimatedVisibilityScope
import com.codingwithsalman.voicenotes.core.designsystem.components.LocalSharedTransitionScope
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnMotion
import com.codingwithsalman.voicenotes.feature.capture.CaptureScreen
import com.codingwithsalman.voicenotes.feature.library.LibraryScreen
import com.codingwithsalman.voicenotes.feature.note.NoteDetailScreen
import com.codingwithsalman.voicenotes.feature.onboarding.OnboardingScreen
import com.codingwithsalman.voicenotes.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object CaptureRoute

@Serializable
data class NoteRoute(val noteId: Long)

@Serializable
data object SettingsRoute

@Serializable
data object OnboardingRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VoiceNotesNavHost(
    startAtOnboarding: Boolean,
    openNoteId: Long? = null,
    onNoteOpened: () -> Unit = {},
    sharedAudioUris: List<android.net.Uri> = emptyList(),
    onSharedAudioConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    // A reminder notification names a note; navigate once it arrives, on top of Library so Back
    // lands somewhere sensible rather than closing the app.
    LaunchedEffect(openNoteId) {
        val noteId = openNoteId ?: return@LaunchedEffect
        navController.navigate(NoteRoute(noteId)) { launchSingleTop = true }
        onNoteOpened()
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            VoiceNotesNavGraph(
                navController, startAtOnboarding, sharedAudioUris, onSharedAudioConsumed,
            )
        }
    }
}

@Composable
private fun VoiceNotesNavGraph(
    navController: androidx.navigation.NavHostController,
    startAtOnboarding: Boolean,
    sharedAudioUris: List<android.net.Uri>,
    onSharedAudioConsumed: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = if (startAtOnboarding) OnboardingRoute else LibraryRoute,
        enterTransition = { fadeIn(tween(VnMotion.FADE_MS)) },
        exitTransition = { fadeOut(tween(VnMotion.FADE_MS)) },
    ) {
        composable<LibraryRoute> {
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                LibraryScreen(
                    onRecord = { navController.navigate(CaptureRoute) },
                    onOpenNote = { noteId -> navController.navigate(NoteRoute(noteId)) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    sharedAudioUris = sharedAudioUris,
                    onSharedAudioConsumed = onSharedAudioConsumed,
                )
            }
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<OnboardingRoute> {
            OnboardingScreen(
                onDone = {
                    navController.navigate(LibraryRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }

        composable<CaptureRoute>(
            enterTransition = {
                slideInVertically(animationSpec = VnMotion.smooth()) { it / 6 } +
                    fadeIn(tween(VnMotion.FADE_MS))
            },
            exitTransition = {
                slideOutVertically(animationSpec = VnMotion.smooth()) { it / 6 } +
                    fadeOut(tween(VnMotion.FADE_MS))
            },
        ) {
            CaptureScreen(
                onClose = { navController.popBackStack() },
                onSaved = { noteId ->
                    navController.navigate(NoteRoute(noteId)) {
                        popUpTo(LibraryRoute)
                    }
                },
            )
        }

        composable<NoteRoute> {
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                NoteDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
