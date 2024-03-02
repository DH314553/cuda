package com.daisaku31469.cuda

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.ListView
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.daisaku31469.cuda.viewmodel.MainViewModel
import com.daisaku31469.cuda.viewmodel.ViewModelFactory
import com.daisaku31469.cuda.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray

data class WeatherData(
    val date: String,
    val areaName: String,
    val precipitation: Float,
    val predictedWeather: String
)

class MainActivity : AppCompatActivity() {

    private val areaDic = mapOf(
        "北海道/釧路" to "014100",
        "北海道/旭川" to "012000",
        "北海道/札幌" to "016000",
        "青森県" to "020000",
        "岩手県" to "030000",
        "宮城県" to "040000",
        "秋田県" to "050000",
        "山形県" to "060000",
        "福島県" to "070000",
        "茨城県" to "080000",
        "栃木県" to "090000",
        "群馬県" to "100000",
        "埼玉県" to "110000",
        "千葉県" to "120000",
        "東京都" to "130000",
        "神奈川県" to "140000",
        "新潟県" to "150000",
        "富山県" to "160000",
        "石川県" to "170000",
        "福井県" to "180000",
        "山梨県" to "190000",
        "長野県" to "200000",
        "岐阜県" to "210000",
        "静岡県" to "220000",
        "愛知県" to "230000",
        "三重県" to "240000",
        "滋賀県" to "250000",
        "京都府" to "260000",
        "大阪府" to "270000",
        "兵庫県" to "280000",
        "奈良県" to "290000",
        "和歌山県" to "300000",
        "鳥取県" to "310000",
        "島根県" to "320000",
        "岡山県" to "330000",
        "広島県" to "340000",
        "山口県" to "350000",
        "徳島県" to "360000",
        "香川県" to "370000",
        "愛媛県" to "380000",
        "高知県" to "390000",
        "福岡県" to "400000",
        "佐賀県" to "410000",
        "長崎県" to "420000",
        "熊本県" to "430000",
        "大分県" to "440000",
        "宮崎県" to "450000",
        "鹿児島県" to "460100",
        "沖縄県/那覇" to "471000",
        "沖縄県/石垣" to "474000"
    )

    private lateinit var selectedDateTimeTextView: TextView
    private lateinit var viewModel: WeatherViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var showWeatherButton: Button
    private lateinit var showDateTimeButton: Button
    private lateinit var weatherResultTextView: TextView
    private lateinit var showListViewButton: Button
    private lateinit var showWeeklyWeatherButton: Button
    private lateinit var jmaWeatherData: JSONArray
    private var selectedItem = ""

    companion object {
        var selectedAreaCode = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ViewModelの初期化
        initViewModel()

        // UI要素の初期化
        initUI()

        // UIのセットアップ
        setupUI()
    }

