package com.example.flightlog.data

import com.example.flightlog.data.export.ExportWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExportWorkbookTest {
    @Test fun generatedWorkbookCanBeReadByExistingExcelLibrary() {
        val output = ByteArrayOutputStream()
        val workbook = ExportWorkbook()
        val sheet = workbook.createSheet("Полёты")
        sheet.createRow(0).createCell(0).setCellValue("ФИО КВС")
        sheet.createRow(1).createCell(0).setCellValue("Иванов & Петров <КВС>")
        sheet.createRow(1).createCell(1).setCellValue(5964.0)
        sheet.setColumnWidth(0, 24 * 256)
        workbook.createSheet("Дежурства").createRow(0).createCell(0).setCellValue(9.0)
        workbook.write(output)
        workbook.close()
        XSSFWorkbook(output.toByteArray().inputStream()).use { restored ->
            assertEquals(2, restored.numberOfSheets)
            assertEquals("Иванов & Петров <КВС>", restored.getSheetAt(0).getRow(1).getCell(0).stringCellValue)
            assertEquals(5964.0, restored.getSheetAt(0).getRow(1).getCell(1).numericCellValue, 0.0)
            assertEquals(9.0, restored.getSheet("Дежурства").getRow(0).getCell(0).numericCellValue, 0.0)
        }
    }
}
