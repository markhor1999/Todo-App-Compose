package com.codingwithsalman.jotjive.core.domain.jot

import com.codingwithsalman.jotjive.core.domain.jive.Mood
import java.time.Instant

data class Jot(
    val mood: Mood,
    val title: String,
    val note: String?,
    val topics: List<String>,
    val addedAt: Instant,
    val id: Int? = null
)
