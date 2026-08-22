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

    @Update
    suspend fun updateFlight(flight: FlightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    @Query("DELETE FROM flight_records WHERE id = :id")
    suspend fun deleteFlight(id: Long)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_records")
    fun getAllDuties(): Flow<List<DutyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuty(duty: DutyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(duty: DutyEntity)
}

@Dao
interface TariffDao {
    // Получение истории всех тарифов
    @Query("SELECT * FROM tariff_config ORDER BY effectiveFromYear DESC, effectiveFromMonth DESC")
    fun getAllTariffs(): Flow<List<TariffEntity>>

    // Добавление или обновление тарифа
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: TariffEntity)

    // Удаление тарифа при необходимости
    @Query("DELETE FROM tariff_config WHERE id = :id")
    suspend fun deleteTariff(id: Long)
}
