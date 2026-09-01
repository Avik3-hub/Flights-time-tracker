package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
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
            for (flight in flights.sortedBy { it.dateTimestamp }) {
                val row = flightSheet.createRow(rowIndex++)
                val dateStr = dateFormat.format(flight.dateTimestamp)
                
                row.createCell(0).setCellValue(dateStr)
                row.createCell(1).setCellValue(flight.aircraftNumber)
                row.createCell(2).setCellValue(flight.missionNumber ?: "")
                row.createCell(3).setCellValue(flight.captain)
                row.createCell(4).setCellValue(formatMinutesToHHMM(flight.landTimeMinutes))
                row.createCell(5).setCellValue(formatMinutesToHHMM(flight.seaTimeMinutes))
            }

            // 2. Отдельный лист "Дежурства" с разбивкой ПО МЕСЯЦАМ
            val dutySheet = workbook.createSheet("Дежурства")
            val dutyHeader = dutySheet.createRow(0)
            dutyHeader.createCell(0).setCellValue("Год")
            dutyHeader.createCell(1).setCellValue("Месяц")
            dutyHeader.createCell(2).setCellValue("Дней дежурства")

            var dutyRowIndex = 1
            // Сортируем дежурства по году и месяцу
            for (duty in duties.sortedWith(compareBy({ it.year }, { it.month }))) {
                if (duty.dutyDays > 0) {
                    val row = dutySheet.createRow(dutyRowIndex++)
                    row.createCell(0).setCellValue(duty.year.toDouble())
                    row.createCell(1).setCellValue(duty.month.toDouble()) // Числовой номер месяца (1-12)
                    row.createCell(2).setCellValue(duty.dutyDays.toDouble())
                }
            }

            // 3. Лист с тарифами (для точного восстановления расчетов)
            val tariffList = if (allTariffs.isNotEmpty()) allTariffs else listOfNotNull(activeTariff)
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
            }

            // Сохранение файла
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

    private fun formatMinutesToHHMM(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }
}
