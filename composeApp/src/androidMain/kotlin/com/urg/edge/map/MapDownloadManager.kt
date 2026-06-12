package com.urg.edge.map

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MapDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "MapDownloadManager"
        private const val PREFS_NAME = "map_prefs"
        private const val KEY_DOWNLOADED_PREFECTURE = "downloaded_prefecture"
    }

    fun getDownloadedPrefecture(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DOWNLOADED_PREFECTURE, null)
    }

    fun getMbtilesPath(fileName: String): String {
        return context.getExternalFilesDir(null)?.absolutePath + "/$fileName.mbtiles"
    }

    fun isMbtilesDownloaded(fileName: String): Boolean {
        return File(getMbtilesPath(fileName)).exists()
    }

    suspend fun getPrefectureFromLocation(lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            // まずGeocoderを試みる
            try {
                val geocoder = Geocoder(context, Locale.JAPAN)
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val result = addresses?.firstOrNull()?.adminArea
                if (!result.isNullOrEmpty()) {
                    Log.d(TAG, "Geocoder success: $result")
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder failed: ${e.message}")
            }

            // フォールバック：緯度経度から県を判定
            Log.d(TAG, "Falling back to coordinate-based detection")
            detectPrefectureByCoordinates(lat, lng)
        }
    }

    private fun detectPrefectureByCoordinates(lat: Double, lng: Double): String? {
        return when {
            lat in 34.57..35.42 && lng in 136.67..137.82 -> "愛知県"
            lat in 34.93..35.79 && lng in 136.72..137.12 -> "岐阜県"
            lat in 34.47..35.20 && lng in 136.00..137.00 -> "三重県"
            lat in 34.60..35.68 && lng in 135.88..136.88 -> "滋賀県"
            lat in 34.71..35.79 && lng in 135.39..135.88 -> "京都府"
            lat in 34.27..34.99 && lng in 135.08..135.88 -> "大阪府"
            lat in 34.21..35.67 && lng in 134.26..135.47 -> "兵庫県"
            lat in 33.82..34.70 && lng in 135.56..136.17 -> "奈良県"
            lat in 33.44..34.35 && lng in 135.21..136.06 -> "和歌山県"
            lat in 34.53..35.26 && lng in 137.40..138.24 -> "静岡県"
            lat in 35.10..36.77 && lng in 137.36..138.24 -> "長野県"
            lat in 35.49..36.79 && lng in 136.45..137.67 -> "富山県"
            lat in 36.00..37.53 && lng in 136.45..137.35 -> "石川県"
            lat in 35.43..36.30 && lng in 135.88..136.79 -> "福井県"
            lat in 35.22..36.25 && lng in 138.38..139.24 -> "山梨県"
            lat in 35.10..36.12 && lng in 138.67..139.15 -> "神奈川県"
            lat in 35.29..36.24 && lng in 139.08..140.18 -> "埼玉県"
            lat in 35.33..36.11 && lng in 139.73..140.93 -> "千葉県"
            lat in 35.50..35.90 && lng in 139.42..139.92 -> "東京都"
            lat in 36.03..37.06 && lng in 139.33..140.29 -> "栃木県"
            lat in 36.19..37.05 && lng in 138.71..139.70 -> "群馬県"
            lat in 36.03..36.86 && lng in 139.69..140.90 -> "茨城県"
            lat in 36.76..40.57 && lng in 140.22..141.69 -> "岩手県"
            lat in 39.37..41.56 && lng in 140.20..141.69 -> "青森県"
            lat in 37.77..39.00 && lng in 140.19..141.67 -> "宮城県"
            lat in 39.00..40.54 && lng in 139.69..140.97 -> "秋田県"
            lat in 37.71..39.00 && lng in 139.69..140.59 -> "山形県"
            lat in 36.78..37.97 && lng in 139.24..140.98 -> "福島県"
            lat in 36.39..37.68 && lng in 138.24..139.77 -> "新潟県"
            lat in 43.37..45.56 && lng in 141.34..145.82 -> "北海道"
            lat in 33.08..34.36 && lng in 129.27..131.12 -> "福岡県"
            lat in 33.08..33.84 && lng in 129.27..130.24 -> "佐賀県"
            lat in 32.58..34.74 && lng in 128.62..130.18 -> "長崎県"
            lat in 32.08..33.24 && lng in 130.08..131.34 -> "熊本県"
            lat in 32.72..33.91 && lng in 130.89..132.10 -> "大分県"
            lat in 31.36..32.72 && lng in 130.23..131.97 -> "宮崎県"
            lat in 30.01..32.28 && lng in 129.54..131.98 -> "鹿児島県"
            lat in 24.00..27.09 && lng in 122.94..129.67 -> "沖縄県"
            lat in 33.08..35.00 && lng in 131.98..134.20 -> "山口県"
            lat in 33.85..35.15 && lng in 132.02..133.18 -> "広島県"
            lat in 34.20..35.15 && lng in 133.18..134.46 -> "岡山県"
            lat in 34.55..35.60 && lng in 132.01..133.59 -> "島根県"
            lat in 35.00..35.60 && lng in 133.18..134.46 -> "鳥取県"
            lat in 33.44..34.41 && lng in 133.10..134.32 -> "高知県"
            lat in 33.26..34.21 && lng in 133.47..134.39 -> "愛媛県"
            lat in 33.65..34.46 && lng in 133.44..134.39 -> "徳島県"
            lat in 34.15..34.87 && lng in 133.44..134.46 -> "香川県"
            else -> {
                Log.w(TAG, "Could not detect prefecture for: $lat, $lng")
                null
            }
        }
    }

    suspend fun downloadMbtiles(
        fileName: String,
        onProgress: (Int) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val downloadUrl = getGoogleDriveDownloadUrl(fileName)
            if (downloadUrl == null) {
                Log.e(TAG, "No download URL for: $fileName")
                return@withContext false
            }

            try {
                val outputFile = File(getMbtilesPath(fileName))
                val tempFile = File(getMbtilesPath(fileName) + ".tmp")

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.connect()

                val fileSize = connection.contentLength

                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var bytes: Int

                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes
                            if (fileSize > 0) {
                                onProgress((downloaded * 100 / fileSize).toInt())
                            }
                        }
                    }
                }

                tempFile.renameTo(outputFile)

                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_DOWNLOADED_PREFECTURE, fileName)
                    .apply()

                Log.d(TAG, "Download complete: $fileName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                false
            }
        }
    }
}