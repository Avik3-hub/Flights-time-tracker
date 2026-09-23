package com.example.flightlog.data

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import com.example.flightlog.data.db.*
import com.example.flightlog.data.export.ExcelExporter
import com.example.flightlog.data.export.ExcelImporter
import com.example.flightlog.ui.displayAircraftNumber
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FlightUpgradeTest {
    @Test fun migrationPreservesAllRecordsAndAllowsEditingType() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "upgrade-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE flight_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateTimestamp INTEGER NOT NULL, aircraftNumber TEXT NOT NULL, captain TEXT NOT NULL, missionNumber TEXT, landTimeMinutes INTEGER NOT NULL, seaTimeMinutes INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE duty_records (year INTEGER NOT NULL, month INTEGER NOT NULL, dutyDays INTEGER NOT NULL, PRIMARY KEY(year, month))")
            db.execSQL("CREATE TABLE tariff_config (effectiveFromYear INTEGER NOT NULL, effectiveFromMonth INTEGER NOT NULL, landHourlyRate REAL NOT NULL, seaHourlyRate REAL NOT NULL, dutyDayRate REAL NOT NULL, PRIMARY KEY(effectiveFromYear, effectiveFromMonth))")
            db.execSQL("INSERT INTO flight_records VALUES (7, 1790035200000, '22468', 'Иванов', '192', 0, 200)")
            db.execSQL("INSERT INTO duty_records VALUES (2026, 9, 9)")
            db.execSQL("INSERT INTO tariff_config VALUES (2026, 9, 1180, 5964, 1952)")
            db.version = 5
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_5_6).allowMainThreadQueries().build()
        try {
            val old = db.flightDao().getAllFlights().first().single()
            assertEquals(7L, old.id)
            assertEquals(200, old.seaTimeMinutes)
            assertEquals("192", old.missionNumber)
            assertEquals("Пассажирский", old.flightType)
            assertEquals(9, db.dutyDao().getAllDuties().first().single().dutyDays)
            assertEquals(5964.0, db.tariffDao().getAllTariffs().first().single().seaHourlyRate, 0.0)
            db.flightDao().updateFlight(old.copy(flightType = "Санитарный"))
            assertEquals("Санитарный", db.flightDao().getAllFlights().first().single().flightType)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun excelRoundTripKeepsTypeAndDoesNotModifySource() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "roundtrip.xlsx")
        val flight = FlightEntity(dateTimestamp = 1790035200000, aircraftNumber = "22468",
            captain = "Иванов", missionNumber = "192", landTimeMinutes = 60, seaTimeMinutes = 20,
            flightType = "Санитарный")
        assertTrue(ExcelExporter.exportToExcel(context, Uri.fromFile(file), listOf(flight), emptyList(), null))
        val before = file.readBytes()
        val result = ExcelImporter.importFromExcel(context, Uri.fromFile(file)).flights.single()
        assertEquals(flight.flightType, result.flightType)
        assertEquals(flight.missionNumber, result.missionNumber)
        assertEquals(flight.landTimeMinutes, result.landTimeMinutes)
        assertEquals(flight.seaTimeMinutes, result.seaTimeMinutes)
        assertArrayEquals(before, file.readBytes())
    }

    @Test fun legacyExcelUsesPassengerDefault() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "legacy.xlsx")
        XSSFWorkbook().use { book ->
            val sheet = book.createSheet("Полёты")
            sheet.createRow(0).createCell(0).setCellValue("Дата")
            val row = sheet.createRow(1)
            listOf("22.09.2026", "22468", "192", "Иванов", "0:00", "3:20")
                .forEachIndexed { index, value -> row.createCell(index).setCellValue(value) }
            file.outputStream().use { book.write(it) }
        }
        assertEquals("Пассажирский",
            ExcelImporter.importFromExcel(context, Uri.fromFile(file)).flights.single().flightType)
    }

    @Test fun aircraftPrefixIsNotDuplicated() {
        assertEquals("RA-22468", displayAircraftNumber("22468"))
        assertEquals("RA-22468", displayAircraftNumber(" RA-22468 "))
        assertEquals("RA-22468", displayAircraftNumber("ра 22468"))
        assertEquals("RF-12345", displayAircraftNumber("RF-12345"))
        assertEquals("—", displayAircraftNumber(""))
    }
}
