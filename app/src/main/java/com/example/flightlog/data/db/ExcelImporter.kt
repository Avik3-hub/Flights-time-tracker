package com.example.flightlog.data.export

import android.content.Context
import android.net.Uri
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat
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

            // Лист 1 (индекс 0): Полеты
            if (workbook.numberOfSheets > 0) {
                val sheet = workbook.getSheetAt(0)
                val rowIterator = sheet.rowIterator()
                if (rowIterator.hasNext()) {
                    rowIterator.next() // Пропускаем заголовок
                    while (rowIterator.hasNext()) {
                        val row = rowIterator.next()
                        try {
                            val dateStr = getCellSafe(row, 0)
                            if (dateStr.isBlank()) continue

                            val aircraftNum = getCellSafe(row, 1).ifBlank { "б/н" }
                            val missionNum = getCellSafe(row, 2).ifBlank { null }
                            val captain = getCellSafe(row, 3).ifBlank { "Не указан" }

                            val landMinutes = parseCellToMinutes(row.getCell(5))
                            val seaMinutes = parseCellToMinutes(row.getCell(6))

                            val timestamp = parseDateToTimestamp(dateStr)

                            flights.add(
                                FlightEntity(
                                    dateTimestamp = timestamp,
                                    aircraftNumber = aircraftNum,
                                    captain = captain,
                                    missionNumber = missionNum,
                                    landTimeMinutes = landMinutes,
                                    seaTimeMinutes = seaMinutes
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            // Лист 2 (индекс 1): Дежурства
            if (workbook.numberOfSheets > 1) {
                val sheet = workbook.getSheetAt(1)
                val rowIterator = sheet.rowIterator()
                if (rowIterator.hasNext()) {
                    rowIterator.next() // Пропускаем заголовок
                    while (rowIterator.hasNext()) {
                        val row = rowIterator.next()
                        val rowValues = (0 until row.lastCellNum).map { getCellSafe(row, it) }
                        
                        var foundYear: Int? = null
                        var foundMonth: Int? = null
                        var foundDays: Int? = null

                        for (value in rowValues) {
                            val cleanNum = parseCleanDouble(value)?.toInt()
                            if (cleanNum != null && cleanNum in 2000..2100) {
                                foundYear = cleanNum
                                break
                            }
                        }

                        for (value in rowValues) {
                            val month = parseMonth(value)
                            if (month != null) {
                                foundMonth = month
                                break
                            }
                        }

                        for (value in rowValues) {
                            val num = parseCleanDouble(value)?.toInt()
                            if (num != null && num in 1..31 && num != foundYear && num != foundMonth) {
                                foundDays = num
                                break
                            }
                        }

                        if (foundYear != null && foundMonth != null && foundDays != null) {
                            if (duties.none { it.year == foundYear && it.month == foundMonth }) {
                                duties.add(
                                    DutyEntity(
                                        year = foundYear,
                                        month = foundMonth,
                                        dutyDays = foundDays
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Лист 3 (индекс 2): Тарифы
            if (workbook.numberOfSheets > 2) {
                val sheet = workbook.getSheetAt(2)
                val rowIterator = sheet.rowIterator()
                if (rowIterator.hasNext()) {
                    rowIterator.next() // Пропускаем заголовок
                    while (rowIterator.hasNext()) {
                        val row = rowIterator.next()
                        val rowValues = (0 until row.lastCellNum).map { getCellSafe(row, it) }
                        val numbers = rowValues.mapNotNull { parseCleanDouble(it) }

                        var foundYear: Int? = null
                        var foundMonth: Int? = null

                        for (value in rowValues) {
                            val cleanNum = parseCleanDouble(value)?.toInt()
                            if (cleanNum != null && cleanNum in 2000..2100) {
                                foundYear = cleanNum
                                break
                            }
                        }

                        for (value in rowValues) {
                            val month = parseMonth(value)
                            if (month != null) {
                                foundMonth = month
                                break
                            }
                        }

                        if (foundYear != null && foundMonth != null && numbers.size >= 3) {
                            val landRate = numbers.getOrNull(numbers.size - 3) ?: 0.0
                            val seaRate = numbers.getOrNull(numbers.size - 2) ?: 0.0
                            val dutyRate = numbers.getOrNull(numbers.size - 1) ?: 0.0

                            if (tariffs.none { it.effectiveFromYear == foundYear && it.effectiveFromMonth == foundMonth }) {
                                tariffs.add(
                                    TariffEntity(
                                        effectiveFromYear = foundYear,
                                        effectiveFromMonth = foundMonth,
                                        landHourlyRate = landRate,
                                        seaHourlyRate = seaRate,
                                        dutyDayRate = dutyRate
                                    )
                                )
                            }
                        }
                    }
                }
            }

            workbook.close()
        }

        return ImportResult(flights, duties, tariffs)
    }

    private fun parseCleanDouble(value: String): Double? {
        if (value.isBlank()) return null
        return value.trim().replace(',', '.').toDoubleOrNull()
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

    private fun parseCellToMinutes(cell: Cell?): Int {
        if (cell == null) return 0
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                val numeric = cell.numericCellValue
                if (DateUtil.isCellDateFormatted(cell)) {
                    (numeric * 24.0 * 60.0).toInt()
                } else {
                    (numeric * 60.0).toInt()
                }
            }
            CellType.STRING -> parseTimeToMinutes(cell.stringCellValue)
            CellType.FORMULA -> {
                runCatching {
                    val numeric = cell.numericCellValue
                    if (DateUtil.isCellDateFormatted(cell)) {
                        (numeric * 24.0 * 60.0).toInt()
                    } else {
                        (numeric * 60.0).toInt()
                    }
                }.getOrElse {
                    parseTimeToMinutes(cell.stringCellValue)
                }
            }
            else -> 0
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val clean = timeStr.trim().lowercase()

        if (clean.contains(":")) {
            val parts = clean.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }

        val normalizedDouble = clean.replace(',', '.')
        val doubleHours = normalizedDouble.toDoubleOrNull() ?: 0.0
        return (doubleHours * 60).toInt()
    }

    private fun parseMonth(monthStr: String): Int? {
        if (monthStr.isBlank()) return null
        val clean = monthStr.trim().lowercase()
        parseCleanDouble(clean)?.toInt()?.let {
            if (it in 1..12) return it
        }
        val months = listOf("янв", "фев", "мар", "апр", "маи", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
        val index = months.indexOfFirst { clean.contains(it) }
        return if (index >= 0) index + 1 else null
    }

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
