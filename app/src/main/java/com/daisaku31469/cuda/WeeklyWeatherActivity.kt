package com.daisaku31469.cuda

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daisaku31469.cuda.adapter.WeeklyWeatherAdapter
import com.daisaku31469.cuda.viewmodel.MainViewModel
import com.daisaku31469.cuda.viewmodel.ViewModelFactory
import com.daisaku31469.cuda.viewmodel.WeatherViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.launch
import org.json.JSONException

class WeeklyWeatherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chart: LineChart
    private lateinit var weatherData: List<WeatherData>
    private lateinit var mainViewModel: MainViewModel
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var generatedTextObserver: Observer<String>
    private lateinit var areaCode: String
    private var isWeatherDisplayed = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_weather)

        // areaCodeを取得
        areaCode = intent.getStringExtra("areaCode") ?: ""

        recyclerView = findViewById(R.id.weeklyWeatherRecyclerView)
        chart = findViewById(R.id.precipitationChart)
        val chartPrecipitationButton = findViewById<Button>(R.id.chart_or_weekly)

        weatherViewModel = WeatherViewModel()

        mainViewModel = ViewModelProvider(this, ViewModelFactory(weatherViewModel))[MainViewModel::class.java]

        weatherData = weatherViewModel.fetchWeatherData(areaCode)

// LiveData を観察し、データが変更されたときに RecyclerView のアダプターを更新
        weatherViewModel.weatherData.observe(this) { weatherData ->
            val adapter = WeeklyWeatherAdapter(weatherData)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
        }

        // LiveDataオブジェクトを監視し、データが更新されたらUIを更新する
        generatedTextObserver = Observer { generatedText ->
            // 生成されたテキストをTextViewに表示する
            val generatedTextView: TextView = findViewById(R.id.weeklyWeatherTitleTextView)
            generatedTextView.text = generatedText
        }
        mainViewModel.generatedText.observe(this, generatedTextObserver)

        fetchDataAndGenerateText(weatherData)

        chartPrecipitationButton.setOnClickListener {
            // データを取得してテキストを生成する
            fetchDataAndGenerateText(weatherData)
            chart.visibility = View.VISIBLE
        }
    }

    private fun fetchDataAndGenerateText(weatherData: List<WeatherData>): Boolean {
        lifecycleScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isWeatherDisplayed) {
                    // 天気データを取得して表示
                    weatherViewModel.fetchWeatherData(areaCode)
                    recyclerView.visibility = View.VISIBLE
                    chart.visibility = View.GONE
                } else {
                    // 降水確率の折れ線グラフを表示
                    setupPrecipitationChart(weatherData)
                    recyclerView.visibility = View.GONE
                    chart.visibility = View.VISIBLE
                }
                isWeatherDisplayed = !isWeatherDisplayed
            }
        }
        return isWeatherDisplayed
    }

    override fun onDestroy() {
        super.onDestroy()
        // Observerの登録を解除する
        mainViewModel.generatedText.removeObserver(generatedTextObserver)
    }

    private fun setupPrecipitationChart(weatherData: List<WeatherData>) {
        val weatherToPrecipitation = weatherData.map { it.precipitation }
//        val precipitationData = PrecipitationProbabilityUtil.getWeeklyPrecipitationProbability(jmaData).second

        val entries = mutableListOf<Entry>()
        val dates = mutableListOf<String>() // グラフ上に表示する日付のリスト

        for ((index, value) in weatherToPrecipitation.withIndex()) {
            // エントリには日付 (index) と降水確率 (value) を設定します
            entries.add(Entry(index.toFloat(), value))
            // エントリに対応する日付をリストに追加
            weatherData.forEach { weather ->
                val formattedDate = getFormattedDate(weather)
                dates.add(formattedDate)
            }

        }

        val dataSet = LineDataSet(entries, "降水確率")
        dataSet.axisDependency = YAxis.AxisDependency.LEFT // 縦軸を左側に設定
        dataSet.color = Color.BLUE // ラインの色を設定
        dataSet.setCircleColor(Color.BLUE) // ポイントの色を設定

        val lineDataSets = ArrayList<ILineDataSet>()
        lineDataSets.add(dataSet)

        val lineData = LineData(lineDataSets)

        // X軸を設定
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawLabels(true)

        // Y軸を設定
        val yAxisLeft = chart.axisLeft
        yAxisLeft.axisMinimum = 0f // Y軸の最小値を0に設定
        yAxisLeft.axisMaximum = 100f // Y軸の最大値を100に設定

        xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return dates.toString()
            }
        }

        chart.data = lineData

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.invalidate()
    }

    private fun getFormattedDate(weatherData: WeatherData): String {
        try {
            val thirdArray = weatherData.date
            val stringBuilder = StringBuilder()

            // thirdArrayのサイズが4以上の場合、一週間分の日付を取得する
            if (thirdArray.length >= 4) {
                for (i in thirdArray.indices) {
                    val dateArray = thirdArray[i]
                    stringBuilder.append(dateArray)
                    stringBuilder.append(", ")
                }
            }

            // 最後のカンマとスペースを削除して返す
            if (stringBuilder.isNotEmpty()) {
                return stringBuilder.substring(0, stringBuilder.length - 2)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        // データがない場合やエラーが発生した場合は空文字列を返す
        return ""
    }
}
