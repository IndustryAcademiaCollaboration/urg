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

// 「障害物あり」ボタンで指定する通行禁止エリア（BRouterのnogo機能に対応）
data class NogoPoint(
    val lat: Double,
    val lon: Double,
    val radiusMeters: Double = 25.0 // 道路1本を塞ぐイメージのデフォルト半径
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
        toLng: Double,
        nogoPoints: List<NogoPoint> = emptyList()
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val segmentsDir = getSegmentsDir()
            Log.d(TAG, "Calculating route: from=($fromLat, $fromLng) to=($toLat, $toLng)")
            Log.d(TAG, "Segments dir: ${segmentsDir.absolutePath}")
            Log.d(TAG, "Segments files: ${segmentsDir.listFiles()?.map { it.name }}")
            Log.d(TAG, "Nogo points: ${nogoPoints.size}")
            val profileFile = copyProfileFromAssets("trekking.brf")
            Log.d(TAG, "Profile file: ${profileFile.absolutePath}, exists=${profileFile.exists()}")

            // RoutingContext のセットアップ
            val rc = RoutingContext()
            rc.localFunction = profileFile.absolutePath

            // 障害物（nogoポイント）を設定。「通れない」と指定した地点を中心とした
            // 円の中を通るルートは、BRouterが計算時に避けてくれる
            if (nogoPoints.isNotEmpty()) {
                val nogoList = nogoPoints.map { nogo ->
                    OsmNodeNamed().apply {
                        // BRouterの命名規則「nogo<半径(m)>」。prepareNogoPointsはこの名前から
                        // 半径を読み取ってradiusに反映するため、この形式に合わせる必要がある
                        name = "nogo${nogo.radiusMeters.toInt()}"
                        // ウェイポイントと同じ座標系（(度 + オフセット) * 1000000）でエンコード
                        ilon = ((nogo.lon + 180.0) * 1000000.0 + 0.5).toInt()
                        ilat = ((nogo.lat + 90.0) * 1000000.0 + 0.5).toInt()
                        radius = nogo.radiusMeters
                        isNogo = true // これが無いと円の中に入っても通行禁止として扱われない
                        // nogoWeightをNaNにしないと「重み付きnogo（追加コスト0）」＝実質無視されてしまう。
                        // 絶対通行禁止として扱わせるにはNaNが必須（BRouter公式のdecodeNogoと同じ設定）
                        nogoWeight = Double.NaN
                    }
                }.toMutableList()
                // BRouter公式アプリと同じ手順：代入前に必ずprepareNogoPointsを呼ぶ必要がある
                RoutingContext.prepareNogoPoints(nogoList)
                rc.nogopoints = nogoList
            }

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
            ).also {
                Log.d(TAG, "RESULT nogoCount=${nogoPoints.size} distance=${it.distanceMeters} " +
                        "pointsCount=${points.size} firstPoint=${points.firstOrNull()} lastPoint=${points.lastOrNull()}")
            }
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