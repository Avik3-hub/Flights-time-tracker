package com.example.flightlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.theme.FlightLogTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            FlightLogTheme {
                // Подписываемся на данные из базы в реальном времени
                val flights by db.flightDao().getAllFlights().collectAsState(initial = emptyList())
                val tariff by db.tariffDao().getTariffFlow().collectAsState(initial = com.example.flightlog.data.db.TariffEntity())

                MainScreen(
                    flights = flights,
                    tariff = tariff,
                    onAddFlight = { flight ->
                        lifecycleScope.launch {
                            db.flightDao().insertFlight(flight)
                        }
                    },
                    onDeleteFlight = { flight ->
                        lifecycleScope.launch {
                            db.flightDao().deleteFlight(flight)
                        }
                    }
                )
            }
        }
    }
}
