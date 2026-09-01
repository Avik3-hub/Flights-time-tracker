package com.example.flightlog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flight_records ORDER BY dateTimestamp DESC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    // Массовый импорт полетов
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(flights: List<FlightEntity>)

    @Update
    suspend fun updateFlight(flight: FlightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    @Query("DELETE FROM flight_records WHERE id = :id")
    suspend fun deleteFlight(id: Long)

    // Синоним для вызова из MainActivity
    @Query("DELETE FROM flight_records WHERE id = :id")
    suspend fun deleteFlightById(id: Long)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_records")
    fun getAllDuties(): Flow<List<DutyEntity>>

    @Query("SELECT * FROM duty_records")
    fun getAllDutyRecords(): Flow<List<DutyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuty(duty: DutyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuty(duty: DutyEntity)

    // Массовый импорт дежурств
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuties(duties: List<DutyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(duty: DutyEntity)
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariff_config ORDER BY effectiveFromYear DESC, effectiveFromMonth DESC")
    fun getAllTariffs(): Flow<List<TariffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: TariffEntity)

    // Массовый импорт тарифов
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariffs(tariffs: List<TariffEntity>)

    // Удаление по объекту тарифа
    @Delete
    suspend fun deleteTariff(tariff: TariffEntity)

    // Удаление по году и месяцу действия
    @Query("DELETE FROM tariff_config WHERE effectiveFromYear = :year AND effectiveFromMonth = :month")
    suspend fun deleteTariffByPeriod(year: Int, month: Int)
}
