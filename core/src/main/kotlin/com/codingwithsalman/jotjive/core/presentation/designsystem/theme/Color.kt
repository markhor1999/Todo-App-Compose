package com.codingwithsalman.jotjive.core.presentation.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Stressed25 = Color(0xFFF9EFEF)
val Stressed35 = Color(0xFFE9C5C5)
val Stressed80 = Color(0xFFDE3A3A)
val Stressed95 = Color(0xFF6B0303)

val Sad25 = Color(0xFFEFF4F8)
val Sad35 = Color(0xFFC5D8E9)
val Sad80 = Color(0xFF3A8EDE)
val Sad95 = Color(0xFF004585)

val Neutral25 = Color(0xFFEEF7F3)
val Neutral35 = Color(0xFFB9DDCB)
val Neutral80 = Color(0xFF41B278)
val Neutral95 = Color(0xFF0A5F33)

val Peaceful25 = Color(0xFFF6F2F5)
val Peaceful35 = Color(0xFFE1CEDB)
val Peaceful80 = Color(0xFFBE3294)
val Peaceful95 = Color(0xFF6C044D)

val Excited25 = Color(0xFFF5F2EF)
val Excited35 = Color(0xFFDDD2C8)
val Excited80 = Color(0xFFDB6C0B)
val Excited95 = Color(0xFF723602)

val MoodPrimary25 = Color(0xFFEEF0FF)
val MoodPrimary35 = Color(0xFFBAC6E9)
val MoodPrimary80 = Color(0xFF00419C)

val Primary10 = Color(0xFF001945)
val Primary30 = Color(0xFF004CB4)
val Primary40 = Color(0xFF0057CC)
val Primary50 = Color(0xFF1F70F5)
val Primary60 = Color(0xFF578CFF)
val Primary90 = Color(0xFFD9E2FF)
val Primary95 = Color(0xFFEEF0FF)
val Primary100 = Color(0xFFFFFFFF)

val Secondary30 = Color(0xFF3B4663)
val Secondary50 = Color(0xFF6B7796)
val Secondary70 = Color(0xFF9FABCD)
val Secondary80 = Color(0xFFBAC6E9)
val Secondary90 = Color(0xFFD9E2FF)
val Secondary95 = Color(0xFFEEF0FF)

val NeutralVariant10 = Color(0xFF191A20)
val NeutralVariant30 = Color(0xFF40434F)
val NeutralVariant50 = Color(0xFF6C7085)
val NeutralVariant80 = Color(0xFFC1C3CE)
val NeutralVariant90 = Color(0xFFE0E1E7)
val NeutralVariant99 = Color(0xFFFCFDFE)

val Error20 = Color(0xFF680014)
val Error95 = Color(0xFFFFEDEC)
val Error100 = Color(0xFFFFFFFF)

val Gray = Color(0xFF8E8E93)
val Gray2 = Color(0xFFAEAEB2)
val Gray3 = Color(0xFFC7C7CC)
val Gray4 = Color(0xFFD1D1D6)
val Gray5 = Color(0xFFE5E5EA)
val Gray6 = Color(0xFFF2F2F7)

val RedOrange = Color(0xffffab91)
val RedPink = Color(0xfff48fb1)
val BabyBlue = Color(0xff81deea)
val Violet = Color(0xffcf94da)
val LightGreen = Color(0xffe7ed9b)

val ColorScheme.bgGradient: Brush
    get() = Brush.verticalGradient(
        listOf(
            Secondary90.copy(alpha = 0.4f),
            Secondary95.copy(alpha = 0.4f),
        )
    )

val ColorScheme.buttonGradient: Brush
    get() = Brush.verticalGradient(
        listOf(
            Primary60,
            Primary50,
        )
    )

val ColorScheme.buttonGradientPressed: Brush
    get() = Brush.verticalGradient(
        listOf(
            Primary60,
            Primary40,
        )
    )

val ColorScheme.primary90: Color
    get() = Primary90

val ColorScheme.primary95: Color
    get() = Primary95

val ColorScheme.secondary95: Color
    get() = Secondary95

val ColorScheme.secondary70: Color
    get() = Secondary70