package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
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
        val duties: List<DutyEntity>,
        val tariffs: List<TariffEntity> = emptyList()
    )

    fun importFromExcel(context: Context, uri: Uri): ImportResult {
        val flights = mutableListOf<FlightEntity>()
        val duties = mutableListOf<DutyEntity>()
        val tariffs = mutableListOf<TariffEntity>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = sheet.sheetName.trim()

                // 1. Чтение тарифов
                if (sheetName.equals("Тарифы", ignoreCase = true)) {
                    for (rowIndex in 1..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIndex) ?: continue
                        val year = getCellSafe(row, 0).toIntOrNull()
                        val month = getCellSafe(row, 1).toIntOrNull()
                        val landRate = getCellSafe(row, 2).toDoubleOrNull()
                        val seaRate = getCellSafe(row, 3).toDoubleOrNull()
                        val dutyRate = getCellSafe(row, 4).toDoubleOrNull()

                        if (year != null && month != null && landRate != null && seaRate != null && dutyRate != null) {
                            tariffs.add(
                                TariffEntity(
                                    effectiveFromYear = year,
                                    effectiveFromMonth = month,
                                    landHourlyRate = landRate,
                                    seaHourlyRate = seaRate,
                                    dutyDayRate = dutyRate
                                )
                            )
                        }
                    }
                    continue
                }

                // 2. Чтение полетов и дежурств
                var sheetMonth = getMonthIndex(sheetName)
                var sheetYear = 2026
                var dutyDaysForMonth = 0

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    try {
                        val dateStr = getCellSafe(row, 0)
                        val aircraftNum = getCellSafe(row, 1)
                        val missionNum = getCellSafe(row, 2)
                        val captain = getCellSafe(row, 3)
                        val landHoursStr = getCellSafe(row, 4)
                        val seaHoursStr = getCellSafe(row, 5)
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

        return ImportResult(flights, duties, tariffs)
    }

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
        val clean = timeStr.trim().lowercase()

        // 1. Формат "ЧЧ:ММ"
        if (clean.contains(":")) {
            val parts = clean.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }

        // 2. Текстовый формат "X ч Y мин" / "Xч Yмин"
        val hoursRegex = Regex("""(\d+)\s*ч""")
        val minsRegex = Regex("""(\d+)\s*мин""")
        val hMatch = hoursRegex.find(clean)
        val mMatch = minsRegex.find(clean)

        if (hMatch != null || mMatch != null) {
            val hours = hMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val minutes = mMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return hours * 60 + minutes
        }

        // 3. Десятичные числа
        val normalizedDouble = clean.replace(',', '.')
        val doubleHours = normalizedDouble.toDoubleOrNull() ?: 0.0
        return (doubleHours * 60).toInt()
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
