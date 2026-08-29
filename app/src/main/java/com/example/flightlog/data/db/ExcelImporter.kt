package com.example.flightlog.data.db

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExcelImporter {

    data class ImportResult(
        val flights: List<FlightEntity>,
        val duties: List<DutyEntity>
    )

    fun importFromExcel(context: Context, uri: Uri): ImportResult {
        val flights = mutableListOf<FlightEntity>()
        val duties = mutableListOf<DutyEntity>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = sheet.sheetName.trim()
                var sheetMonth = getMonthIndex(sheetName)
                var sheetYear = 2026
                var dutyDaysForMonth = 0

                // Читаем строки со 2-й (индекс 1 в 0-based нумерации POI), пропуская заголовок
                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val dateStr = getCellSafe(row, 0)
                        val aircraftNum = getCellSafe(row, 1)
                        val landHoursStr = getCellSafe(row, 2)
                        val seaHoursStr = getCellSafe(row, 3)
                        val captain = getCellSafe(row, 4)
                        val missionNum = getCellSafe(row, 5)
                        val dutyCellStr = getCellSafe(row, 6)

                        val dutyDaysInRow = dutyCellStr.toIntOrNull() ?: 0
                        if (dutyDaysInRow > 0) {
                            dutyDaysForMonth += dutyDaysInRow
                        }

                        if (dateStr.isNotBlank() && (landHoursStr.isNotBlank() || seaHoursStr.isNotBlank())) {
                            val timestamp = parseDateToTimestamp(dateStr)
                            val landMinutes = parseTimeToMinutes(landHoursStr)
                            val seaMinutes = parseTimeToMinutes(seaHoursStr)

                            flights.add(
                                FlightEntity(
                                    dateTimestamp = timestamp,
                                    aircraftNumber = aircraftNum,
                                    captain = captain,
                                    missionNumber = missionNum.ifBlank { null },
                                    landTimeMinutes = landMinutes,
                                    seaTimeMinutes = seaMinutes
                                )
                            )

                            if (timestamp > 0) {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                sheetMonth = cal.get(Calendar.MONTH) + 1
                                sheetYear = cal.get(Calendar.YEAR)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (dutyDaysForMonth > 0 && sheetMonth in 1..12) {
                    duties.add(
                        DutyEntity(
                            month = sheetMonth,
                            year = sheetYear,
                            dutyDays = dutyDaysForMonth
                        )
                    )
                }
            }
            workbook.close()
        }

        return ImportResult(flights, duties)
    }

    // Безопасное получение значений из любых типов ячеек Excel
    private fun getCellSafe(row: Row, index: Int): String {
        val cell = row.getCell(index) ?: return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    sdf.format(cell.dateCellValue)
                } else {
                    val numeric = cell.numericCellValue
                    if (numeric == numeric.toLong().toDouble()) {
                        numeric.toLong().toString()
                    } else {
                        numeric.toString()
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                runCatching { cell.stringCellValue.trim() }.getOrElse {
                    runCatching {
                        val num = cell.numericCellValue
                        if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                    }.getOrDefault("")
                }
            }
            else -> ""
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        return if (timeStr.contains(":")) {
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            h * 60 + m
        } else {
            val doubleHours = timeStr.toDoubleOrNull() ?: 0.0
            (doubleHours * 60).toInt()
        }
    }

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun getMonthIndex(sheetName: String): Int {
        val months = listOf(
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
        )
        val index = months.indexOfFirst { sheetName.lowercase().contains(it) }
        return if (index >= 0) index + 1 else 0
    }
}
