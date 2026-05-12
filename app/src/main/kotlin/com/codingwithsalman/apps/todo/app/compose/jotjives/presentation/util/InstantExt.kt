package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Instant.toReadableTime(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}