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

@Entity(
    tableName = "duty_records",
    primaryKeys = ["year", "month"]
)
data class DutyEntity(
    val year: Int,
    val month: Int,
    val dutyDays: Int
)

@Entity(
    tableName = "tariff_config",
    primaryKeys = ["effectiveFromYear", "effectiveFromMonth"]
)
data class TariffEntity(
    val effectiveFromYear: Int,
    val effectiveFromMonth: Int,
    val landHourlyRate: Double = 879.57,
    val seaHourlyRate: Double = 0.0,
    val dutyDayRate: Double = 0.0
)
