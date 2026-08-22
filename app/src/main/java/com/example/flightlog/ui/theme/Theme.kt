package com.example.flightlog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Чистый AMOLED Black для темной темы
private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF121212),       // Темный контейнер для шапки (TopAppBar)
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E1E1E),     // Карточки
    onSecondaryContainer = Color.White,
    background = Color.Black,                    // Абсолютно черный фон
    surface = Color.Black,                       // Абсолютно черный для поверхностей
    surfaceVariant = Color(0xFF121212),         // Поля ввода и карточки
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
    surfaceVariant = Color(0xFFE1E2EC),
    onBackground = Color(0xFF191C1E),
    onSurface = Color(0xFF191C1E),
    onSurfaceVariant = Color(0xFF44474E)
)

@Composable
fun FlightLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmoledDarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Фиксируем черный цвет статус-бара в темной теме
            window.statusBarColor = if (darkTheme) Color.Black.toArgb() else colorScheme.primaryContainer.toArgb()
            
            // Настройка цвета иконки батареи/часов (белые в темной теме, темные в светлой)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
