package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightMedicalColorScheme = lightColorScheme(
    primary = MedicalNavy,
    onPrimary = PureWhite,
    primaryContainer = SoftIceBlue,
    onPrimaryContainer = MedicalNavy,
    secondary = ClinicalTeal,
    onSecondary = PureWhite,
    secondaryContainer = SoftTealTint,
    onSecondaryContainer = ClinicalTeal,
    tertiary = ClinicalSky,
    onTertiary = PureWhite,
    background = ClinicalPaper,
    onBackground = DeepSlateText,
    surface = PureWhite,
    onSurface = DeepSlateText,
    surfaceVariant = SoftIceBlue,
    onSurfaceVariant = MutedSlateText,
    outline = SlateOutline,
    error = UrgentCrimson,
    onError = PureWhite,
    errorContainer = UrgentCrimsonBg,
    onErrorContainer = UrgentCrimson
)

private val DarkMedicalColorScheme = darkColorScheme(
    primary = ColorTokens.DarkPrimary,
    onPrimary = IcuMidnight,
    primaryContainer = ColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = LightInk,
    secondary = LuminousTeal,
    onSecondary = IcuMidnight,
    secondaryContainer = ColorTokens.DarkSecondaryContainer,
    onSecondaryContainer = LuminousTeal,
    tertiary = ClinicalSky,
    onTertiary = IcuMidnight,
    background = IcuMidnight,
    onBackground = LightInk,
    surface = IcuSurface,
    onSurface = LightInk,
    surfaceVariant = IcuElevated,
    onSurfaceVariant = MutedLightInk,
    outline = IcuBorder,
    error = ColorTokens.DarkError,
    onError = IcuMidnight,
    errorContainer = ColorTokens.DarkErrorContainer,
    onErrorContainer = ColorTokens.DarkError
)

private object ColorTokens {
    val DarkPrimary = androidx.compose.ui.graphics.Color(0xFF60A5FA)
    val DarkPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F)
    val DarkSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF134E4A)
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFCA5A5)
    val DarkErrorContainer = androidx.compose.ui.graphics.Color(0xFF7F1D1D)
}

val JournalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun AlAnharJournalTheme(
    themeMode: String = "system",
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkMedicalColorScheme else LightMedicalColorScheme
    val typography = buildJournalTypography(isArabic = isArabic)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = JournalShapes,
        content = content
    )
}
