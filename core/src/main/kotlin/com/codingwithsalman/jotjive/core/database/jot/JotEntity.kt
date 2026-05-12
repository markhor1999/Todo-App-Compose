package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codingwithsalman.jotjive.core.domain.jive.Mood

@Entity
internal data class JotEntity(
    @PrimaryKey(autoGenerate = true)
    val jotId: Int = 0,
    val title: String,
    val mood: Mood,
    val addedAt: Long,
    val note: String?,
    val isTodo: Boolean = false,
    val isDone: Boolean = false
)

class InvalidNoteException(message: String) : Exception(message)