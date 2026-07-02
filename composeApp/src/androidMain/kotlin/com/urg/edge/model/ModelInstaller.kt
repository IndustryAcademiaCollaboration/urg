package com.urg.edge.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class RemoteModelFile(
    val fileName: String,
    val url: String,
    val expectedBytes: Long? = null,
)

object ModelInstaller {
    suspend fun ensureFiles(
        targetDir: File,
        files: List<RemoteModelFile>,
        label: String,
        onStatus: suspend (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        files.forEachIndexed { index, file ->
            val destination = File(targetDir, file.fileName)
            if (destination.isComplete(file.expectedBytes)) {
                return@forEachIndexed
            }

            onStatus("$label をダウンロード中... ${index + 1}/${files.size}: ${file.fileName}")
            download(file, destination) { downloaded, total ->
                if (total > 0) {
                    val percent = (downloaded * 100 / total).toInt()
                    if (percent % 10 == 0) {
                        onStatus(
                            "$label をダウンロード中... ${index + 1}/${files.size}: " +
                                "${file.fileName} $percent%"
                        )
                    }
                }
            }
        }
    }

    private suspend fun download(
        file: RemoteModelFile,
        destination: File,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit,
    ) {
        destination.parentFile?.mkdirs()
        val tempFile = File(destination.parentFile, "${destination.name}.tmp")

        var startByte = if (tempFile.exists()) tempFile.length() else 0L
        var connection = openConnection(file.url, startByte)
        var append = startByte > 0 && connection.responseCode == HttpURLConnection.HTTP_PARTIAL

        if (startByte > 0 && !append) {
            tempFile.delete()
            startByte = 0L
            connection.disconnect()
            connection = openConnection(file.url, startByte)
        }

        val totalBytes = when {
            file.expectedBytes != null -> file.expectedBytes
            connection.contentLengthLong > 0 -> connection.contentLengthLong + startByte
            else -> -1L
        }
        var downloadedBytes = startByte
        var lastReported = -1

        try {
            connection.inputStream.use { input ->
                FileOutputStream(tempFile, append).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val percent = (downloadedBytes * 100 / totalBytes).toInt()
                            if (percent != lastReported && percent % 10 == 0) {
                                lastReported = percent
                                onProgress(downloadedBytes, totalBytes)
                            }
                        }
                    }
                }
            }

            if (!tempFile.isComplete(file.expectedBytes)) {
                throw IllegalStateException("ダウンロードサイズが不正です: ${file.fileName}")
            }
            if (destination.exists()) destination.delete()
            if (!tempFile.renameTo(destination)) {
                throw IllegalStateException("モデルファイルの配置に失敗しました: ${file.fileName}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, startByte: Long): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        if (startByte > 0) {
            connection.setRequestProperty("Range", "bytes=$startByte-")
        }
        connection.connect()
        return connection
    }

    private fun File.isComplete(expectedBytes: Long?): Boolean {
        if (!exists()) return false
        return expectedBytes == null || length() == expectedBytes
    }
}
