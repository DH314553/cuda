package com.daisaku31469.cuda.dao

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.daisaku31469.cuda.WeatherData

class WeatherDataRepository(private val weatherDataDao: WeatherDataDao) {

    val allWeatherData: LiveData<List<WeatherData>> = weatherDataDao.getAll()

    @WorkerThread
    suspend fun insert(weatherData: WeatherData) {
        weatherDataDao.insert(weatherData)
    }

    suspend fun getAllWeatherData(): List<WeatherData> {
        return weatherDataDao.getAllSync()
    }
}

