package com.urg.edge.stt

import android.content.res.AssetManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavLoader {
    fun loadFromAssets(assets: AssetManager, path: String): FloatArray {
        val bytes = assets.open(path).use { it.readBytes() }
        val data = findDataChunk(bytes)
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(data.size / 2)
        for (i in samples.indices) {
            samples[i] = buffer.short / 32768.0f
        }
        return samples
    }

    private fun findDataChunk(bytes: ByteArray): ByteArray {
        for (i in 12..bytes.size - 8) {
            if (bytes[i] == 'd'.code.toByte() && bytes[i + 1] == 'a'.code.toByte() &&
                bytes[i + 2] == 't'.code.toByte() && bytes[i + 3] == 'a'.code.toByte()
            ) {
                val size = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                return bytes.copyOfRange(i + 8, i + 8 + size)
            }
        }
        throw IllegalArgumentException("WAV data chunk not found")
    }
}
