package com.urg.edge.routing

import android.content.Context
import android.util.Log
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class RouteResult(
    val points: List<Pair<Double, Double>>, // (lat, lng) のリスト
    val distanceMeters: Double,
    val timeSeconds: Double
)

class BRouterEngine(private val context: Context) {

    companion object {
        private const val TAG = "BRouterEngine"
    }

    private fun getSegmentsDir(): File {
        return File(context.getExternalFilesDir(null), "segments").also { it.mkdirs() }
    }

    fun areSegmentsAvailable(): Boolean {
        val dir = getSegmentsDir()
        return com.urg.edge.map.rd5FileConfigs.all { config ->
            File(dir, config.fileName).exists()
        }
    }

    suspend fun calculateRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val segmentsDir = getSegmentsDir()
            Log.d(TAG, "Calculating route: from=($fromLat, $fromLng) to=($toLat, $toLng)")
            Log.d(TAG, "Segments dir: ${segmentsDir.absolutePath}")
            Log.d(TAG, "Segments files: ${segmentsDir.listFiles()?.map { it.name }}")
            val profileFile = copyProfileFromAssets("trekking.brf")
            Log.d(TAG, "Profile file: ${profileFile.absolutePath}, exists=${profileFile.exists()}")

            // RoutingContext のセットアップ
            val rc = RoutingContext()
            rc.localFunction = profileFile.absolutePath

            // ウェイポイントのセットアップ
            val waypoints = ArrayList<OsmNodeNamed>()

            val start = OsmNodeNamed()
            start.name = "from"
            // BRouterの内部座標系：(度 + オフセット) * 1000000
            start.ilon = ((fromLng + 180.0) * 1000000.0 + 0.5).toInt()
            start.ilat = ((fromLat + 90.0) * 1000000.0 + 0.5).toInt()
            waypoints.add(start)

            val end = OsmNodeNamed()
            end.name = "to"
            end.ilon = ((toLng + 180.0) * 1000000.0 + 0.5).toInt()
            end.ilat = ((toLat + 90.0) * 1000000.0 + 0.5).toInt()
            waypoints.add(end)

            // RoutingEngine の実行
            val engine = RoutingEngine(
                null,           // outfileBase（ファイル出力不要なのでnull）
                null,           // logfileBase
                segmentsDir,    // segmentDir
                waypoints,      // waypoints
                rc              // routingContext
            )
            engine.doRun(0L)

            // 結果取得
            val track = engine.foundTrack
            if (track == null || track.nodes.isEmpty()) {
                Log.w(TAG, "No route found")
                return@withContext null
            }

            val points = track.nodes.map { node ->
                val lat = (node.getILat() - 90000000) / 1000000.0
                val lng = (node.getILon() - 180000000) / 1000000.0
                Pair(lat, lng)
            }

            RouteResult(
                points = points,
                distanceMeters = track.distance.toDouble(),
                timeSeconds = track.getTotalSeconds().toDouble()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Route calculation failed: ${e.message}", e)
            null
        }
    }

    // assetsからprofileファイルとlookups.datをキャッシュディレクトリにコピー
    private fun copyProfileFromAssets(fileName: String): File {
        // lookups.dat は trekking.brf と同じディレクトリに必要
        copyAssetFile("lookups.dat")
        return copyAssetFile(fileName)
    }

    private fun copyAssetFile(fileName: String): File {
        val dest = File(context.cacheDir, fileName)
        // 常に最新をコピー（更新対応）
        context.assets.open(fileName).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return dest
    }
}