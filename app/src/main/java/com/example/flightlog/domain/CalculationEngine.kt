package com.example.flightlog.domain

import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import kotlin.math.roundToLong

data class FlightSummary(
    val totalFlightDays: Int,
    val totalLandMinutes: Int,
    val totalSeaMinutes: Int,
    val totalLandMoney: Double,
    val totalSeaMoney: Double,
    val totalDutyMoney: Double,
    val grandTotalMoney: Double
) {
    val totalMinutes: Int get() = totalLandMinutes + totalSeaMinutes
}

object CalculationEngine {

    fun minutesToHoursAndMinutes(totalMinutes: Int): Pair<Int, Int> {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return Pair(hours, minutes)
    }

    fun parseTimeStringToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val parts = timeStr.trim().split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun calculateFlightEarnings(
        landMinutes: Int,
        seaMinutes: Int,
        tariff: TariffEntity
    ): Pair<Double, Double> {
        val landHours = landMinutes / 60.0
        val seaHours = seaMinutes / 60.0

        val landEarnings = landHours * tariff.landHourlyRate
        val seaEarnings = seaHours * tariff.seaHourlyRate

        return Pair(landEarnings, seaEarnings)
    }

    fun calculateSummary(
        flights: List<FlightEntity>,
        duties: List<DutyEntity>,
        tariff: TariffEntity
    ): FlightSummary {
        val uniqueFlightDays = flights.map { it.dateTimestamp }.distinct().size

        var landMinutesSum = 0
        var seaMinutesSum = 0
        var landMoneySum = 0.0
        var seaMoneySum = 0.0

        flights.forEach { flight ->
            landMinutesSum += flight.landTimeMinutes
            seaMinutesSum += flight.seaTimeMinutes

            val (landMoney, seaMoney) = calculateFlightEarnings(
                flight.landTimeMinutes,
                flight.seaTimeMinutes,
                tariff
            )
            landMoneySum += landMoney
            seaMoneySum += seaMoney
        }

        val dutyMoneySum = duties.sumOf { it.daysCount * tariff.dutyDailyRate }
        val grandTotal = landMoneySum + seaMoneySum + dutyMoneySum

        return FlightSummary(
            totalFlightDays = uniqueFlightDays,
            totalLandMinutes = landMinutesSum,
            totalSeaMinutes = seaMinutesSum,
            totalLandMoney = roundToTwoDecimals(landMoneySum),
            totalSeaMoney = roundToTwoDecimals(seaMoneySum),
            totalDutyMoney = roundToTwoDecimals(dutyMoneySum),
            grandTotalMoney = roundToTwoDecimals(grandTotal)
        )
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToLong() / 100.0
    }
}
