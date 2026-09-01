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
    primaryKeys = ["year", "month"] // Составной ключ: замена будет работать по Году и Месяцу
)
data class DutyEntity(
    val year: Int,
    val month: Int,
    val dutyDays: Int
)

@Entity(tableName = "tariff_config")
data class TariffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Изменено: теперь генерируется автоматически для сохранения истории
    val effectiveFromYear: Int,                        // Год ввода тарифа (например, 2025)
    val effectiveFromMonth: Int,                       // Месяц ввода тарифа (1-12, например 7 для июля)
    val landHourlyRate: Double = 879.57,
    val seaHourlyRate: Double = 0.0,
    val dutyDayRate: Double = 0.0
)
