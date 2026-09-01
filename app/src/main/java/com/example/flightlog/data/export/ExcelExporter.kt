package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        uri: Uri,
        flights: List<FlightEntity>,
        duties: List<DutyEntity>,
        activeTariff: TariffEntity?,
        allTariffs: List<TariffEntity> = emptyList()
    ): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val tariffList = if (allTariffs.isNotEmpty()) allTariffs else listOfNotNull(activeTariff)
            val sortedFlights = flights.sortedBy { it.dateTimestamp }

            // Фильтрация дежурств под период выгружаемых полетов
            val calendar = Calendar.getInstance()
            val flightMonths = sortedFlights.map { flight ->
                calendar.timeInMillis = flight.dateTimestamp
                Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            }.toSet()

            val filteredDuties = if (flightMonths.isNotEmpty()) {
                duties.filter { duty -> Pair(duty.year, duty.month) in flightMonths }
            } else {
                duties
            }

            // 1. Лист со списком полётов
            val flightSheet = workbook.createSheet("Полёты")
            val flightHeader = flightSheet.createRow(0)
            flightHeader.createCell(0).setCellValue("Дата")
            flightHeader.createCell(1).setCellValue("№ ВС")
            flightHeader.createCell(2).setCellValue("Задание")
            flightHeader.createCell(3).setCellValue("ФИО КВС")
            flightHeader.createCell(4).setCellValue("Земля (ч:мм)")
            flightHeader.createCell(5).setCellValue("Море (ч:мм)")

            var rowIndex = 1
            for (flight in sortedFlights) {
                val row = flightSheet.createRow(rowIndex++)
                val dateStr = dateFormat.format(flight.dateTimestamp)
                
                row.createCell(0).setCellValue(dateStr)
                row.createCell(1).setCellValue(flight.aircraftNumber)
                row.createCell(2).setCellValue(flight.missionNumber ?: "")
                row.createCell(3).setCellValue(flight.captain)
                row.createCell(4).setCellValue(formatMinutesToHHMM(flight.landTimeMinutes))
                row.createCell(5).setCellValue(formatMinutesToHHMM(flight.seaTimeMinutes))
            }
            for (i in 0..5) {
                flightSheet.setColumnWidth(i, 10 * 256)
            }

            // 2. Лист "Дежурства" (отфильтрованный)
            val dutySheet = workbook.createSheet("Дежурства")
            val dutyHeader = dutySheet.createRow(0)
            dutyHeader.createCell(0).setCellValue("Год")
            dutyHeader.createCell(1).setCellValue("Месяц")
            dutyHeader.createCell(2).setCellValue("Дней дежурства")

            var dutyRowIndex = 1
            for (duty in filteredDuties.sortedWith(compareBy({ it.year }, { it.month }))) {
                if (duty.dutyDays > 0 && duty.month in 1..12) {
                    val row = dutySheet.createRow(dutyRowIndex++)
                    row.createCell(0).setCellValue(duty.year.toDouble())
                    row.createCell(1).setCellValue(duty.month.toDouble())
                    row.createCell(2).setCellValue(duty.dutyDays.toDouble())
                }
            }
            for (i in 0..2) {
                dutySheet.setColumnWidth(i, 9 * 256)
            }

            // 3. Лист с тарифами
            if (tariffList.isNotEmpty()) {
                val tariffSheet = workbook.createSheet("Тарифы")
                val tariffHeader = tariffSheet.createRow(0)
                tariffHeader.createCell(0).setCellValue("Год")
                tariffHeader.createCell(1).setCellValue("Месяц")
                tariffHeader.createCell(2).setCellValue("Ставка Земля")
                tariffHeader.createCell(3).setCellValue("Ставка Море")
                tariffHeader.createCell(4).setCellValue("Ставка Дежурство")

                var tariffRowIndex = 1
                for (t in tariffList) {
                    val row = tariffSheet.createRow(tariffRowIndex++)
                    row.createCell(0).setCellValue(t.effectiveFromYear.toDouble())
                    row.createCell(1).setCellValue(t.effectiveFromMonth.toDouble())
                    row.createCell(2).setCellValue(t.landHourlyRate)
                    row.createCell(3).setCellValue(t.seaHourlyRate)
                    row.createCell(4).setCellValue(t.dutyDayRate)
                }
                for (i in 0..4) {
                    tariffSheet.setColumnWidth(i, 10 * 256)
                }
            }

            // 4. Отдельный лист "Сводка" с финансовым отчетом
            try {
                if (sortedFlights.isNotEmpty() || filteredDuties.isNotEmpty()) {
                    val report = CalculationEngine.calculateReport(
                        flights = sortedFlights,
                        duties = filteredDuties,
                        tariffs = tariffList
                    )

                    val summarySheet = workbook.createSheet("Сводка")
                    var sumRowIdx = 0

                    val titleRow = summarySheet.createRow(sumRowIdx++)
                    titleRow.createCell(0).setCellValue("ФИНАНСОВЫЙ И ИТОГОВЫЙ ОТЧЕТ")

                    sumRowIdx++ // Пустая строка

                    fun addSummaryRow(label: String, value: Any) {
                        val row = summarySheet.createRow(sumRowIdx++)
                        row.createCell(0).setCellValue(label)
                        when (value) {
                            is Double -> row.createCell(1).setCellValue(value)
                            is Int -> row.createCell(1).setCellValue(value.toDouble())
                            is Long -> row.createCell(1).setCellValue(value.toDouble())
                            is String -> row.createCell(1).setCellValue(value)
                        }
                    }

                    addSummaryRow("Общий налет", formatMinutesToHHMM(report.totalMinutes))
                    addSummaryRow("Полетных дней", report.totalFlightDays)
                    addSummaryRow("Земля (общее)", formatMinutesToHHMM(report.totalLandMinutes))
                    addSummaryRow("Море (общее)", formatMinutesToHHMM(report.totalSeaMinutes))

                    val totalDutyDays = filteredDuties.filter { it.dutyDays > 0 && it.month in 1..12 }.sumOf { it.dutyDays }
                    if (totalDutyDays > 0) {
                        addSummaryRow("Дней дежурства (всего)", totalDutyDays)
                        addSummaryRow("Оплата за дежурство (₽)", report.dutyPayment)
                    }

                    addSummaryRow("Итоговая выплата (включая дежурство и с вычетом 13% НДФЛ) (₽)", report.totalPayment)

                    summarySheet.setColumnWidth(0, 25 * 256)
                    summarySheet.setColumnWidth(1, 10 * 256)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            // Сохранение файла
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    private fun formatMinutesToHHMM(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }
}
