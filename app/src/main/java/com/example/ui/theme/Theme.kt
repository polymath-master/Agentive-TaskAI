package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkColor,
    onPrimary = DeepNavyDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = BlueContainer,
    onSecondary = NavyPrimary,
    secondaryContainer = BorderDark,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = IndigoContainer,
    onTertiary = IndigoOnContainer,
    background = DeepNavyDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = BlueOnContainer,
    tertiary = BlueContainer,
    onTertiary = BlueOnContainer,
    background = SoftBackLight,
    onBackground = CharcoalOnBackLight,
    surface = SoftBackLight,
    onSurface = CharcoalOnBackLight,
    surfaceVariant = NavBarBackLight,
    onSurfaceVariant = CharcoalOnBackLight,
    outline = CardBorderColor,
    outlineVariant = NavBarBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false by default to enforce curated "Bold Typography" design theme visual scheme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

