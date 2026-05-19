package com.urg.edge.stt

import com.k2fsa.sherpa.onnx.OfflineRecognizer

class SherpaSttEngine(
    private val recognizer: OfflineRecognizer,
    private val sampleRate: Int,
) : SttEngine {

    override fun recognize(samples: FloatArray): String {
        val stream = recognizer.createStream()
        return try {
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
