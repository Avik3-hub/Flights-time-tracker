package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
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

                // Фиксируем ширину колонок (КВС = 14 знаков)
                sheet.width(0, 12.0) // Дата
                sheet.width(1, 10.0) // № ВС
                sheet.width(2, 12.0) // Задание
                sheet.width(3, 14.0) // КВС
                sheet.width(4, 15.0) // Земля
                sheet.width(5, 15.0) // Море

                // 1. Шапка
                val headers = listOf("Дата", "№ ВС", "Задание", "КВС", "Земля", "Море")
                headers.forEachIndexed { col, title ->
                    sheet.value(0, col, title)
                    sheet.style(0, col).bold().fillColor("E0E0E0").horizontalAlignment("center").set()
                }

                // 2. Список полётов с голубой заливкой часов
                var row = 1
                flights.forEach { flight ->
                    sheet.value(row, 0, formatDate(flight.dateTimestamp))
                    sheet.value(row, 1, flight.aircraftNumber)
                    sheet.value(row, 2, flight.missionNumber ?: "")
                    sheet.value(row, 3, flight.captain)

                    sheet.value(row, 4, flight.landTimeMinutes.minutesToHoursAndMinutes())
                    sheet.style(row, 4).fillColor("C6D9F1").horizontalAlignment("center").set()

                    sheet.value(row, 5, flight.seaTimeMinutes.minutesToHoursAndMinutes())
                    sheet.style(row, 5).fillColor("C6D9F1").horizontalAlignment("center").set()

                    row++
                }

                // 3. Расчёты
                val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
                val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }

                val landPayment = (totalLandMinutes / 60.0) * tariff.landHourlyRate * 0.87
                val seaPayment = (totalSeaMinutes / 60.0) * tariff.seaHourlyRate * 0.87
                val flightPayment = landPayment + seaPayment

                val dutyDays = duty?.dutyDays ?: 0
                val dutyPayment = dutyDays * tariff.dutyDayRate * 0.87

                val grandTotalPayment = flightPayment + dutyPayment

                row++ // пустая строка перед блоком итогов

                // 4. Сумма часов
                sheet.value(row, 4, "Сумма часов:")
                sheet.range(row, 4, row, 5).merge()
                sheet.style(row, 4).bold().horizontalAlignment("center").set()
                row++

                sheet.value(row, 4, totalLandMinutes.minutesToHoursAndMinutes())
                sheet.style(row, 4).bold().fillColor("C6D9F1").horizontalAlignment("center").set()

                sheet.value(row, 5, totalSeaMinutes.minutesToHoursAndMinutes())
                sheet.style(row, 5).bold().fillColor("C6D9F1").horizontalAlignment("center").set()
                row++

                // 5. Сумма денег за налёт
                sheet.value(row, 4, "Сумма:")
                sheet.range(row, 4, row, 5).merge()
                sheet.style(row, 4).bold().horizontalAlignment("center").set()
                row++

                sheet.value(row, 4, "${String.format("%.2f", landPayment)} руб.")
                sheet.style(row, 4).horizontalAlignment("center").set()

                sheet.value(row, 5, "${String.format("%.2f", seaPayment)} руб.")
                sheet.style(row, 5).horizontalAlignment("center").set()
                row++

                // 6. Дежурство
                sheet.value(row, 4, "Дежурство:")
                sheet.style(row, 4).bold().horizontalAlignment("right").set()

                sheet.value(row, 5, "${String.format("%.2f", dutyPayment)} руб.")
                sheet.style(row, 5).bold().fillColor("D9EAD3").horizontalAlignment("center").set()
                row++

                // 7. Общая сумма
                sheet.value(row, 4, "Общая сумма:")
                sheet.range(row, 4, row, 5).merge()
                sheet.style(row, 4).bold().horizontalAlignment("center").set()
                row++

                sheet.value(row, 4, "${String.format("%.2f", grandTotalPayment)} руб.")
                sheet.range(row, 4, row, 5).merge()
                sheet.style(row, 4).bold().fillColor("FCE4D6").horizontalAlignment("center").set()

                workbook.finish()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
