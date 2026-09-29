package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SapphirePrimaryDark,
    onPrimary = OnSapphirePrimaryDark,
    primaryContainer = SapphirePrimaryContainerDark,
    onPrimaryContainer = OnSapphirePrimaryContainerDark,
    secondary = CobaltSecondaryDark,
    onSecondary = OnCobaltSecondaryDark,
    secondaryContainer = CobaltSecondaryContainerDark,
    onSecondaryContainer = OnCobaltSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = OnAmberTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = OnAmberTertiaryContainerDark,
    background = StoreBackgroundDark,
    onBackground = StoreOnBackgroundDark,
    surface = StoreSurfaceDark,
    onSurface = StoreOnSurfaceDark,
    surfaceVariant = StoreSurfaceVariantDark,
    onSurfaceVariant = StoreOnSurfaceVariantDark,
    outline = StoreOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SapphirePrimary,
    onPrimary = OnSapphirePrimary,
    primaryContainer = SapphirePrimaryContainer,
    onPrimaryContainer = OnSapphirePrimaryContainer,
    secondary = CobaltSecondary,
    onSecondary = OnCobaltSecondary,
    secondaryContainer = CobaltSecondaryContainer,
    onSecondaryContainer = OnCobaltSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmberTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = OnAmberTertiaryContainer,
    background = StoreBackgroundLight,
    onBackground = StoreOnBackgroundLight,
    surface = StoreSurfaceLight,
    onSurface = StoreOnSurfaceLight,
    surfaceVariant = StoreSurfaceVariantLight,
    onSurfaceVariant = StoreOnSurfaceVariantLight,
    outline = StoreOutlineLight
)

val StoreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StoreShapes,
        content = content
    )
}
