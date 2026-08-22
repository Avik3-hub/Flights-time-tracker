package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.ui.formatDate
import com.example.flightlog.ui.minutesToHoursAndMinutes
import org.dhatim.fastexcel.Workbook

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        uri: Uri,
        flights: List<FlightEntity>,
        duty: DutyEntity?,
        tariff: TariffEntity
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = Workbook(outputStream, "FlightLog", "1.0")
                val sheet = workbook.newWorksheet("Отчет по налету")

                // Шапка таблицы
                val headers = listOf("Дата", "№ ВС", "Задание", "КВС", "Земля", "Море", "Всего")
                headers.forEachIndexed { col, title ->
                    sheet.value(0, col, title)
                    sheet.style(0, col).bold().fillColor("E0E0E0").set()
                }

                // Данные полётов
                flights.forEachIndexed { index, flight ->
                    val row = index + 1
                    val totalMinutes = flight.landTimeMinutes + flight.seaTimeMinutes

                    sheet.value(row, 0, formatDate(flight.dateTimestamp))
                    sheet.value(row, 1, flight.aircraftNumber)
                    sheet.value(row, 2, flight.missionNumber ?: "")
                    sheet.value(row, 3, flight.captain)
                    sheet.value(row, 4, flight.landTimeMinutes.minutesToHoursAndMinutes())
                    sheet.value(row, 5, flight.seaTimeMinutes.minutesToHoursAndMinutes())
                    sheet.value(row, 6, totalMinutes.minutesToHoursAndMinutes())
                }

                // Итоговый расчет
                val report = CalculationEngine.calculateMonthlyReport(
                    flights,
                    duty ?: DutyEntity(month = 0, year = 0, dutyDays = 0),
                    tariff
                )

                val summaryRow = flights.size + 2
                sheet.value(summaryRow, 0, "ИТОГО К ВЫПЛАТЕ (с вычетом 13% НДФЛ):")
                sheet.style(summaryRow, 0).bold().set()
                sheet.value(summaryRow, 6, "${String.format("%.2f", report.totalPayment)} руб.")
                sheet.style(summaryRow, 6).bold().set()

                workbook.finish()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
