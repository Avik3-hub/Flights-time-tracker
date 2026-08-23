package com.example.flightlog.data.db

import android.content.Context
import android.net.Uri
import org.dhatim.fastexcel.reader.ReadableWorkbook
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
            ReadableWorkbook(inputStream).use { workbook ->
                workbook.sheets.forEach { sheet ->
                    val sheetName = sheet.name.trim()
                    var sheetMonth = getMonthIndex(sheetName)
                    var sheetYear = 2026
                    var dutyDaysForMonth = 0

                    val rows = sheet.read()
                    for (row in rows) {
                        if (row.rowNum > 1) {
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
            }
        }

        return ImportResult(flights, duties)
    }

    private fun getCellSafe(row: org.dhatim.fastexcel.reader.Row, index: Int): String {
        return try {
            row.getCellAsString(index).orElse("").trim()
        } catch (e: Exception) {
            ""
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
