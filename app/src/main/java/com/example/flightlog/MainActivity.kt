package com.example.flightlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

FlightLogTheme(darkTheme = isDarkTheme) {
    // Вызов MainScreen с передачей isDarkTheme и onToggleTheme = { isDarkTheme = !isDarkTheme }
}

            MaterialTheme {
                var currentScreen by remember { mutableStateOf("main") }

                val flights by db.flightDao().getAllFlights().collectAsState(initial = emptyList())
                val dutyRecords by db.dutyDao().getAllDuties().collectAsState(initial = emptyList())
                val rawTariff by db.tariffDao().getTariff().collectAsState(initial = TariffEntity())
                val currentTariff = rawTariff ?: TariffEntity()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        "main" -> {
                            MainScreen(
                                flights = flights,
                                dutyRecords = dutyRecords,
                                tariff = currentTariff,
                                onAddFlight = { flight ->
                                    lifecycleScope.launch {
                                        db.flightDao().insertFlight(flight)
                                    }
                                },
                                onUpdateFlight = { flight -> // <--- ДОБАВЬТЕ ЭТУ СТРОКУ
        lifecycleScope.launch {
            db.flightDao().updateFlight(flight)
        }
    },
                                onDeleteFlight = { flight ->
                                    lifecycleScope.launch {
                                        db.flightDao().deleteFlight(flight)
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
                            SettingsScreen(
                                currentTariff = currentTariff,
                                onSaveTariff = { updatedTariff ->
                                    lifecycleScope.launch {
                                        db.tariffDao().saveTariff(updatedTariff)
                                        currentScreen = "main"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
