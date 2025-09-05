package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
internal data class JotEntity(
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    @PrimaryKey val id: Int? = null
)

class InvalidNoteException(message: String) : Exception(message)