package com.example.flightlog.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    // === ПОЛЕТЫ (flight_records) ===
    @Query("SELECT * FROM flight_records ORDER BY dateTimestamp DESC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    // === ДЕЖУРСТВА (duty_records) ===
    @Query("SELECT * FROM duty_records WHERE month = :month AND year = :year LIMIT 1")
    fun getDutyForMonth(month: Int, year: Int): Flow<DutyEntity?>

    @Query("SELECT * FROM duty_records")
    fun getAllDuties(): Flow<List<DutyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuty(duty: DutyEntity)

    // === ТАРИФЫ (tariff_config) ===
    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    fun getTariff(): Flow<TariffEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTariff(tariff: TariffEntity)
}
