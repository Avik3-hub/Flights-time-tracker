package com.example.flightlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.SettingsScreen
import com.example.flightlog.ui.theme.FlightLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация синглтона базы данных
        val db = AppDatabase.getDatabase(this)

        setContent {
            // Управление переключением темы
            var isDarkTheme by remember { mutableStateOf(false) }

            // Переключение между экранами
            var currentScreen by remember { mutableStateOf("main") }

            // Подписка на Flow из базы данных
            val flights by db.flightDao().getAllFlights().collectAsState(initial = emptyList())
            val dutyRecords by db.dutyDao().getAllDuties().collectAsState(initial = emptyList())
            val tariffs by db.tariffDao().getAllTariffs().collectAsState(initial = emptyList())

            FlightLogTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (currentScreen) {
                        "main" -> {
                            MainScreen(
                                flights = flights,
                                dutyRecords = dutyRecords,
                                tariffs = tariffs,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                onAddFlight = { flight ->
                                    lifecycleScope.launch {
                                        db.flightDao().insertFlight(flight)
                                    }
                                },
                                onUpdateFlight = { flight ->
                                    lifecycleScope.launch {
                                        db.flightDao().updateFlight(flight)
                                    }
                                },
                                onDeleteFlight = { flightId ->
                                    lifecycleScope.launch {
                                        db.flightDao().deleteFlightById(flightId)
                                    }
                                },
                                onSaveDuty = { duty ->
                                    lifecycleScope.launch {
                                        db.dutyDao().saveDuty(duty)
                                    }
                                },
                                onSettingsClick = {
                                    currentScreen = "settings"
                                }
                            )
                        }
                        "settings" -> {
    val currentTariff = tariffs.firstOrNull() ?: TariffEntity(
        effectiveFromYear = 2026,
        effectiveFromMonth = 8,
        landHourlyRate = 1500.0,
        seaHourlyRate = 0.0,
        dutyDayRate = 0.0
    )

    SettingsScreen(
        currentTariff = currentTariff,
        flights = flights,
        dutyRecord = dutyRecords.firstOrNull(),
        onSaveTariff = { updatedTariff ->
            lifecycleScope.launch {
                db.tariffDao().insertTariff(updatedTariff)
                currentScreen = "main"
            }
        },
        onBackClick = {
            currentScreen = "main"
        }
    )
}
                    }
                }
            }
        }
    }
}
