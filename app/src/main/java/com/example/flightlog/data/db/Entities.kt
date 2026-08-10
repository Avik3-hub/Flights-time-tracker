package com.example.flightlog.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flight_records")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long,
    val aircraftNumber: String,
    val captain: String,
    val missionNumber: String?,
    val landTimeMinutes: Int,
    val seaTimeMinutes: Int
)

@Entity(tableName = "duty_records")
data class DutyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int,
    val year: Int,
    val dutyDays: Int
)

@Entity(tableName = "tariff_config")
data class TariffEntity(
    @PrimaryKey val id: Int = 1,
    val landHourlyRate: Double = 879.57,
    val seaHourlyRate: Double = 0.0,
    val dutyDayRate: Double = 0.0,
    val flightDayRate: Double = 0.0
)
