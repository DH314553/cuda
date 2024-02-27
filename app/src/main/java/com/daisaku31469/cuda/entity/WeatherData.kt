package com.daisaku31469.cuda.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_data")
data class WeatherData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val datetime: String,
    val areaName: String,
    val rainfall: Float,
    val predictedWeather: String
)

