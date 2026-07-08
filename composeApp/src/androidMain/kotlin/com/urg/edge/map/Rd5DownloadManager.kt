package com.urg.edge.map

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Rd5DownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "Rd5DownloadManager"
    }

    private fun getSegmentsDir(): File {
        return File(context.getExternalFilesDir(null), "segments").also { it.mkdirs() }
    }

    fun areAllSegmentsDownloaded(): Boolean {
        // TODO: 県ごとのMBTilesに対応する際は、ユーザーの県に必要な
        //       セグメントのみチェックするよう変更する
        return rd5FileConfigs.all { config ->
            val file = File(getSegmentsDir(), config.fileName)
            file.exists() && file.length() > 1024
        }
    }

    // assetsからrd5ファイルをexternalFilesにコピー
    // TODO: 県ごとのMBTilesに対応する際は、ユーザーの県に対応する
    //       セグメントファイルのみコピーするよう変更する
    suspend fun downloadAllSegments(
        onProgress: (current: Int, total: Int, fileName: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val configs = rd5FileConfigs
        configs.forEachIndexed { index, config ->
            val destFile = File(getSegmentsDir(), config.fileName)

            // 有効なファイルが既にあればスキップ
            if (destFile.exists() && destFile.length() > 1024) {
                Log.d(TAG, "Already exists: ${config.fileName}")
                onProgress(index + 1, configs.size, config.fileName)
                return@forEachIndexed
            }

            onProgress(index + 1, configs.size, config.fileName)

            try {
                // assetsからコピー
                context.assets.open(config.fileName).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 65536)
                    }
                }
                Log.d(TAG, "Copied from assets: ${config.fileName} (${destFile.length()} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy ${config.fileName} from assets: ${e.message}")
                // assetsにない場合はGoogle Driveからダウンロード（フォールバック）
                val success = downloadFromGoogleDrive(config.downloadUrl, destFile)
                if (!success) {
                    Log.e(TAG, "Failed to download ${config.fileName}")
                    return@withContext false
                }
            }
        }
        true
    }

    private fun downloadFromGoogleDrive(urlString: String, outputFile: File): Boolean {
        return try {
            val tempFile = File(outputFile.absolutePath + ".tmp")
            var currentUrl = urlString
            var connection: HttpURLConnection
            var redirectCount = 0

            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    currentUrl = location
                    redirectCount++
                    if (redirectCount > 10) return false
                    continue
                }

                val contentType = connection.contentType ?: ""
                if (contentType.contains("text/html")) {
                    Log.e(TAG, "Got HTML instead of binary for ${outputFile.name}")
                    connection.disconnect()
                    return false
                }
                break
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 65536)
                }
            }
            connection.disconnect()

            if (tempFile.length() < 1024) {
                tempFile.delete()
                return false
            }

            tempFile.renameTo(outputFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${outputFile.name}: ${e.message}")
            false
        }
    }
}