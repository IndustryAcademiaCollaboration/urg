package com.urg.edge.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.concurrent.thread
import kotlin.math.sqrt

class AudioRecorder(
    private val sampleRate: Int = 16000,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private val chunks = mutableListOf<ShortArray>()

    fun start(onAmplitude: ((Float) -> Unit)? = null): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return false

        synchronized(chunks) { chunks.clear() }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )
        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "RECORD_AUDIO permission missing", e)
            return false
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        audioRecord = record
        isRecording = true
        record.startRecording()
        Log.d("AudioRecorder", "start: sampleRate=$sampleRate, minBuf=$minBufferSize")

        recordingThread = thread(name = "AudioRecorder", isDaemon = true) {
            val readBuf = ShortArray(512)
            while (isRecording) {
                val n = record.read(readBuf, 0, readBuf.size)
                if (n > 0) {
                    synchronized(chunks) { chunks.add(readBuf.copyOf(n)) }
                    // RMS 振幅を計算してコールバックで通知
                    if (onAmplitude != null) {
                        var sumSq = 0f
                        for (i in 0 until n) {
                            val v = readBuf[i] / 32768.0f
                            sumSq += v * v
                        }
                        onAmplitude(sqrt(sumSq / n))
                    }
                }
            }
        }
        return true
    }

    fun stop(): FloatArray {
        isRecording = false
        recordingThread?.join()
        recordingThread = null

        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null

        val collected = synchronized(chunks) {
            val copy = chunks.toList()
            chunks.clear()
            copy
        }
        val total = collected.sumOf { it.size }
        val samples = FloatArray(total)
        var idx = 0
        var maxAbs = 0f
        var sumAbs = 0f
        collected.forEach { chunk ->
            for (s in chunk) {
                val v = s / 32768.0f
                samples[idx++] = v
                val a = if (v < 0f) -v else v
                if (a > maxAbs) maxAbs = a
                sumAbs += a
            }
        }
        val meanAbs = if (total > 0) sumAbs / total else 0f
        Log.d(
            "AudioRecorder",
            "stop: chunks=${collected.size}, samples=$total, durationMs=${total * 1000 / sampleRate}, maxAbs=$maxAbs, meanAbs=$meanAbs"
        )
        return samples
    }
}
