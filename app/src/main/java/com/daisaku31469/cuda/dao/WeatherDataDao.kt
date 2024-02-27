package com.daisaku31469.cuda.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daisaku31469.cuda.WeatherData

@Dao
interface WeatherDataDao {

    @Query("SELECT * FROM weather_data")
    fun getAll(): LiveData<List<WeatherData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weatherData: WeatherData)

    @Query("SELECT * FROM weather_data")
    suspend fun getAllSync(): List<WeatherData> // 同期的に全てのWeatherDataを取得するメソッド

    @Query("DELETE FROM weather_data")
    suspend fun deleteAll()
}