    private fun initUI() {
        selectedDateTimeTextView = findViewById(R.id.selectedDateTimeTextView)
        showWeatherButton = findViewById(R.id.showWeatherButton)
        showDateTimeButton = findViewById(R.id.showDateTimePickerButton) // 追加
        weatherResultTextView = findViewById(R.id.weatherResultTextView)
        showListViewButton = findViewById(R.id.showListViewButton)
        showWeeklyWeatherButton = findViewById(R.id.showWeeklyWeatherButton)

        // 日付と時間ピッカーを表示するボタンがクリックされたときのリスナーを設定
        showDateTimeButton.setOnClickListener {
            // 日付と時間ピッカーを表示
            showDateTimePickers()
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        mainViewModel = ViewModelProvider(this, ViewModelFactory(viewModel))[MainViewModel::class.java]
    }

    private fun setupUI() {
        // 日時が選択されたときにテキストビューを更新
        val selectedDateTimeObserver = Observer<String> { selectedDateTime ->
            "選択された日時: $selectedDateTime".also { selectedDateTimeTextView.text = it }
        }
        mainViewModel.dateFormat.observe(this, selectedDateTimeObserver)

        // 天気情報が取得されたときにUIを更新
        val selectedFormattedWeatherDataObserver = Observer<String> { _ ->
            mainViewModel.viewModelScope.launch {
                jmaWeatherData = mainViewModel.getJmaData(selectedAreaCode)!!
                val selectedFormattedWeatherData = mainViewModel.generateResultText(jmaWeatherData).first.toString()
                weatherResultTextView.text = selectedFormattedWeatherData
            }
        }
        mainViewModel.generatedText.observe(this, selectedFormattedWeatherDataObserver)

        // setupUI()メソッド内のshowWeatherButtonのクリックリスナーを修正
        showWeatherButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && selectedAreaCode != "") {
                // 天気データを取得するメソッドを呼び出す
                mainViewModel.fetchWeatherDataToday(selectedAreaCode)

                mainViewModel.weatherData.observe(this) { weatherData ->
                    weatherData?.let {
                        // 天気データがnullでない場合の処理
                        // ここにUIを更新するコードを記述します
                        val formattedWeatherData = mainViewModel.fetchWeatherDataToday(selectedAreaCode)
                        weatherResultTextView.text = formattedWeatherData.toString()
                        showWeeklyWeatherButton.visibility = View.VISIBLE
                    } ?: Toast.makeText(this@MainActivity, "データが取得できませんでした", Toast.LENGTH_SHORT).show()
                }
                // 一週間の天気のボタンを有効にする
                showWeeklyWeatherButton.visibility = View.VISIBLE
            } else {
                Toast.makeText(this@MainActivity, "エリアが選択されていません", Toast.LENGTH_SHORT).show()
            }
        }

        showWeeklyWeatherButton.setOnClickListener {
            val intent = Intent(this, WeeklyWeatherActivity::class.java)
            intent.putExtra("areaCode", selectedAreaCode) // areaCodeをIntentに追加
            startActivity(intent) // WeeklyWeatherActivityに遷移
        }

// formattedWeatherDataObserverを追加
        val formattedWeatherDataObserver = Observer<List<WeatherData>> { weatherDataList ->
            // 天気情報データが更新されたときの処理を記述
            // たとえば、天気情報を表示するTextViewにデータを設定する
            val stringBuilder = StringBuilder()
            for (weatherData in weatherDataList) {
                stringBuilder.append("日付: ${weatherData.date}\n")
                stringBuilder.append("地域名: ${weatherData.areaName}\n")
                stringBuilder.append("降水確率: ${weatherData.precipitation}%\n")
                stringBuilder.append("天気予測: ${weatherData.predictedWeather}\n\n")
            }
            weatherResultTextView.text = stringBuilder.toString()
        }

// formattedWeatherDataObserverをObserverとして設定
        mainViewModel.formattedWeatherData.observe(this, formattedWeatherDataObserver)

        // 地域リストを表示するボタンにリスナーを設定
        showListViewButton.setOnClickListener {
            showListViewDialog()
        }
    }

    private fun showListViewDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_listview, null)
        val listView = dialogView.findViewById<ListView>(R.id.dialogListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, areaDic.keys.toList())
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("都道府県を選択してください")
            .setView(dialogView)
            .setNegativeButton("キャンセル") { dialog, _ -> dialog.dismiss() }
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedItem = adapter.getItem(position) ?: ""
            selectedAreaCode = areaDic[selectedItem] ?: ""
            Toast.makeText(this, "選択された地域: $selectedItem", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateTimePickers() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_time_picker, null)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.dialogDatePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.dialogTimePicker)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                // 日付と時間を取得して表示
                val year = datePicker.year
                val month = datePicker.month
                val dayOfMonth = datePicker.dayOfMonth
                val hourOfDay =
                    timePicker.hour
                val minute =
                    timePicker.minute
                val selectedDateTime = "${year}年${month + 1}月${dayOfMonth}日 ${hourOfDay}時${minute}分"
                selectedDateTimeTextView.text = selectedDateTime
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            datePicker.setOnDateChangedListener { _, _, _, _ ->
                // 日付が選択されたらTimePickerを表示
                timePicker.visibility = View.VISIBLE
                datePicker.visibility = View.GONE
            }
        }
        dialog.show()
    }
}

