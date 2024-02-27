package com.daisaku31469.cuda.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.daisaku31469.cuda.WeatherData
import kotlinx.coroutines.launch


class MainViewModel(private val repository: WeatherViewModel) : WeatherViewModel() {

    private val _formattedWeatherData = MutableLiveData<List<WeatherData>>()
    val formattedWeatherData: LiveData<List<WeatherData>> = _formattedWeatherData

    override fun fetchWeatherData(areaCode: String): List<WeatherData> {
        var weatherList = mutableListOf<WeatherData>()
        viewModelScope.launch {
            val jmaData = getJmaData(areaCode)
            val parsedData = repository.generateResultText(jmaData)
            _formattedWeatherData.value = formatWeatherData(parsedData.first)
            weatherList = _formattedWeatherData.value as MutableList<WeatherData>

        }
        return weatherList
    }


    private fun formatWeatherData(weatherData: List<WeatherData>): List<WeatherData> {
        val formattedWeatherData = mutableListOf<WeatherData>()
        weatherData.forEach { data ->
            formattedWeatherData.add(WeatherData(data.date, data.areaName, data.precipitation, data.predictedWeather))
        }
        return formattedWeatherData
    }
}


