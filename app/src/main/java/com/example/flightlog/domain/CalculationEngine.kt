package com.example.flightlog.domain

import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity

data class MonthlyReport(
    val totalLandMinutes: Int = 0,
    val totalSeaMinutes: Int = 0,
    val totalFlightDays: Int = 0,
    val totalDutyDays: Int = 0,
    val landPayment: Double = 0.0,
    val seaPayment: Double = 0.0,
    val dutyPayment: Double = 0.0,
    val totalPayment: Double = 0.0
) {
    val totalMinutes: Int get() = totalLandMinutes + totalSeaMinutes
}

object CalculationEngine {
    fun calculateMonthlyReport(
        flights: List<FlightEntity>,
        duty: DutyEntity?,
        tariff: TariffEntity?
    ): MonthlyReport {
        val t = tariff ?: TariffEntity()
        val totalLand = flights.sumOf { it.landTimeMinutes }
        val totalSea = flights.sumOf { it.seaTimeMinutes }
        val flightDays = flights.map { it.dateTimestamp }.distinct().size
        val dutyDays = duty?.dutyDays ?: 0

        val landPay = (totalLand / 60.0) * t.landHourlyRate
        val seaPay = (totalSea / 60.0) * t.seaHourlyRate
        val dutyPay = dutyDays * t.dutyDayRate
        val flightPay = flightDays * t.flightDayRate

        val total = landPay + seaPay + dutyPay + flightPay

        return MonthlyReport(
            totalLandMinutes = totalLand,
            totalSeaMinutes = totalSea,
            totalFlightDays = flightDays,
            totalDutyDays = dutyDays,
            landPayment = landPay,
            seaPayment = seaPay,
            dutyPayment = dutyPay,
            totalPayment = total
        )
    }
}
