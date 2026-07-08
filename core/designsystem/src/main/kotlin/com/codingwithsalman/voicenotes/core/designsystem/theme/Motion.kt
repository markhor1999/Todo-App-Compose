package com.codingwithsalman.voicenotes.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * The app's motion language: springs for anything that moves or transforms,
 * short fades for appearance. No linear tweens on spatial properties.
 */
object VnMotion {
    /** Default for most movement — settled, no visible overshoot. */
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 380f)

    /** Playful — record button morph, badges popping in. */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.65f, stiffness = 300f)

    /** Immediate — pressed states, scrubber tracking. */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 700f)

    const val FADE_MS = 200
}
