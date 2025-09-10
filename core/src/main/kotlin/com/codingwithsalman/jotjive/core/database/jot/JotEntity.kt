package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codingwithsalman.jotjive.core.domain.jive.Mood

@Entity
internal data class JotEntity(
    val title: String,
    val mood: Mood,
    val addedAt: Long,
    val note: String?,
    @PrimaryKey val jotId: Int? = null
)

class InvalidNoteException(message: String) : Exception(message)