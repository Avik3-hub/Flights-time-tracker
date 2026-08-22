package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.ui.formatDate
import com.example.flightlog.ui.minutesToHoursAndMinutes
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        uri: Uri,
        flights: List<FlightEntity>,
        duty: DutyEntity?,
        tariff: TariffEntity
    ): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Отчет по налету")

            // Стиль заголовков
            val headerStyle: CellStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply { isBold = true }
                setFont(font)
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            // Шапка таблицы
            val headers = listOf("Дата", "№ ВС", "Задание", "КВС", "Земля", "Море", "Всего")
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
                cell.setCellStyle(headerStyle)
            }

            // Заполнение списком полетов
            var rowIndex = 1
            flights.forEach { flight ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(formatDate(flight.dateTimestamp))
                row.createCell(1).setCellValue(flight.aircraftNumber)
                row.createCell(2).setCellValue(flight.missionNumber ?: "")
                row.createCell(3).setCellValue(flight.captain)
                row.createCell(4).setCellValue(flight.landTimeMinutes.minutesToHoursAndMinutes())
                row.createCell(5).setCellValue(flight.seaTimeMinutes.minutesToHoursAndMinutes())
                row.createCell(6).setCellValue((flight.landTimeMinutes + flight.seaTimeMinutes).minutesToHoursAndMinutes())
            }

            // Расчет итогов
            val report = CalculationEngine.calculateMonthlyReport(
                flights,
                duty ?: DutyEntity(month = 0, year = 0, dutyDays = 0),
                tariff
            )

            rowIndex++
            val summaryRow = sheet.createRow(rowIndex)
            summaryRow.createCell(0).setCellValue("ИТОГО К ВЫПЛАТЕ (с вычетом 13% НДФЛ):")
            summaryRow.createCell(6).setCellValue("${String.format("%.2f", report.totalPayment)} руб.")

            // Автонастройка ширины столбцов
            headers.indices.forEach { sheet.autoSizeColumn(it) }

            // Запись в выбранный файл
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
