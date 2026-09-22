package com.example.flightlog.ui.theme

enum class AppTheme(val title: String, val description: String) {
    CLASSIC("Классика", "Графит и янтарные показатели"),
    BLUE("Синяя", "Светлые поверхности и спокойный авиационный синий"),
    AMOLED("Тёмная", "AMOLED: чёрный фон и приглушённые акценты"),
    SYSTEM("Как в системе", "Синяя в светлом режиме, тёмная в ночном");

    fun resolve(systemDark: Boolean): AppTheme =
        if (this == SYSTEM) { if (systemDark) AMOLED else BLUE } else this

    companion object {
        /** Preserve the explicit dark preference from 1.1.8. */
        fun fromPreferences(saved: String?, legacyDark: Boolean): AppTheme =
            values().firstOrNull { it.name == saved }
                ?: if (legacyDark) AMOLED else CLASSIC
    }
}
