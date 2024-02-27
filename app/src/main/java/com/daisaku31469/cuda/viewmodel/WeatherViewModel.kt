package com.daisaku31469.cuda.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daisaku31469.cuda.WeatherData
import com.daisaku31469.cuda.data.PrecipitationProbabilityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt

open class WeatherViewModel : ViewModel() {
    private val _weatherData = MutableLiveData<List<WeatherData>>()
    var weatherData: LiveData<List<WeatherData>> = _weatherData
    private val _generatedText = MutableLiveData<String>() // LiveDataオブジェクトを作成
    var generatedText: LiveData<String> = _generatedText // LiveDataオブジェクトを公開
    private val _dateFormat = MutableLiveData<String>()
    val dateFormat: LiveData<String> = _dateFormat


    open fun fetchWeatherData(areaCode: String): List<WeatherData> {
        val weather = mutableListOf<WeatherData>()
        viewModelScope.launch {
            val jmaData = getJmaData(areaCode)
            val parsedData = generateResultText(jmaData)
            _weatherData.value = parsedData.second
        }
        return weather
    }

//    fun updateWeatherData(parsedData: List<WeatherData>) {
//        _weatherData.postValue(parsedData)
//    }


    suspend fun getJmaData(areaCode: String): JSONArray? {
        return withContext(Dispatchers.IO) {
            val jmaUrl =
                "https://www.jma.go.jp/bosai/forecast/data/forecast/$areaCode.json"
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

    fun generateResultText(jmaData: JSONArray?): Pair<List<WeatherData>, List<WeatherData>> {
        val todayWeatherDataList = mutableListOf<WeatherData>()
        val weeklyWeatherDataList = mutableListOf<WeatherData>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jsonArray = jmaData ?: JSONArray()
            val tomorrow = LocalDateTime.now().plusDays(1)
            val nextWeek = LocalDateTime.now().plusWeeks(2)

            val (weatherMatrix, names, dates) = create2DArray(jmaData)
            val uniqueNames = names.toSet()

            val weeklyDates = mutableListOf<String>()
            for (i in 0 until minOf(dates.length(), 5)) {
                val dateList = dates.getJSONArray(i)
                for (j in 0 until dateList.length()) {
                    weeklyDates.add(dateList.getString(j))
                }
            }

            for (name in uniqueNames) {
                for (k in weeklyDates.indices) {
                    if (".*島.*".toRegex().matches(name)) continue
                    val formatDatetime = getDatetimeFormat(weeklyDates[k])
                    val results = Array(weatherMatrix.size) { row ->
                        FloatArray(weatherMatrix[row].size) { col -> weatherMatrix[row][col] }
                    }
                    val result = cudaRidgeDetection(results, 0.5f, jsonArray)
                    var (totalRidge, threshold) = calculateTotalRidgeAndThreshold(result)
                    val jmaRainfalls = PrecipitationProbabilityUtil.getPrecipitationProbability(jmaData)
                        .toMutableList()
                    when {
                        totalRidge >= 10 -> threshold += 0.5f
                        totalRidge >= 2 -> jmaRainfalls[k] =
                            minOf(maxOf(totalRidge * 10, 50f), 100f)
                        totalRidge.toInt() == 0 -> jmaRainfalls[k] = 0f
                        else -> jmaRainfalls[k] = minOf(totalRidge * 10f, 100f)
                    }
                    val averageRainfalls = calculateAverageRainfall(jmaRainfalls)
                    val lowTemperature = calculateAverageTemperature(jmaData).second
                    val upTemperature = calculateAverageTemperature(jmaData).third
                    var (snowPredicted, predictedWeather, snowProbability) = predictWeather(
                        lowTemperature, upTemperature, averageRainfalls,
                        totalRidge, jmaRainfalls[k], getWinds(jmaData)
                    )
                    if (snowProbability > 0.1) snowProbability += 0.2f
                    else if (snowPredicted) snowProbability += 0.2f
                    val weatherData =
                        WeatherData(formatDatetime, name, jmaRainfalls[k], predictedWeather)
                    val dateTime = formatDatetime.replace(Regex(" .*曜日"), "")
                    val dateLocalDateTime = LocalDateTime.parse(
                        dateTime,
                        DateTimeFormatter.ofPattern("yyyy年M月d日H時m分")
                    )
                    if (dateLocalDateTime.isAfter(tomorrow) &&
                        !dateLocalDateTime.isBefore(tomorrow) &&
                        dateLocalDateTime.isBefore(nextWeek) &&
                        dateLocalDateTime.toLocalTime() == LocalTime.MIDNIGHT) {
                        weeklyWeatherDataList.add(weatherData)
                    } else {
                        todayWeatherDataList.add(weatherData)
                    }
                }
            }
        }
        // LiveDataオブジェクトにデータをセット
        _weatherData.postValue(weeklyWeatherDataList)
        return Pair(todayWeatherDataList, weeklyWeatherDataList)
    }

    fun create2DArray(jsonData: JSONArray?): Triple<Array<FloatArray>, MutableList<String>, JSONArray> {
        val names = mutableListOf<String>()
        val weatherCodes = mutableListOf<JSONArray>()
        val dates = mutableListOf<JSONArray>()

        // データが提供されている場合のみ処理を実行
        if (jsonData != null) {
            // 各天気観測所のデータにアクセス
            for (i in 0 until jsonData.length()) {
                val weatherStation = jsonData.getJSONObject(i)
                val timeSeriesArray = weatherStation.getJSONArray("timeSeries")
                for (timeSeriesIndex in 0 until timeSeriesArray.length()) {
                    val timeSeriesObject = timeSeriesArray.getJSONObject(timeSeriesIndex)
                    val timeDefinesArray = timeSeriesObject.getJSONArray("timeDefines")
                    dates.add(timeDefinesArray)
                    val areasArray = timeSeriesObject.getJSONArray("areas")
                    processAreas(areasArray, names, weatherCodes)
                }
            }
        }

        // 最大の地域数を取得し、weatherCodesをパディングする
        val maxAreaCount = weatherCodes.maxOfOrNull { it.length() } ?: 0
        val paddedWeatherCodes = padWeatherCodes(weatherCodes, maxAreaCount)

        // 2D配列を生成して結果を返す
        val result = Array(paddedWeatherCodes.size) { i ->
            FloatArray(maxAreaCount) { j ->
                paddedWeatherCodes[i].getDouble(j).toFloat()
            }
        }

        return Triple(result, names, JSONArray(dates))
    }

    private fun processAreas(areasArray: JSONArray, names: MutableList<String>, weatherCodes: MutableList<JSONArray>) {
        // 各地域のデータを処理
        for (areaIndex in 0 until areasArray.length()) {
            val areaObject = areasArray.getJSONObject(areaIndex)
            val name = areaObject.getJSONObject("area").getString("name")
            val areaWeatherCodes = areaObject.optJSONArray("weatherCodes") ?: JSONArray() // nullの場合は空のJSONArrayを使用
            names.add(name)
            weatherCodes.add(areaWeatherCodes)
        }
    }

    private fun padWeatherCodes(weatherCodes: List<JSONArray>, maxAreaCount: Int): List<JSONArray> {
        return weatherCodes.map { areaCodes ->
            val newArray = JSONArray()
            for (i in 0 until maxAreaCount) {
                if (i < areaCodes.length()) newArray.put(areaCodes.getInt(i))
                else newArray.put(-1) // Padding with -1
            }
            newArray
        }
    }

    private fun calculateTotalRidgeAndThreshold(results: Array<FloatArray>): Pair<Float, Float> {
        var totalRidge = 0f
        var dataSize = 0

        // 全てのデータを合計
        for (data in results) {
            for (value in data) {
                totalRidge += value
                dataSize++
            }
        }

        // 平均値を計算
        val meanValue = totalRidge / dataSize

        // 分散を計算
        var variance = 0f
        for (data in results) {
            for (value in data) {
                variance += (value - meanValue).pow(2)
            }
        }
        variance /= dataSize

        // 標準偏差を計算
        val stdDeviation = sqrt(variance)

        // 閾値を計算
        val threshold = (meanValue + 2 * stdDeviation) / 10.0.pow(34.0) // 平均値 + 2倍の標準偏差 / 10の34乗で少数点数にする

        return Pair(totalRidge, threshold.toFloat())
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun getDatetimeFormat(date: String): String {
        try {
            val datetimeString = date.substringBefore("+").removePrefix("[\"")
            val inputDatetime = LocalDateTime.parse(datetimeString)

            // 曜日を取得
            val weekly = inputDatetime.dayOfWeek

            val year = inputDatetime.year.toString()
            val month = inputDatetime.month.value.toString()
            val day = inputDatetime.dayOfMonth.toString()
            val hour = inputDatetime.hour.toString()
            val minute = inputDatetime.minute.toString()
            // 日付をフォーマット
            var formattedDatetime = "${year}年${month}月${day}日${hour}時${minute}分"

            // 曜日を文字列に追加
            formattedDatetime += when (weekly) {
                DayOfWeek.MONDAY -> " 月曜日"
                DayOfWeek.TUESDAY -> " 火曜日"
                DayOfWeek.WEDNESDAY -> " 水曜日"
                DayOfWeek.THURSDAY -> " 木曜日"
                DayOfWeek.FRIDAY -> " 金曜日"
                DayOfWeek.SATURDAY -> " 土曜日"
                else -> " 日曜日"
            }
            return formattedDatetime
        } catch (e: Exception) {
            e.printStackTrace()
            return "日付が無効です"
        }
    }

    private fun calculateAverageRainfall(jmaRainfalls: MutableList<Float>): Float? {
        val pops = mutableListOf<Float>()
        if (jmaRainfalls.isNotEmpty()) {
            for (entry in jmaRainfalls) {
                try {
                    // 各要素を数値に変換してリストに追加
                    pops.add(entry)
                } catch (e: NumberFormatException) {
                    return null // 数値に変換できない場合はnullを返す
                }
            }
        } else {
            return null // 空のリストの場合はnullを返す
        }
        // 空文字列や空の要素を取り除く
        val filteredPops = pops.toList()
        // リストに要素があれば平均を計算
        return if (filteredPops.isNotEmpty()) {
            filteredPops.average().toFloat()
        } else {
            null // 空のリストの場合はnullを返す
        }
    }

    private fun removeZeros(list: MutableList<String>): MutableList<String> {
        return list.filter { it != "0" }.toMutableList()
    }

    private fun calculateAverageTemperature(data: JSONArray?): Triple<MutableList<Float>, MutableList<Float>, MutableList<Float>> {
        val temperatures = mutableListOf<String>()
        var lowerTemperatures = mutableListOf<String>()
        var upperTemperatures = mutableListOf<String>()

        // データが提供されている場合のみ処理を実行
        if (data != null) {
            // 各気象観測所のデータにアクセス
            for (i in 0 until data.length()) {
                val weatherStation = data.getJSONObject(i)
                val timeSeriesArray = weatherStation.getJSONArray("timeSeries")

                // 時系列データに含まれる各エントリを処理
                for (j in 0 until timeSeriesArray.length()) {
                    val timeSeriesObject = timeSeriesArray.getJSONObject(j)
                    val areasArray = timeSeriesObject.getJSONArray("areas")

                    // 各地域のデータを処理
                    for (l in 0 until areasArray.length()) {
                        val area = areasArray.getJSONObject(l)
                        if (area.has("temps")) {
                            val tempsArray = area.getJSONArray("temps")
                            for (index in 0 until tempsArray.length()) {
                                temperatures.add(tempsArray.getString(index))
                            }
                        } else {
                            // 温度データが欠落している場合は、NaN を追加してリストを補完する
                            temperatures.add("0")
                        }
                        if (area.has("tempsMin") && area.has("tempsMax")) {
                            val lowTemperaturesArray = area.getJSONArray("tempsMin")
                            val upTemperaturesArray = area.getJSONArray("tempsMax")
                            // JSONArray から値を取得し、適切な型に変換してリストに追加する
                            for (index in 0 until lowTemperaturesArray.length()) {
                                lowerTemperatures.add(lowTemperaturesArray.getString(index))
                            }
                            for (index in 0 until upTemperaturesArray.length()) {
                                upperTemperatures.add(upTemperaturesArray.getString(index))
                            }
                        } else {
                            // 温度データが欠落している場合は、NaN を追加してリストを補完する
                            lowerTemperatures.add("0")
                            upperTemperatures.add("0")
                        }
                    }
                }
            }
            // ゼロを排除したリストを作成
            lowerTemperatures = removeZeros(lowerTemperatures)
            upperTemperatures = removeZeros(upperTemperatures)
        }

        // 空文字列や空の要素を取り除く
        val tempBasic = temperatures.toList()
        val lowTemperatures = lowerTemperatures.toList()
        val upTemperatures = upperTemperatures.toList()

        // 各温度を対応するリストの先頭に挿入
        val result = mutableListOf<Float>()
        val lowResult = mutableListOf<Float>()
        val upResult = mutableListOf<Float>()
        for (i in tempBasic.indices) {
            result.add(tempBasic[i].toFloat())
        }
        for (j in lowerTemperatures.indices) {
            if (lowTemperatures[j] == "") {
                lowResult.add("0".toFloat())
            } else {
                lowResult.add(lowTemperatures[j].toFloat())
            }
        }
        for (k in upTemperatures.indices) {
            if (upTemperatures[k] == "") {
                upResult.add("0".toFloat())
            } else {
                upResult.add(upTemperatures[k].toFloat())
            }
        }
        return Triple(result, lowResult, upResult)
    }

    // LiveData の値を取得して天気予測を適用したリッジ検出の結果を返す
    private fun cudaRidgeDetection(
        data: Array<FloatArray>,
        thres: Float,
        jmaData: JSONArray
    ): Array<FloatArray> {
        val rows = data.size
        val cols = data[0].size
        val count = Array(rows) { FloatArray(cols) { 0f } } // 初期化時に 0 を使用する

        // ループを 1 から始めて、境界チェックを追加
        for (i in 1 until rows - 1) {
            for (j in 1 until cols - 1) {
                // 条件を修正しNaNのチェックを追加
                if (data[i][j].isFinite() && data[i][j] > thres) {
                    var stepI = i
                    var stepJ = j
                    var loopCount = 0 // ループ回数のカウンターを追加
                    while (loopCount < 1000) { // 最大で1000回のループ
                        // 境界チェックを追加
                        if (stepI <= 0 || stepJ <= 0 || stepI >= rows - 1 || stepJ >= cols - 1) {
                            break
                        }
                        var index = 4
                        var vmax = Float.NEGATIVE_INFINITY
                        for (ii in 0 until 3) {
                            for (jj in 0 until 3) {
                                val value = data[stepI + ii - 1][stepJ + jj - 1]
                                if (value > vmax) {
                                    vmax = value
                                    index = jj + 3 * ii
                                }
                            }
                        }
                        if (index == 4 || vmax == data[stepI][stepJ] || vmax.isNaN()) {
                            break
                        }
                        val row = index / 3
                        val col = index % 3
                        count[stepI - 1 + row][stepJ - 1 + col]++
                        stepI += row - 1
                        stepJ += col - 1
                        loopCount++ // ループ回数をインクリメント
                    }
                }
            }
        }
        return applyWeatherForecast(count, jmaData)
    }

    // 天気予測に基づいたリッジ検出の結果を返す
    private fun applyWeatherForecast(
        count: Array<FloatArray>,
        jsonData: JSONArray
    ): Array<FloatArray> {
        // 天気予測を適用した結果の配列を初期化
        val predictedCount = Array(count.size) { FloatArray(count[0].size) }

        // 天気予測に基づいてリッジ検出の結果を調整
        val forecastMap = getWeatherForecast(jsonData)
        for (i in count.indices) {
            for (j in count[i].indices) {
                // リッジ検出の結果を調整する処理
                val weather = forecastMap.toString()
                predictedCount[i][j] = when {
                    weather.contains("雨") -> count[i][j] * 2f
                    weather.contains(Regex(".*曇り.*.*雨.*")) -> count[i][j] * 1.5f
                    weather.contains(Regex(".*晴れ.*.*曇り.*")) -> count[i][j] * 0.5f
                    weather.contains("曇り") -> count[i][j] * 1f
                    weather.contains("晴れ") -> count[i][j] * 0f
                    else -> count[i][j] // その他の天気の場合は変更なし
                }
            }
        }

        return predictedCount
    }

    // 天気予報を取得してマップとして返す
    private fun getWeatherForecast(jsonData: JSONArray): List<String> {
        val forecastList = mutableListOf<String>()

        for (i in 0 until jsonData.length()) {
            val weatherStation = jsonData.getJSONObject(i)
            val timeSeriesArray = weatherStation.getJSONArray("timeSeries")
            for (j in 0 until timeSeriesArray.length()) {
                val timeSeriesObject = timeSeriesArray.getJSONObject(j)
                val areas = timeSeriesObject.getJSONArray("areas")
                if (j == 1) {
                    return forecastList
                } else {
                    val weather = areas.getJSONObject(0).getJSONArray("weathers").getString(0)
                    forecastList.add(weather)
                }
            }
        }
        return forecastList
    }


    private fun getWinds(data: JSONArray?): Int? {
        val winds = mutableListOf<String>()
        if (data != null) {
            for (i in 0 until data.length()) {
                val weatherStation = data.getJSONObject(i)
                val timeSeriesArray = weatherStation.getJSONArray("timeSeries")
                for (j in 0 until timeSeriesArray.length()) {
                    val timeSeriesObject = timeSeriesArray.getJSONObject(j)
                    val timeDefinesArray = timeSeriesObject.getJSONArray("timeDefines")
                    // 各時間定義のデータを処理
                    for (k in 0 until timeDefinesArray.length()) {
                        val areasArray = timeSeriesObject.getJSONArray("areas")
                        for (l in 0 until areasArray.length()) {
                            val windsObject = areasArray.getJSONObject(l)
                            if (windsObject.has("waves")) {
                                val windsArray = windsObject.getJSONArray("waves")
                                winds.add(windsArray.toString())
                            } else {
                                winds.add("0")
                            }
                        }
                    }
                }
            }
        }

        if (winds.isNotEmpty()) {
            val windSpeeds = mutableListOf<String>()
            for (wind in winds) {
                val windValues = Regex("""\d+""").findAll(wind).map { it.value }.toMutableList()
                windSpeeds.addAll(windValues)
            }
            if (windSpeeds.isNotEmpty()) {
                val windResultData = removeZeros(windSpeeds)
                return windResultData.toList().maxOfOrNull { it.toInt() }
            }
        }
        return null
    }

    private fun predictWeather(
        lowAverageTemperature: MutableList<Float>,
        upAverageTemperature: MutableList<Float>,
        averageRainfall: Float?,
        totalRidge: Float,
        precipitationProbability: Float,
        averageWinds: Int?
    ): Triple<Boolean, String, Float> {
        var snowProbability = precipitationProbability // 最後の値を取得

        // 平均気温の閾値を調整
        snowProbability += if ((lowAverageTemperature.lastOrNull() ?: 0.0f) <= -2.0f) {
            0.3f  // 平均気温が-2.0度以下の場合、雪の確率を増加
        } else {
            0.0f
        }

        // 降水量が影響する条件を調整
        if (averageWinds != null) {
            if (averageRainfall != null && averageRainfall >= 5.0f && averageWinds >= 4) {
                snowProbability += 0.2f  // 平均降水量が5.0mm以上の場合、雪の確率を増加
            }
        }

        // 雪の予測ロジックを調整
        val snowPredicted = (
                (
                        (lowAverageTemperature.lastOrNull() ?: 0.0f) < 0.0f &&
                                (upAverageTemperature.lastOrNull() ?: 0.0f) <= 5.0f &&
                                precipitationProbability >= 10.0f &&
                                snowProbability in 10.0f..30.0f
                        )
                )

        // 天気予測のロジック
        val predictedWeather: String = if (snowPredicted) {
            "雪"
        } else if (totalRidge == 0.0f || precipitationProbability <= 20.0f) {
            snowProbability = 0.0f
            "晴れ"
        } else if (totalRidge == 1.0f || precipitationProbability in 20.0f..40.0f) {
            snowProbability = 0.0f
            "曇り"
        } else if (totalRidge == 2.0f || precipitationProbability >= 50.0f) {
            snowProbability = 0.0f
            "雨"
        } else {
            "不明"
        }

        // 修正：snowProbabilityも返す
        return Triple(snowPredicted, predictedWeather, snowProbability)
    }
}