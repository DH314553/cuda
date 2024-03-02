package com.daisaku31469.cuda

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.launch

class WeeklyWeatherActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chart: LineChart
    private lateinit var mainViewModel: MainViewModel
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var weatherData: List<WeatherData>
    private lateinit var generatedTextObserver: Observer<String>
    private lateinit var selectedAreaCode: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_weather)

        // areaCodeを取得
        selectedAreaCode = intent.getStringExtra("areaCode") ?: ""

        recyclerView = findViewById(R.id.weeklyWeatherRecyclerView)
        chart = findViewById(R.id.precipitationChart)
        val chartPrecipitationButton = findViewById<Button>(R.id.chart_or_weekly)
        val regionSpecificButton = findViewById<Button>(R.id.regionSpecificButton)

        weatherViewModel = WeatherViewModel()

        mainViewModel = ViewModelProvider(this, ViewModelFactory(weatherViewModel))[MainViewModel::class.java]

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

        chartPrecipitationButton.setOnClickListener {
            weatherViewModel.viewModelScope.launch {
                if (chart.visibility == View.VISIBLE && regionSpecificButton.visibility == View.VISIBLE) {
                    chart.visibility = View.GONE
                    weatherData = weatherViewModel.fetchWeatherDataWeekly(selectedAreaCode) ?: return@launch
                    fetchDataAndGenerateText(selectedAreaCode, weatherData)
                } else {
                    // データを取得してテキストを生成する
                    regionSpecificButton.visibility = View.VISIBLE
                    chart.visibility = View.VISIBLE
                    weatherData = weatherViewModel.fetchWeatherDataWeekly(selectedAreaCode) ?: return@launch
                    fetchDataAndGenerateText(selectedAreaCode, weatherData)
                }
            }
        }

        // 地域別表示ボタンがクリックされたときの処理
        regionSpecificButton.setOnClickListener {
            // 地域別のデータを取得して表示する処理を実装
            fetchRegionSpecificData()
        }
    }

    private fun fetchRegionSpecificData() {
        weatherViewModel.viewModelScope.launch {
            // 地域別のデータを取得
            val regionSpecificData = weatherViewModel.fetchWeatherDataWeekly(selectedAreaCode) ?: return@launch

            // 取得したデータがnullでない場合は、RecyclerView にデータをセットして表示する
            regionSpecificData.let { data ->
                val adapter = WeeklyWeatherAdapter(data)
                recyclerView.layoutManager = LinearLayoutManager(this@WeeklyWeatherActivity)
                recyclerView.adapter = adapter
                recyclerView.visibility = View.VISIBLE
                chart.visibility = View.GONE

                // グラフのセットアップ
                setupPrecipitationChart(data)

                // ボタンのセットアップ
                setupLists(data)
            }
        }
    }

    private fun setupLists(weatherData: List<WeatherData>?) {
        val uniqueAreaNames = weatherData?.map { it.areaName }?.distinct() ?: return

        val builder = AlertDialog.Builder(this)
        builder.setTitle("地域を選択してください")

        val areaNamesArray = uniqueAreaNames.toTypedArray()
        builder.setItems(areaNamesArray) { _, which ->
            val selectedAreaName = areaNamesArray[which]
            weatherViewModel.viewModelScope.launch {
                fetchDataAndGenerateText(selectedAreaName, weatherData)
            }
        }

        val dialog = builder.create()
        dialog.show()
    }


    private fun fetchDataAndGenerateText(areaName: String, weatherData: List<WeatherData>?) {
        val areaWeatherData = weatherData?.filter { it.areaName == areaName }

        if (areaWeatherData!!.isNotEmpty()) {
            setupPrecipitationChart(areaWeatherData)
            recyclerView.adapter = WeeklyWeatherAdapter(areaWeatherData)
            recyclerView.visibility = View.VISIBLE
            chart.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            chart.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Observerの登録を解除する
        mainViewModel.generatedText.removeObserver(generatedTextObserver)
    }

    private fun setupPrecipitationChart(weatherData: List<WeatherData>?) {
        val entries = mutableListOf<Entry>()
        val dates = mutableListOf<String>()

        // 降水確率と日付を取得してエントリと日付リストに追加
        weatherData?.forEachIndexed { index, data ->
            entries.add(Entry(index.toFloat(), data.precipitation))
            // 日付をMM/DD形式にフォーマットして追加
            val formattedDate = formatDateString(data.date)
            dates.add(formattedDate)
        }

        val dataSet = LineDataSet(entries, "降水確率")
        dataSet.axisDependency = YAxis.AxisDependency.LEFT
        dataSet.color = Color.BLUE
        dataSet.setCircleColor(Color.BLUE)

        val lineDataSets = ArrayList<ILineDataSet>()
        lineDataSets.add(dataSet)

        val lineData = LineData(lineDataSets)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawLabels(true)

        // X軸の値フォーマットを設定
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // インデックスが日付リストの範囲内にある場合、その日付を返す
                val index = value.toInt()
                val datesList = dates.distinct()
                return if (index in datesList.indices) {
                    datesList[index]
                } else {
                    ""
                }
            }
        }

        val yAxisLeft = chart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f

        chart.data = lineData

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.invalidate()
    }

    // 日付をMM/DD形式にフォーマットする関数
    private fun formatDateString(date: String): String {
        // 正規表現を使用して年、月、日を抽出する
        val regex = Regex("""(\d+)年(\d+)月(\d+)日.*""")
        val matchResult = regex.find(date)
        // マッチした場合は年、月、日を取得してフォーマットする
        return if (matchResult != null && matchResult.groupValues.size >= 4) {
            val month = matchResult.groupValues[2].toInt()
            val day = matchResult.groupValues[3].toInt()
            String.format("%2d/%2d", month, day)
        } else {
            // マッチしない場合は空文字列を返すか、エラー処理を行う
            ""
        }
    }
}
