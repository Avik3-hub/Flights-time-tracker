package com.example.flightlog

import android.content.Context
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
import com.example.flightlog.data.db.*
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.SettingsScreen
import com.example.flightlog.ui.theme.FlightLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember { 
                mutableStateOf(prefs.getBoolean("is_dark_theme", false)) 
            }
            
            var currentScreen by remember { mutableStateOf("main") }

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
                                onToggleTheme = {
                                    val newThemeState = !isDarkTheme
                                    isDarkTheme = newThemeState
                                    prefs.edit().putBoolean("is_dark_theme", newThemeState).apply()
                                },
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
                                        if (duty.dutyDays <= 0) {
                                            db.dutyDao().deleteDutyByPeriod(duty.year, duty.month)
                                        } else {
                                            db.dutyDao().saveDuty(duty)
                                        }
                                    }
                                },
                                onSaveTariff = { tariff ->
                                    lifecycleScope.launch {
                                        db.tariffDao().insertTariff(tariff)
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
                                effectiveFromMonth = 7,
                                landHourlyRate = 1180.0,
                                seaHourlyRate = 5964.0,
                                dutyDayRate = 1952.0
                            )
                            SettingsScreen(
                                currentTariff = currentTariff,
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
