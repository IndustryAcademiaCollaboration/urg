package com.urg.edge.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import com.piperplus.g2p.PiperPlusG2p
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * piper-plus-g2p-android で G2P し、Piper-Plus ONNX モデルで音声合成して
 * AudioPlayer で再生する TTS エンジン。
 *
 * 旧 SherpaTtsEngine の置き換え。インターフェース [TtsEngine] と再生クラス
 * [AudioPlayer] はそのまま流用する。
 */
class PiperTtsEngine(
    private val g2p: PiperPlusG2p,
    private val ortEnv: OrtEnvironment,
    private val ortSession: OrtSession,
    private val piperConfig: PiperConfig,
    private val ttsConfig: TtsConfig,
    private val player: AudioPlayer,
) : TtsEngine {

    // multi-lang モデルかどうかは入力テンソル名から動的判定する。
    private val inputNames: Set<String> = ortSession.inputNames

    override fun speak(text: String) {
        if (text.isBlank()) return
        player.stop()

        // 1. テキスト→音素列
        val phonemes = g2p.phonemize(text, ttsConfig.language).phonemeList
        if (phonemes.isEmpty()) {
            Log.w("TTS_SPEAK", "phonemize returned empty for text.len=${text.length}")
            return
        }

        // 2. 音素→ID 列（Piper 慣例: 先頭^/末尾$、音素ごとに blank(_) を挟む）
        val ids = buildIds(phonemes)
        if (ids.isEmpty()) {
            Log.w("TTS_SPEAK", "no valid phoneme IDs after mapping")
            return
        }

        // 3. ONNX 推論
        val samples = runInference(ids)
        Log.d(
            "TTS_SPEAK",
            "synthesized: samples=${samples.size}, sampleRate=${piperConfig.sampleRate}, textLen=${text.length}",
        )

        // 4. 再生
        player.play(samples, piperConfig.sampleRate)
    }

    private fun buildIds(phonemes: List<String>): LongArray {
        val map = piperConfig.phonemeIdMap
        val bos = map["^"]?.firstOrNull()?.toLong()
        val eos = map["$"]?.firstOrNull()?.toLong()
        val pad = map["_"]?.firstOrNull()?.toLong() ?: 0L

        val out = ArrayList<Long>(phonemes.size * 2 + 4)
        bos?.let { out.add(it); out.add(pad) }
        for (p in phonemes) {
            val mapped = map[p] ?: continue
            for (id in mapped) {
                out.add(id.toLong())
                out.add(pad)
            }
        }
        eos?.let { out.add(it) }
        return out.toLongArray()
    }

    private fun runInference(ids: LongArray): FloatArray {
        val numPhonemes = ids.size.toLong()
        val inputs = mutableMapOf<String, OnnxTensor>()

        try {
            inputs["input"] = OnnxTensor.createTensor(
                ortEnv, LongBuffer.wrap(ids), longArrayOf(1L, numPhonemes),
            )
            inputs["input_lengths"] = OnnxTensor.createTensor(
                ortEnv, LongBuffer.wrap(longArrayOf(numPhonemes)), longArrayOf(1L),
            )
            inputs["scales"] = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(floatArrayOf(
                    ttsConfig.noiseScale, ttsConfig.lengthScale, ttsConfig.noiseW,
                )),
                longArrayOf(3L),
            )
            if ("sid" in inputNames) {
                inputs["sid"] = OnnxTensor.createTensor(
                    ortEnv,
                    LongBuffer.wrap(longArrayOf(ttsConfig.speakerId.toLong())),
                    longArrayOf(1L),
                )
            }
            if ("lid" in inputNames) {
                val langId = (piperConfig.languageIdMap[ttsConfig.language] ?: 0).toLong()
                inputs["lid"] = OnnxTensor.createTensor(
                    ortEnv, LongBuffer.wrap(longArrayOf(langId)), longArrayOf(1L),
                )
            }
            // PR #320 以降の MB-iSTFT-VITS2 export は speaker_embedding/mask が
            // 必須入力として宣言される。ゼロベクトル + mask=0 を feed すると
            // モデル側で emb_g(sid) にフォールバックする（Tsukuyomi-chan v1.12.0）。
            if ("speaker_embedding" in inputNames) {
                val embDim = (ortSession.inputInfo["speaker_embedding"]?.info
                    as? ai.onnxruntime.TensorInfo)
                    ?.shape?.getOrNull(1)
                    ?.takeIf { it > 0 }?.toInt() ?: 512
                inputs["speaker_embedding"] = OnnxTensor.createTensor(
                    ortEnv,
                    FloatBuffer.wrap(FloatArray(embDim)),
                    longArrayOf(1L, embDim.toLong()),
                )
            }
            if ("speaker_embedding_mask" in inputNames) {
                inputs["speaker_embedding_mask"] = OnnxTensor.createTensor(
                    ortEnv,
                    LongBuffer.wrap(longArrayOf(0L)),
                    longArrayOf(1L, 1L),
                )
            }
            // Piper-Plus 独自の韻律特徴 (a1/a2/a3 per phoneme)。
            // Kotlin G2P (piper-plus-g2p-android 1.0.0) は韻律を返さないため、
            // 全ゼロで feed して infer.py の None→[0,0,0] フォールバック相当にする。
            // 形状は [1, num_phonemes, 3]、dtype は Int64。
            if ("prosody_features" in inputNames) {
                val zeros = LongArray(ids.size * 3)
                inputs["prosody_features"] = OnnxTensor.createTensor(
                    ortEnv,
                    LongBuffer.wrap(zeros),
                    longArrayOf(1L, numPhonemes, 3L),
                )
            }

            // モデルが必要としない入力を渡すと失敗するので、入力名でフィルタする。
            val feed = inputs.filterKeys { it in inputNames }
            ortSession.run(feed).use { result ->
                return flattenAudio(result[0].value)
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    /**
     * Piper VITS の出力は通常 [1, 1, T] の FloatArray。ネストを剥がして 1 次元の FloatArray に。
     */
    private fun flattenAudio(value: Any?): FloatArray {
        var v: Any? = value
        while (v is Array<*>) {
            v = v.firstOrNull() ?: return FloatArray(0)
        }
        return (v as? FloatArray) ?: FloatArray(0)
    }

    override fun stop() {
        player.stop()
    }

    override fun close() {
        player.stop()
        try { ortSession.close() } catch (_: Exception) {}
        try { g2p.close() } catch (_: Exception) {}
        // OrtEnvironment は singleton のためここでは閉じない。
    }
}
