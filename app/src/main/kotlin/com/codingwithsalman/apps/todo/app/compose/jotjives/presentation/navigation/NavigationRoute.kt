package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    @Serializable
    data class JotJives(
        val startRecording: Boolean
    ) : NavigationRoute

    @Serializable
    data class Jives(
        val startRecording: Boolean
    ) : NavigationRoute

    @Serializable
    data class CreateJive(
        val recordingPath: String,
        val duration: Long,
        val amplitudes: String
    ) : NavigationRoute

    @Serializable
    data object CreateJot : NavigationRoute

    @Serializable
    data object Settings
}