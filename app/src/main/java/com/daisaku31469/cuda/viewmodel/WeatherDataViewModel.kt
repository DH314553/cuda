package com.daisaku31469.cuda.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daisaku31469.cuda.WeatherData
import com.daisaku31469.cuda.dao.WeatherDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherDataViewModel(private val repository: WeatherDataRepository) : WeatherViewModel() {

    val allWeatherData: LiveData<List<WeatherData>> = repository.allWeatherData

    fun insert(weatherData: WeatherData) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(weatherData)
        }
    }

    // Factory class to create WeatherDataViewModel with repository dependency
    class Factory(private val repository: WeatherDataRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherDataViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WeatherDataViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
