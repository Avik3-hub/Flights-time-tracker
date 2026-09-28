package com.example.flightlog.data.db

import androidx.room.ColumnInfo
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
    val seaTimeMinutes: Int,
    @ColumnInfo(defaultValue = "'Пассажирский'") val flightType: String = "Пассажирский"
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
    val landHourlyRate: Double = DEFAULT_LAND_HOURLY_RATE,
    val seaHourlyRate: Double = DEFAULT_SEA_HOURLY_RATE,
    val dutyDayRate: Double = DEFAULT_DUTY_DAY_RATE
) {
    companion object {
        const val DEFAULT_EFFECTIVE_YEAR = 2026
        const val DEFAULT_EFFECTIVE_MONTH = 7
        const val DEFAULT_LAND_HOURLY_RATE = 1180.0
        const val DEFAULT_SEA_HOURLY_RATE = 5964.0
        const val DEFAULT_DUTY_DAY_RATE = 1952.0

        fun default(
            effectiveFromYear: Int = DEFAULT_EFFECTIVE_YEAR,
            effectiveFromMonth: Int = DEFAULT_EFFECTIVE_MONTH
        ) = TariffEntity(
            effectiveFromYear = effectiveFromYear,
            effectiveFromMonth = effectiveFromMonth,
            landHourlyRate = DEFAULT_LAND_HOURLY_RATE,
            seaHourlyRate = DEFAULT_SEA_HOURLY_RATE,
            dutyDayRate = DEFAULT_DUTY_DAY_RATE
        )
    }
}
