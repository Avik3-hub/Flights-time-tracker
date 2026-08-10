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

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_records")
    fun getAllDuties(): Flow<List<DutyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuty(duty: DutyEntity)
}


@Dao
interface TariffDao {
    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    fun getTariff(): Flow<TariffEntity?>

    // Алиас для совместимости с вызовами getTariffFlow() в UI
    fun getTariffFlow(): Flow<TariffEntity?> = getTariff()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTariff(tariff: TariffEntity)
}
