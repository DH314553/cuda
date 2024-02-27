package com.daisaku31469.cuda.data

import org.json.JSONArray

object PrecipitationProbabilityUtil {
    fun getPrecipitationProbability(data: JSONArray?): MutableList<Float> {
        val pops = mutableListOf<Float>()

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
                            val popsObject = areasArray.getJSONObject(l)
                            if (popsObject.has("pops")) {
                                val popsArray = popsObject.getJSONArray("pops")
                                for (m in 0 until popsArray.length()) {
                                    val popString = popsArray.getString(m)
                                    if (popString.isBlank()) {
                                        pops.add(0f)
                                    } else {
                                        pops.add(popString.toFloat())
                                    }
                                }
                            } else {
                                pops.add(0f)
                            }
                        }
                    }
                }
            }
        }
        // 空文字列や空の要素を取り除く
        return removeEmptyAndZeroValues(pops)
    }

//    fun getWeeklyPrecipitationProbability(data: JSONArray?): Pair<MutableList<Float>, MutableList<Float>> {
//        val todayPops = mutableListOf<Float>()
//        val weeklyPops = mutableListOf<Float>()
//
//        if (data != null) {
//            for (i in 0 until data.length()) {
//                val weatherStation = data.getJSONObject(i)
//                val timeSeriesArray = weatherStation.getJSONArray("timeSeries")
//                for (j in 0 until timeSeriesArray.length()) {
//                    val timeSeriesObject = timeSeriesArray.getJSONObject(j)
//                    val timeDefinesArray = timeSeriesObject.getJSONArray("timeDefines")
//                    val todayDateString = timeDefinesArray.getString(0).substringBefore("T")// 今日の日付を取得
//                    val weeklyDateString = timeDefinesArray.getString(1).substringBefore("T") // 一週間後の日付を取得
//
//                    // 各時間定義のデータを処理
//                    for (k in 0 until timeDefinesArray.length()) {
//                        val areasArray = timeSeriesObject.getJSONArray("areas")
//                        for (l in 0 until areasArray.length()) {
//                            val popsObject = areasArray.getJSONObject(l)
//                            if (popsObject.has("pops")) {
//                                val popsArray = popsObject.getJSONArray("pops")
//                                for (m in 0 until popsArray.length()) {
//                                    val popString = popsArray.getString(m)
//                                    if (popString.isBlank()) {
//                                        todayPops.add(0f)
//                                        weeklyPops.add(0f)
//                                    } else {
//                                        val pop = popString.toFloat()
//                                        if (timeDefinesArray.getString(m).substringBefore("T") == todayDateString) {
//                                            todayPops.add(pop) // 今日の降水確率を追加
//                                        } else if (timeDefinesArray.getString(m).substringBefore("T") == weeklyDateString) {
//                                            weeklyPops.add(pop) // 一週間後の降水確率を追加
//                                        }
//                                    }
//                                }
//                            } else {
//                                todayPops.add(0f)
//                                weeklyPops.add(0f)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        // 空文字列や空の要素を取り除く
//        val cleanedTodayPops = removeEmptyAndZeroValues(todayPops)
//        val cleanedWeeklyPops = removeEmptyAndZeroValues(weeklyPops)
//        return Pair(cleanedTodayPops, cleanedWeeklyPops)
//    }

    private fun removeEmptyAndZeroValues(pops: MutableList<Float>): MutableList<Float> {
        val nonEmptyValues = mutableListOf<Float>()
        for (pop in pops) {
            if (pop != 0f) {
                nonEmptyValues.add(pop)
            }
        }
        return nonEmptyValues
    }
}
