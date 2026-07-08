package com.urg.edge.shelter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

class ShelterRepository(private val context: Context) {

    companion object {
        private const val TAG = "ShelterRepository"
        private const val DEFAULT_COUNT = 5   // 種別未選択時の各種別表示件数
        private const val FILTERED_COUNT = 5  // 種別選択時の表示件数
    }

    // 全種別から各5件ずつ（未選択時）
    suspend fun getNearestShelters(
        lat: Double,
        lng: Double
    ): Map<ShelterType, List<Shelter>> = withContext(Dispatchers.IO) {
        ShelterType.entries.associate { type ->
            type to loadAndSort(type, lat, lng, DEFAULT_COUNT)
        }
    }

    // 指定種別から10件（種別選択時）
    suspend fun getNearestSheltersByType(
        type: ShelterType,
        lat: Double,
        lng: Double
    ): List<Shelter> = withContext(Dispatchers.IO) {
        loadAndSort(type, lat, lng, FILTERED_COUNT)
    }

    private fun loadAndSort(
        type: ShelterType,
        lat: Double,
        lng: Double,
        count: Int
    ): List<Shelter> {
        return try {
            val shelters = parseCsv(type)
            shelters
                .map { it.copy(distanceMeters = haversine(lat, lng, it.latitude, it.longitude)) }
                .sortedBy { it.distanceMeters }
                .take(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ${type.csvFileName}: ${e.message}")
            emptyList()
        }
    }

    private fun parseCsv(type: ShelterType): List<Shelter> {
        val lines = context.assets.open(type.csvFileName)
            .bufferedReader(Charsets.UTF_8)
            .readLines()

        if (lines.size < 2) return emptyList()

        // BOMを除去したヘッダー行
        val header = lines[0].trimStart('\uFEFF').split(",")

        // 各CSVの列名に対応して緯度・経度・施設名のインデックスを取得
        val nameIdx = header.indexOfFirst {
            it.trim() in listOf("施設・場所名", "施設名")
        }
        val latIdx = header.indexOfFirst { it.trim() == "緯度" }
        val lngIdx = header.indexOfFirst { it.trim() == "経度" }

        if (nameIdx < 0 || latIdx < 0 || lngIdx < 0) {
            Log.e(TAG, "Column not found in ${type.csvFileName}: name=$nameIdx lat=$latIdx lng=$lngIdx")
            return emptyList()
        }

        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(",")
            if (cols.size <= maxOf(nameIdx, latIdx, lngIdx)) return@mapNotNull null
            val name = cols[nameIdx].trim()
            val lat = cols[latIdx].trim().toDoubleOrNull() ?: return@mapNotNull null
            val lng = cols[lngIdx].trim().toDoubleOrNull() ?: return@mapNotNull null
            if (name.isEmpty()) return@mapNotNull null
            Shelter(name = name, latitude = lat, longitude = lng, type = type)
        }
    }

    // Haversine公式で2点間の距離(m)を計算
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}