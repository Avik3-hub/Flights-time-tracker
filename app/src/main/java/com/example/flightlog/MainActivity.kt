package com.example.flightlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.theme.FlightLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        val flightDao = db.flightDao()
        val tariffDao = db.tariffDao()

        setContent {
            FlightLogTheme {
                val flights by flightDao.getAllFlights().collectAsState(initial = emptyList())
                val tariffState by tariffDao.getTariff().collectAsState(initial = null)
                val scope = rememberCoroutineScope()

                val currentTariff = tariffState ?: TariffEntity()

                MainScreen(
                    flights = flights,
                    tariff = currentTariff,
                    onAddFlight = { flight ->
                        scope.launch {
                            flightDao.insertFlight(flight)
                        }
                    },
                    onDeleteFlight = { flight ->
                        scope.launch {
                            flightDao.deleteFlight(flight)
                        }
                    }
                )
            }
        }
    }
}
