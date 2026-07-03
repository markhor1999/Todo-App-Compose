package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Bridges the app-level SharedTransitionLayout and per-destination
 * AnimatedContentScope into feature screens without threading parameters.
 * When either scope is absent (previews, tests), shared elements no-op.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks this node as one half of a cross-screen shared element. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.vnSharedBounds(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        this@vnSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}
