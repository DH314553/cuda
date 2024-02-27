package com.daisaku31469.cuda.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.daisaku31469.cuda.entity.WeatherData

@Database(entities = [WeatherData::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDataDao(): WeatherDataDao
}
