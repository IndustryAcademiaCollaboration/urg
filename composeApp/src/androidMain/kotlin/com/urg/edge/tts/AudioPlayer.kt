package com.urg.edge.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * 合成済み PCM（FloatArray, [-1, 1]）を [AudioTrack] で再生する。
 * STT 側の録音 [com.urg.edge.stt.AudioRecorder] と対になる再生クラス。再生に権限は不要。
 */
class AudioPlayer {

    private val lock = Any()
    private var track: AudioTrack? = null

    @Volatile
    private var playing = false

    /** [samples] を [sampleRate] で再生する。再生完了（または [stop] による中断）までブロックする。 */
    fun play(samples: FloatArray, sampleRate: Int) {
        if (samples.isEmpty()) return

        val current = synchronized(lock) {
            releaseTrackLocked()
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
            val newTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize, sampleRate * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track = newTrack
            playing = true
            newTrack
        }

        try {
            current.play()
            var offset = 0
            while (playing && offset < samples.size) {
                val written = current.write(
                    samples,
                    offset,
                    samples.size - offset,
                    AudioTrack.WRITE_BLOCKING,
                )
                if (written <= 0) break
                offset += written
            }
            // 書き込んだ分が再生し切られるまで待つ（中断時は playing=false で抜ける）。
            while (playing && current.playbackHeadPosition < samples.size) {
                Thread.sleep(20)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "play failed", e)
        } finally {
            synchronized(lock) {
                if (track === current) releaseTrackLocked()
            }
        }
    }

    /** 再生を中断する。 */
    fun stop() {
        synchronized(lock) {
            playing = false
            track?.let { t ->
                try {
                    if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        t.pause()
                        t.flush()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun releaseTrackLocked() {
        track?.apply {
            try {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    pause()
                    flush()
                    stop()
                }
            } catch (_: Exception) {
            }
            release()
        }
        track = null
    }
}
