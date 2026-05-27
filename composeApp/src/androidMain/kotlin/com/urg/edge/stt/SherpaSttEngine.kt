package com.urg.edge.stt

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineRecognizer

class SherpaSttEngine(
    private val recognizer: OfflineRecognizer,
    private val sampleRate: Int,
) : SttEngine {

    override fun recognize(samples: FloatArray): String {
        if (samples.size < sampleRate / 8) {
            Log.d("STT", "audio too short: samples=${samples.size}")
            return ""
        }

        val paddedSamples =
            if (samples.size < sampleRate) {
                samples + FloatArray(sampleRate - samples.size)
            } else {
                samples
            }

        val stream = recognizer.createStream()
        return try {
            Log.d("STT", "recognize samples=${paddedSamples.size}")
            stream.acceptWaveform(samples, sampleRate)
            recognizer.decode(stream)
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun close() {
        recognizer.release()
    }
}

