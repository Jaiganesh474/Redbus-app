package com.redbus.ai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.redbus.ai.R

// Primary Font: Official redBus typography system (Open Sans) matching redBus Mobile App (Image 5)
val RedbusFontFamily = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_semibold, FontWeight.Medium),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_bold, FontWeight.ExtraBold),
    Font(R.font.opensans_bold, FontWeight.Black)
)

// Aliases point to RedbusFontFamily for complete 100% app-wide font coverage
val LenskartSansFontFamily = RedbusFontFamily
val PoppinsFontFamily = RedbusFontFamily
val MetropolisFontFamily = RedbusFontFamily
val OpenSansFontFamily = RedbusFontFamily

val RedbusTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RedbusFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
