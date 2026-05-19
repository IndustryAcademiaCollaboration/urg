package com.urg.edge.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.concurrent.thread

class AudioRecorder(
    private val sampleRate: Int = 16000,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private val chunks = mutableListOf<ShortArray>()

    fun start(): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return false

        synchronized(chunks) { chunks.clear() }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
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

        recordingThread = thread(name = "AudioRecorder", isDaemon = true) {
            val readBuf = ShortArray(512)
            while (isRecording) {
                val n = record.read(readBuf, 0, readBuf.size)
                if (n > 0) {
                    synchronized(chunks) { chunks.add(readBuf.copyOf(n)) }
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
        collected.forEach { chunk ->
            for (s in chunk) {
                samples[idx++] = s / 32768.0f
            }
        }
        return samples
    }
}
