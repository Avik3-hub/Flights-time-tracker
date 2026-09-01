package com.example.flightlog.domain

import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import java.util.Calendar

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
    private const val TAX_FACTOR = 0.87

    /**
     * Поиск тарифа, действующего для конкретного года и месяца.
     * Если точного совпадения нет, берется последний актуальный тариф из базы 
     * или базовые ставки, предотвращая обнуление выплат.
     */
    fun getActiveTariff(tariffs: List<TariffEntity>, year: Int, month: Int): TariffEntity {
        val targetPeriod = year * 12 + month
        
        // 1. Ищем подходящий по периоду тариф
        val active = tariffs
            .filter { (it.effectiveFromYear * 12 + it.effectiveFromMonth) <= targetPeriod }
            .maxByOrNull { it.effectiveFromYear * 12 + it.effectiveFromMonth }

        if (active != null) return active

        // 2. Если точного нет, но в базе есть другие тарифы — берем самый свежий из доступных
        tariffs.maxByOrNull { it.effectiveFromYear * 12 + it.effectiveFromMonth }?.let {
            return it
        }

        // 3. Абсолютный фоллбек, если таблица тарифов полностью пуста
        return TariffEntity(
            effectiveFromYear = year,
            effectiveFromMonth = month,
            landHourlyRate = 879.57,
            seaHourlyRate = 0.0,
            dutyDayRate = 0.0
        )
    }

    /**
     * Метод для экрана MainScreen, принимающий отфильтрованные полеты за период,
     * одно дежурство и один активный тариф.
     */
    fun calculateMonthlyReport(
        flights: List<FlightEntity>,
        dutyRecord: DutyEntity?,
        tariff: TariffEntity
    ): MonthlyReport {
        val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
        val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }
        val totalMinutes = totalLandMinutes + totalSeaMinutes

        // Подсчет уникальных лётных дней по календарной дате
        val totalFlightDays = flights
            .map { flight ->
                val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            .distinct()
            .size

        var landPayment = 0.0
        var seaPayment = 0.0

        for (flight in flights) {
            landPayment += (flight.landTimeMinutes / 60.0) * tariff.landHourlyRate * TAX_FACTOR
            seaPayment += (flight.seaTimeMinutes / 60.0) * tariff.seaHourlyRate * TAX_FACTOR
        }

        val dutyPayment = if (dutyRecord != null) {
            dutyRecord.dutyDays * tariff.dutyDayRate * TAX_FACTOR
        } else {
            0.0
        }

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

    /**
     * Полный расчет выплат за выбранный период (месяц или год) со списками.
     * Для каждого полета и дежурства динамически применяется тариф своего месяца.
     */
    fun calculateReport(
        flights: List<FlightEntity>,
        duties: List<DutyEntity>,
        tariffs: List<TariffEntity>
    ): MonthlyReport {
        val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
        val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }
        val totalMinutes = totalLandMinutes + totalSeaMinutes

        val totalFlightDays = flights
            .map { flight ->
                val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            .distinct()
            .size

        var landPayment = 0.0
        var seaPayment = 0.0

        for (flight in flights) {
            val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
            val fYear = cal.get(Calendar.YEAR)
            val fMonth = cal.get(Calendar.MONTH) + 1

            val tariff = getActiveTariff(tariffs, fYear, fMonth)

            landPayment += (flight.landTimeMinutes / 60.0) * tariff.landHourlyRate * TAX_FACTOR
            seaPayment += (flight.seaTimeMinutes / 60.0) * tariff.seaHourlyRate * TAX_FACTOR
        }

        var dutyPayment = 0.0
        for (duty in duties) {
            val tariff = getActiveTariff(tariffs, duty.year, duty.month)
            dutyPayment += duty.dutyDays * tariff.dutyDayRate * TAX_FACTOR
        }

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
