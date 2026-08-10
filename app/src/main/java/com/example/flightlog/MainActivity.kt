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
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Modifier
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.SettingsScreen
import com.example.flightlog.ui.theme.FlightLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            FlightLogTheme {
                var currentScreen by remember { mutableStateOf("main") }

                val currentTariff by db.tariffDao().getTariff().collectAsState(initial = TariffEntity())
                val flights by db.flightDao().getAllFlights().collectAsState(initial = emptyList())
                val dutyRecords by db.dutyDao().getAllDuties().collectAsState(initial = emptyList())

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
                                onDeleteFlight = { flight ->
                                    lifecycleScope.launch {
                                        db.flightDao().deleteFlight(flight)
                                    }
                                },
                                onSaveDuty = { duty ->
                                    lifecycleScope.launch {
                                        db.dutyDao().insertOrUpdate(duty)
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
                                        db.tariffDao().insertOrUpdate(updatedTariff)
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
