package com.urg.edge

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

object JmaAlertChecker {

    private const val URL = "https://www.jma.go.jp/bosai/quake/data/list.json"
    private const val WINDOW_MINUTES = 5L

    // 震度5弱以上をトリガーとする
    private val TRIGGER_INTENSITIES = setOf("50", "55", "60", "70")

    suspend fun isDisasterOccurring(): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = java.net.URL(URL).readText()
            val array = JSONArray(json)
            val threshold = System.currentTimeMillis() - WINDOW_MINUTES * 60 * 1000

            for (i in 0 until minOf(array.length(), 10)) {
                val item = array.getJSONObject(i)
                val maxi = item.optString("maxi", "")
                val at   = parseIso8601(item.optString("at", ""))
                if (maxi in TRIGGER_INTENSITIES && at > threshold) {
                    Log.d("JmaChecker", "Disaster: maxi=$maxi")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.w("JmaChecker", "Fetch failed: ${e.message}")
            false
        }
    }

    private fun parseIso8601(text: String): Long =
        runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(text)?.time ?: 0L
        }.getOrDefault(0L)
}