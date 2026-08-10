package com.example.flightlog.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flight_records ORDER BY dateTimestamp DESC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    // Алиас на случай вызова insertOrUpdate для полетов
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    // Перегрузка удаления по ID (на случай, если из UI передается Long id)
    @Query("DELETE FROM flight_records WHERE id = :id")
    suspend fun deleteFlight(id: Long)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_records")
    fun getAllDuties(): Flow<List<DutyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuty(duty: DutyEntity)

    // Алиас для устранения ошибки Unresolved reference: insertOrUpdate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(duty: DutyEntity)
}

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    fun getTariff(): Flow<TariffEntity?>

    // Полноценный запрос Room вместо default-метода интерфейса
    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    fun getTariffFlow(): Flow<TariffEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTariff(tariff: TariffEntity)

    // Алиас для устранения ошибки Unresolved reference: insertOrUpdate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tariff: TariffEntity)
}
