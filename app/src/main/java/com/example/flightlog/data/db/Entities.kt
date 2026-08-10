package com.example.flightlog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long,
    val aircraftNumber: String,
    val captain: String,
    val missionNumber: String?,
    val landTimeMinutes: Int,
    val seaTimeMinutes: Int
)

@Entity(tableName = "tariffs")
data class TariffEntity(
    @PrimaryKey val id: Int = 1,
    val landHourRate: Double = 0.0,
    val seaHourRate: Double = 0.0,
    val dutyDayRate: Double = 0.0,
    val flightDayRate: Double = 0.0
)
