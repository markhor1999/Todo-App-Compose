package com.codingwithsalman.jotjive.core.database.jive

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codingwithsalman.jotjive.core.domain.jive.Mood

@Entity
internal data class JiveEntity(
    @PrimaryKey(autoGenerate = true)
    val jiveId: Int = 0,
    val title: String,
    val mood: Mood,
    val recordedAt: Long,
    val note: String?,
    val audioFilePath: String,
    val audioPlaybackLength: Long,
    val audioAmplitudes: List<Float>
)