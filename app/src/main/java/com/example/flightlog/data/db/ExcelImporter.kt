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

                when {
                    sheetName.contains("тариф") || sheetName.contains("rate") || sheetName.contains("ставка") -> {
                        val rows = sheet.rowIterator()
                        if (rows.hasNext()) rows.next()
                        while (rows.hasNext()) {
                            val row = rows.next()
                            val year = parseCleanDouble(getCellSafe(row, 0))?.toInt()
                            val month = parseMonth(getCellSafe(row, 1))
                            val landRate = parseCleanDouble(getCellSafe(row, 2))
                            val seaRate = parseCleanDouble(getCellSafe(row, 3))
                            val dutyRate = parseCleanDouble(getCellSafe(row, 4))

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
                    }

                    sheetName.contains("дежурст") || sheetName.contains("duty") -> {
                        val rows = sheet.rowIterator()
                        if (rows.hasNext()) rows.next()
                        while (rows.hasNext()) {
                            val row = rows.next()
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
                        val rows = sheet.rowIterator()
                        if (rows.hasNext()) rows.next()
                        while (rows.hasNext()) {
                            val row = rows.next()
                            try {
                                val dateStr = getCellSafe(row, 0)
                                val aircraftNum = getCellSafe(row, 1)
                                val missionNum = getCellSafe(row, 2)
                                val captain = getCellSafe(row, 3)

                                val landCellVal = getCellSafe(row, 4)
                                val seaCellVal = getCellSafe(row, 5)

                                if (dateStr.isNotBlank()) {
                                    val timestamp = parseDateToTimestamp(dateStr)
                                    val landMinutes = parseTimeToMinutes(landCellVal, getCellNumericSafe(row.getCell(4)))
                                    val seaMinutes = parseTimeToMinutes(seaCellVal, getCellNumericSafe(row.getCell(5)))

                                    flights.add(
                                        FlightEntity(
                                            dateTimestamp = timestamp,
                                            aircraftNumber = aircraftNum.ifBlank { "б/н" },
                                            captain = captain.ifBlank { "Не указан" },
                                            missionNumber = missionNum.ifBlank { null },
                                            landTimeMinutes = landMinutes,
                                            seaTimeMinutes = seaMinutes
                                        )
                                    )
                                }
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

    private fun getCellNumericSafe(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.FORMULA -> runCatching { cell.numericCellValue }.getOrNull()
            else -> null
        }
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

    private fun parseTimeToMinutes(timeStr: String, rawNumeric: Double? = null): Int {
        if (rawNumeric != null && rawNumeric > 0.0 && !timeStr.contains(":")) {
            if (rawNumeric < 1.0) {
                return (rawNumeric * 24.0 * 60.0).toInt()
            }
        }

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

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
