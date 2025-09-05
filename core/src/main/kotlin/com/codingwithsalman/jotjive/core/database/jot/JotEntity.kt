package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.BabyBlue
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.LightGreen
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.RedOrange
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.RedPink
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.Violet

@Entity
data class JotEntity(
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int,
    @PrimaryKey val id: Int? = null
) {
    companion object {
        val noteColors = listOf(RedOrange, LightGreen, Violet, BabyBlue, RedPink)
    }
}

class InvalidNoteException(message: String) : Exception(message)