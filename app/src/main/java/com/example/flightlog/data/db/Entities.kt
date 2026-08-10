package com.example.flightlog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flight_records")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTimestamp: Long,
    val aircraftNumber: String,
    val captain: String,
    val missionNumber: String? = null,
    val landTimeMinutes: Int = 0,
    val seaTimeMinutes: Int = 0
)

@Entity(tableName = "duty_records")
data class DutyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val daysCount: Int,
    val month: Int,
    val year: Int
)

@Entity(tableName = "tariff_config")
data class TariffEntity(
    @PrimaryKey
    val id: Long = 1,
    val landHourlyRate: Double = 879.57,
    val seaHourlyRate: Double = 0.0,
    val dutyDailyRate: Double = 1698.24
)
