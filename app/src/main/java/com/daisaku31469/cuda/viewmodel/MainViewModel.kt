package com.daisaku31469.cuda.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.daisaku31469.cuda.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL


class MainViewModel(private val repository: WeatherViewModel) : WeatherViewModel() {

    private val _formattedWeatherData = MutableLiveData<List<WeatherData>>()
    val formattedWeatherData: LiveData<List<WeatherData>> = _formattedWeatherData

    fun fetchWeatherDataToday(areaCode: String): MutableList<WeatherData> {
        var weatherList = mutableListOf<WeatherData>()
        viewModelScope.launch {
            val jmaData = getJmaData(areaCode)
            val parsedData = repository.generateResultText(jmaData)
            _formattedWeatherData.value = formatWeatherData(parsedData.first)
            weatherList = _formattedWeatherData.value as MutableList<WeatherData>

        }
        return weatherList
    }

    override suspend fun getJmaData(areaCode: String): JSONArray? {
        return withContext(Dispatchers.IO) {
            val jmaUrl = "https://www.jma.go.jp/bosai/forecast/data/forecast/$areaCode.json"
            try {
                val url = URL(jmaUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
                    JSONArray(responseStream)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    private fun formatWeatherData(weatherData: List<WeatherData>): List<WeatherData> {
        val formattedWeatherData = mutableListOf<WeatherData>()
        weatherData.forEach { data ->
            formattedWeatherData.add(WeatherData(data.date, data.areaName, data.precipitation, data.predictedWeather))
        }
        return formattedWeatherData
    }
}


