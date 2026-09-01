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

            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                val sheetName = sheet.sheetName.trim().lowercase()

                val rowIterator = sheet.rowIterator()
                if (!rowIterator.hasNext()) continue

                // Читаем заголовок для поиска колонок
                val headerRow = rowIterator.next()
                val headers = mutableMapOf<String, Int>()
                for (cell in headerRow) {
                    val text = getCellSafe(headerRow, cell.columnIndex).lowercase()
                    if (text.isNotBlank()) {
                        headers[text] = cell.columnIndex
                    }
                }

                when {
                    sheetName.contains("тариф") || sheetName.contains("rate") || sheetName.contains("ставка") || sheetName.contains("опл") -> {
                        while (rowIterator.hasNext()) {
                            val row = rowIterator.next()
                            val year = parseCleanDouble(getCellSafe(row, 0))?.toInt() 
                                ?: parseCleanDouble(getCellSafe(row, 1))?.toInt()
                            val monthStr = getCellSafe(row, 1).ifBlank { getCellSafe(row, 2) }
                            val month = parseMonth(monthStr)

                            val numbers = (0 until row.lastCellNum).mapNotNull { 
                                parseCleanDouble(getCellSafe(row, it)) 
                            }

                            if (year != null && month != null && numbers.size >= 3) {
                                val landRate = numbers.getOrNull(numbers.size - 3) ?: 0.0
                                val seaRate = numbers.getOrNull(numbers.size - 2) ?: 0.0
                                val dutyRate = numbers.getOrNull(numbers.size - 1) ?: 0.0

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
                    }

                    sheetName.contains("дежурст") || sheetName.contains("duty") -> {
                        while (rowIterator.hasNext()) {
                            val row = rowIterator.next()
                            val year = parseCleanDouble(getCellSafe(row, 0))?.toInt()
                            val month = parseMonth(getCellSafe(row, 1))
                            val days = parseCleanDouble(getCellSafe(row, 2))?.toInt()

                            if (year != null && month != null && days != null && days > 0) {
                                duties.add(
                                    DutyEntity(
                                        year = year,
                                        month = month,
                                        dutyDays = days
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        // Определяем индексы колонок полетов
                        val dateCol = findColumnIndex(headers, listOf("дат", "date")) ?: 0
                        val acCol = findColumnIndex(headers, listOf("вс", "тип", "самолет", "номер")) ?: 1
                        val missionCol = findColumnIndex(headers, listOf("задан", "№", "миссия")) ?: 2
                        val captainCol = findColumnIndex(headers, listOf("квс", "командир", "фио")) ?: 3

                        // Ищем колонки земли и моря по заголовкам
                        val landCol = findColumnIndex(headers, listOf("земл")) ?: 4
                        val seaCol = findColumnIndex(headers, listOf("мор")) ?: 5

                        while (rowIterator.hasNext()) {
                            val row = rowIterator.next()
                            try {
                                val dateStr = getCellSafe(row, dateCol)
                                if (dateStr.isBlank() || !dateStr.contains(".")) continue

                                val aircraftNum = getCellSafe(row, acCol).ifBlank { getCellSafe(row, 1) }
                                val missionNum = getCellSafe(row, missionCol).ifBlank { null }
                                val captain = getCellSafe(row, captainCol).ifBlank { getCellSafe(row, 3) }

                                // Безопасное чтение минут: берем из найденных колонок или ищем в строке число/время
                                val landMinutes = parseCellToMinutes(row.getCell(landCol))
                                val seaMinutes = parseCellToMinutes(row.getCell(seaCol))

                                val timestamp = parseDateToTimestamp(dateStr)

                                flights.add(
                                    FlightEntity(
                                        dateTimestamp = timestamp,
                                        aircraftNumber = aircraftNum.ifBlank { "б/н" },
                                        captain = captain.ifBlank { "Не указан" },
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
            }
            workbook.close()
        }

        return ImportResult(flights, duties, tariffs)
    }

    private fun findColumnIndex(headers: Map<String, Int>, keywords: List<String>): Int? {
        for ((headerText, index) in headers) {
            if (keywords.any { headerText.contains(it) }) {
                return index
            }
        }
        return null
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
