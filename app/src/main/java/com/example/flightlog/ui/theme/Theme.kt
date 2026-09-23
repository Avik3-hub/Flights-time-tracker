package com.example.flightlog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import com.example.flightlog.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val CockpitFont = FontFamily(Font(R.font.oswald))
val CabinFont = FontFamily(Font(R.font.roboto_condensed))
val LocalCockpitTheme = staticCompositionLocalOf { AppTheme.CLASSIC }

private val ClassicColors = darkColorScheme(
    primary = Color(0xFFEFB94F), onPrimary = Color(0xFF241A0B),
    primaryContainer = Color(0xFF171D1F), onPrimaryContainer = Color(0xFFE4DED2),
    secondary = Color(0xFFD0BA94), onSecondary = Color(0xFF221C13),
    secondaryContainer = Color(0xFF34312A), onSecondaryContainer = Color(0xFFE2D7C4),
    tertiary = Color(0xFFB5C3B9), onTertiary = Color(0xFF18241C),
    tertiaryContainer = Color(0xFF28342C), onTertiaryContainer = Color(0xFFD3DED6),
    background = Color(0xFF111617), onBackground = Color(0xFFE4DED2),
    surface = Color(0xFF171D1F), onSurface = Color(0xFFE4DED2),
    surfaceVariant = Color(0xFF25292B), onSurfaceVariant = Color(0xFFBDB9AF),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF797C78), outlineVariant = Color(0xFF3D4242),
    error = Color(0xFFE4A49A), onError = Color(0xFF381612),
    errorContainer = Color(0xFF4B2622), onErrorContainer = Color(0xFFEEC6BE),
    inverseSurface = Color(0xFFE4DED2), inverseOnSurface = Color(0xFF24282A),
    inversePrimary = Color(0xFF74521B), scrim = Color.Black
)

private val BlueColors = lightColorScheme(
    primary = Color(0xFF0869B6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4F0FC), onPrimaryContainer = Color(0xFF203A50),
    secondary = Color(0xFF486D8A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EDF3), onSecondaryContainer = Color(0xFF29485E),
    tertiary = Color(0xFF427779), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1EFEF), onTertiaryContainer = Color(0xFF234F51),
    background = Color(0xFFF2F7FC), onBackground = Color(0xFF112F53),
    surface = Color(0xFFFCFDFE), onSurface = Color(0xFF112F53),
    surfaceVariant = Color(0xFFEAF0F5), onSurfaceVariant = Color(0xFF526574),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF728591), outlineVariant = Color(0xFFCED9E1),
    error = Color(0xFFAB3E34), onError = Color.White,
    errorContainer = Color(0xFFFBE9E5), onErrorContainer = Color(0xFF712A23),
    inverseSurface = Color(0xFF112F53), inverseOnSurface = Color(0xFFF2F7FC),
    inversePrimary = Color(0xFFA4C6E2), scrim = Color.Black
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFFDCA74F), onPrimary = Color(0xFF19140B),
    primaryContainer = Color(0xFF0B0D0F), onPrimaryContainer = Color(0xFFBDBAB5),
    secondary = Color(0xFFB7A88E), onSecondary = Color(0xFF16130E),
    secondaryContainer = Color(0xFF171613), onSecondaryContainer = Color(0xFFC5BCAD),
    tertiary = Color(0xFFA3B1A9), onTertiary = Color(0xFF111713),
    tertiaryContainer = Color(0xFF101A14), onTertiaryContainer = Color(0xFFBDC9C0),
    background = Color.Black, onBackground = Color(0xFFBDBAB5),
    surface = Color(0xFF0B0D0F), onSurface = Color(0xFFBDBAB5),
    surfaceVariant = Color(0xFF101214), onSurfaceVariant = Color(0xFFA4A19A),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF73736C), outlineVariant = Color(0xFF2B2E30),
    error = Color(0xFFCB958C), onError = Color(0xFF28120E),
    errorContainer = Color(0xFF241310), onErrorContainer = Color(0xFFD4AAA2),
    inverseSurface = Color(0xFF222426), inverseOnSurface = Color(0xFFBDBAB5),
    inversePrimary = Color(0xFFDCA74F), scrim = Color.Black
)

private val BaseTypography = Typography()
private val CockpitTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = CockpitFont),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = CockpitFont),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = CockpitFont),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = CockpitFont, fontWeight = FontWeight.Normal),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = CockpitFont, fontWeight = FontWeight.Normal),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = CabinFont, fontWeight = FontWeight.Normal),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = CabinFont, fontWeight = FontWeight.Medium),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = CabinFont, fontWeight = FontWeight.Normal),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = CabinFont, fontWeight = FontWeight.Normal),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = CabinFont),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = CabinFont),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = CabinFont),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = CabinFont, fontWeight = FontWeight.Normal),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = CabinFont, fontWeight = FontWeight.Normal),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = CabinFont)
)

@Composable
fun FlightLogTheme(theme: AppTheme = AppTheme.CLASSIC, content: @Composable () -> Unit) {
    val resolved = theme.resolve(isSystemInDarkTheme())
    val colors = when (resolved) {
        AppTheme.BLUE -> BlueColors
        AppTheme.AMOLED -> AmoledColors
        else -> ClassicColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.background.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = resolved == AppTheme.BLUE
                    isAppearanceLightNavigationBars = resolved == AppTheme.BLUE
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = CockpitTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(10.dp),
            extraLarge = RoundedCornerShape(12.dp)
        ),
        content = { CompositionLocalProvider(LocalCockpitTheme provides resolved, content = content) }
    )
}
