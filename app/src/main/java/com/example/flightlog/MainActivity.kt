package com.example.flightlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.flightlog.data.db.AppDatabase
import com.example.flightlog.ui.MainScreen
import com.example.flightlog.ui.theme.FlightLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            FlightLogTheme {
                MainScreen(database = db)
            }
        }
    }
}
