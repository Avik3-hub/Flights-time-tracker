package com.example.flightlog.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FlightEntity::class,
        DutyEntity::class,
        TariffEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun dutyDao(): DutyDao
    abstract fun tariffDao(): TariffDao
}
