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
                // Проходим по всем листам (месяцам) файла
                workbook.sheets().forEach { sheet ->
                    val sheetName = sheet.name.trim()
                    var sheetMonth = getMonthIndex(sheetName)
                    var sheetYear = 2026

                    var dutyDaysForMonth = 0

                    sheet.openStream().use { rows ->
                        rows.forEach { row ->
                            // Пропускаем заголовок таблицы (первую строку)
                            if (row.rowNumber > 1) {
                                // Порядок колонок (0 — Дата, 1 — Борт, 2 — Земля, 3 — Море, 4 — КВС, 5 — Задание, 6 — Варандей)
                                val dateStr = row.getCellAsString(0).orElse("").trim()
                                val aircraftNum = row.getCellAsString(1).orElse("").trim()
                                val landHoursStr = row.getCellAsString(2).orElse("").trim()
                                val seaHoursStr = row.getCellAsString(3).orElse("").trim()
                                val captain = row.getCellAsString(4).orElse("").trim()
                                val missionNum = row.getCellAsString(5).orElse("").trim()
                                val dutyCellStr = row.getCellAsString(6).orElse("").trim()

                                // Считаем дни дежурств
                                val dutyDaysInRow = dutyCellStr.toIntOrNull() ?: 0
                                if (dutyDaysInRow > 0) {
                                    dutyDaysForMonth += dutyDaysInRow
                                }

                                // Если в строке есть данные по полету
                                if (dateStr.isNotBlank() && (landHoursStr.isNotBlank() || seaHoursStr.isNotBlank())) {
                                    val timestamp = parseDateToTimestamp(dateStr)
                                    val landMinutes = parseTimeToMinutes(landHoursStr)
                                    val seaMinutes = parseTimeToMinutes(seaHoursStr)

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

                                    // Автоматически определяем месяц и год по первой корректной дате
                                    if (timestamp > 0) {
                                        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                        sheetMonth = cal.get(Calendar.MONTH) + 1
                                        sheetYear = cal.get(Calendar.YEAR)
                                    }
                                }
                            }
                        }
                    }

                    // Добавляем запись дежурства за месяц, если были дни
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

    // Перевод строк вида "01:30" или "1.5" в минуты
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

    // Преобразование "23.08.2026" в миллисекунды (Timestamp)
    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // Определение номера месяца из названия листа (например, "Январь")
    private fun getMonthIndex(sheetName: String): Int {
        val months = listOf(
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
        )
        val index = months.indexOfFirst { sheetName.lowercase().contains(it) }
        return if (index >= 0) index + 1 else 0
    }
}
