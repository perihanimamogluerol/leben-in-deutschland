package com.nextdaymobilestudio.lebenindeutschland.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CivicGold,
    onPrimary = Midnight950,
    primaryContainer = CivicGoldContainer,
    onPrimaryContainer = WarmWhite,
    secondary = AccessibleSuccess,
    onSecondary = Midnight950,
    secondaryContainer = AccessibleSuccessContainer,
    onSecondaryContainer = OnAccessibleSuccessContainer,
    background = Midnight950,
    onBackground = WarmWhite,
    surface = Midnight900,
    onSurface = WarmWhite,
    surfaceVariant = Midnight800,
    onSurfaceVariant = SoftGray,
    outline = OutlineGray,
    outlineVariant = OutlineDark,
    error = AccessibleError,
    onError = Midnight950,
    errorContainer = AccessibleErrorContainer,
    onErrorContainer = OnAccessibleErrorContainer,
)

@Composable
fun LebenInDeutschlandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
