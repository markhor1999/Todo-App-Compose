package com.codingwithsalman.jotjive.core.presentation.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codingwithsalman.jotjive.core.R

val Inter = FontFamily(
    Font(
        resId = R.font.inter_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.inter_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.inter_medium,
        weight = FontWeight.Medium
    ),
)

val Typography = Typography(
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = NeutralVariant10
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = NeutralVariant10
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = NeutralVariant10
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = NeutralVariant10,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = NeutralVariant10
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        color = NeutralVariant10
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        color = NeutralVariant10
    ),
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    )
)