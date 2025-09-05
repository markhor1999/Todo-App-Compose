package com.codingwithsalman.jotjive.core.domain.jot

import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.BabyBlue
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.LightGreen
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.RedOrange
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.RedPink
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.Violet

data class Jot(
    val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long,
    val color: Int
) {
    companion object {
        val noteColors = listOf(RedOrange, LightGreen, Violet, BabyBlue, RedPink)
    }
}
