package com.codingwithsalman.jotjive.core.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val activeTrack: StateFlow<AudioTrack?>
    fun play(filePath: String, onComplete: () -> Unit)
    fun pause()
    fun resume()
    fun stop()
}