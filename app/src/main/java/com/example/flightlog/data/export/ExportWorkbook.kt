package com.example.flightlog.data.export

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/** Writes the existing report layout without POI's reflective OOXML machinery. */
internal class ExportWorkbook {
    private val buffer = ByteArrayOutputStream()
    private val workbook = Workbook(buffer, "FlightLog", "2.0.2")

    fun createSheet(name: String) = ExportSheet(workbook.newWorksheet(name))

    fun write(output: OutputStream) {
        workbook.finish()
        buffer.writeTo(output)
    }

    fun close() { workbook.close() }
}

internal class ExportSheet(private val sheet: Worksheet) {
    fun createRow(index: Int) = ExportRow(sheet, index)
    fun setColumnWidth(column: Int, width: Int) { sheet.width(column, width / 256.0) }
}

internal class ExportRow(private val sheet: Worksheet, private val row: Int) {
    fun createCell(column: Int) = ExportCell(sheet, row, column)
}

internal class ExportCell(private val sheet: Worksheet, private val row: Int, private val column: Int) {
    fun setCellValue(value: String) { sheet.value(row, column, value) }
    fun setCellValue(value: Double) { sheet.value(row, column, value) }
}
