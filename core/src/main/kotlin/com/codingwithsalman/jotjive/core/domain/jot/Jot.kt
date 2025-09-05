package com.codingwithsalman.jotjive.core.domain.jot

data class Jot(
    val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int
)
