package com.example.flightlog.domain

import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity

data class MonthlyReport(
    val totalLandMinutes: Int,
    val totalSeaMinutes: Int,
    val totalFlightDays: Int,
    val totalDutyDays: Int,
    val landPayment: Double,
    val seaPayment: Double,
    val dutyPayment: Double,
    val totalPayment: Double
) {
    val totalMinutes: Int get() = totalLandMinutes + totalSeaMinutes
}

object CalculationEngine {

    fun formatMinutesToHHMM(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    fun parseHHMMToMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun calculateFlightPayment(minutes: Int, ratePerHour: Double): Double {
        return (minutes / 60.0) * ratePerHour
    }

    fun generateMonthlyReport(
        flights: List<FlightEntity>,
        duties: List<DutyEntity>,
        tariff: TariffEntity
    ): MonthlyReport {
        val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
        val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }
        
        val totalFlightDays = flights.map { it.dateTimestamp }.distinct().size
        val totalDutyDays = duties.sumOf { it.dutyDays }

        val landPayment = calculateFlightPayment(totalLandMinutes, tariff.landHourlyRate)
        val seaPayment = calculateFlightPayment(totalSeaMinutes, tariff.seaHourlyRate)
        val dutyPayment = totalDutyDays * tariff.dutyDayRate
        val totalPayment = landPayment + seaPayment + dutyPayment

        return MonthlyReport(
            totalLandMinutes = totalLandMinutes,
            totalSeaMinutes = totalSeaMinutes,
            totalFlightDays = totalFlightDays,
            totalDutyDays = totalDutyDays,
            landPayment = landPayment,
            seaPayment = seaPayment,
            dutyPayment = dutyPayment,
            totalPayment = totalPayment
        )
    }
}
