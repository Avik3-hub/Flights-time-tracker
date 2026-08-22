package com.example.flightlog.domain

import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity

data class MonthlyReport(
    val totalLandMinutes: Int,
    val totalSeaMinutes: Int,
    val totalMinutes: Int,
    val totalFlightDays: Int,
    val landPayment: Double,
    val seaPayment: Double,
    val dutyPayment: Double,
    val totalPayment: Double
)

object CalculationEngine {
    private const val TAX_FACTOR = 0.87 // Учет вычета 13% НДФЛ

    fun calculateMonthlyReport(
        flights: List<FlightEntity>,
        dutyRecord: DutyEntity,
        tariff: TariffEntity
    ): MonthlyReport {
        val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
        val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }
        val totalMinutes = totalLandMinutes + totalSeaMinutes

        val totalFlightDays = flights.map { it.dateTimestamp }.distinct().size

        // Чистая ставка после вычета 13%
        val netLandRate = tariff.landHourRate * TAX_FACTOR
        val netSeaRate = tariff.seaHourRate * TAX_FACTOR
        val netDutyRate = tariff.dutyDayRate * TAX_FACTOR

        val landPayment = (totalLandMinutes / 60.0) * netLandRate
        val seaPayment = (totalSeaMinutes / 60.0) * netSeaRate
        val dutyPayment = dutyRecord.dutyDays * netDutyRate

        val totalPayment = landPayment + seaPayment + dutyPayment

        return MonthlyReport(
            totalLandMinutes = totalLandMinutes,
            totalSeaMinutes = totalSeaMinutes,
            totalMinutes = totalMinutes,
            totalFlightDays = totalFlightDays,
            landPayment = landPayment,
            seaPayment = seaPayment,
            dutyPayment = dutyPayment,
            totalPayment = totalPayment
        )
    }
}
