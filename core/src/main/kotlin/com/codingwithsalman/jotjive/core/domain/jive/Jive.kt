package com.codingwithsalman.jotjive.core.domain.jive

import java.time.Instant
import kotlin.time.Duration

data class Jive(
    val mood: Mood,
    val title: String,
    val note: String?,
    val topics: List<String>,
    val audioFilePath: String,
    val audioPlaybackLength: Duration,
    val audioAmplitudes: List<Float>,
    val recordedAt: Instant,
    val id: Int? = null
)