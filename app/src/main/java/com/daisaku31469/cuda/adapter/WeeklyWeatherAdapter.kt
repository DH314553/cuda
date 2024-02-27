package com.daisaku31469.cuda.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.daisaku31469.cuda.R
import com.daisaku31469.cuda.WeatherData
import okio.utf8Size

class WeeklyWeatherAdapter(private val weatherDataList: List<WeatherData>) : RecyclerView.Adapter<WeeklyWeatherAdapter.WeatherViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_weather_data, parent, false)
        return WeatherViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val currentItem = weatherDataList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = weatherDataList.size

    inner class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val areaTextView: TextView = itemView.findViewById(R.id.areaTextView)
        private val temperatureTextView: TextView = itemView.findViewById(R.id.temperatureTextView)
        private val predictedWeatherTextView: TextView = itemView.findViewById(R.id.predictedWeatherTextView)

        fun bind(weatherData: WeatherData) {
            dateTextView.textSize = 20.0f
            areaTextView.textSize = 30.0f
            temperatureTextView.textSize = 30.0f
            predictedWeatherTextView.textSize = 30.0f
            weatherData.date.also { dateTextView.text = it }
            "地域名　${weatherData.areaName}".also { areaTextView.text = it }
            "降水確率　${weatherData.precipitation.toInt()}%".also { temperatureTextView.text = it }
            "天気予測　${weatherData.predictedWeather}".also { predictedWeatherTextView.text = it }
        }
    }
}
