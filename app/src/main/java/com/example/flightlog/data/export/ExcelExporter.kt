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

                // 1. Шапка таблицы
                val headers = listOf("Дата", "№ ВС", "Задание", "КВС", "Земля", "Море", "Всего")
                headers.forEachIndexed { col, title ->
                    sheet.value(0, col, title)
                    sheet.style(0, col).bold().fillColor("E0E0E0").set()
                }

                // 2. Заполнение списка полетов
                var row = 1
                flights.forEach { flight ->
                    val totalMinutes = flight.landTimeMinutes + flight.seaTimeMinutes

                    sheet.value(row, 0, formatDate(flight.dateTimestamp))
                    sheet.value(row, 1, flight.aircraftNumber)
                    sheet.value(row, 2, flight.missionNumber ?: "")
                    sheet.value(row, 3, flight.captain)
                    sheet.value(row, 4, flight.landTimeMinutes.minutesToHoursAndMinutes())
                    sheet.value(row, 5, flight.seaTimeMinutes.minutesToHoursAndMinutes())
                    sheet.value(row, 6, totalMinutes.minutesToHoursAndMinutes())
                    row++
                }

                // 3. Расчет сумм (с автовычетом 13% НДФЛ)
                val totalLandMinutes = flights.sumOf { it.landTimeMinutes }
                val totalSeaMinutes = flights.sumOf { it.seaTimeMinutes }
                val totalFlightMinutes = totalLandMinutes + totalSeaMinutes

                val landPayment = (totalLandMinutes / 60.0) * tariff.landHourlyRate * 0.87
                val seaPayment = (totalSeaMinutes / 60.0) * tariff.seaHourlyRate * 0.87
                val flightPayment = landPayment + seaPayment

                val dutyDays = duty?.dutyDays ?: 0
                val dutyPayment = dutyDays * tariff.dutyDayRate * 0.87

                val grandTotalPayment = flightPayment + dutyPayment

                // 4. Итоговый детализированный блок
                row++ // пустая строка-разделитель

                // Земля
                sheet.value(row, 3, "Налет (Земля):")
                sheet.value(row, 4, totalLandMinutes.minutesToHoursAndMinutes())
                sheet.value(row, 6, "${String.format("%.2f", landPayment)} руб.")
                row++

                // Море
                sheet.value(row, 3, "Налет (Море):")
                sheet.value(row, 5, totalSeaMinutes.minutesToHoursAndMinutes())
                sheet.value(row, 6, "${String.format("%.2f", seaPayment)} руб.")
                row++

                // Итого за налет
                sheet.value(row, 3, "Итого за налет:")
                sheet.style(row, 3).bold().set()
                sheet.value(row, 4, totalFlightMinutes.minutesToHoursAndMinutes())
                sheet.style(row, 4).bold().set()
                sheet.value(row, 6, "${String.format("%.2f", flightPayment)} руб.")
                sheet.style(row, 6).bold().set()
                row++

                // Дежурства
                sheet.value(row, 3, "Дежурства:")
                sheet.value(row, 4, "$dutyDays дн.")
                sheet.value(row, 6, "${String.format("%.2f", dutyPayment)} руб.")
                row++

                row++ // пустая строка перед главным итогом

                // ВИШЕНКА НА ТОРТЕ: Общий итог
                sheet.value(row, 3, "ОБЩАЯ СУММА К ВЫПЛАТЕ (с вычетом 13% НДФЛ):")
                sheet.style(row, 3).bold().set()
                sheet.value(row, 6, "${String.format("%.2f", grandTotalPayment)} руб.")
                sheet.style(row, 6).bold().fillColor("D9EAD3").set() // Заливка светло-зеленым

                workbook.finish()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
