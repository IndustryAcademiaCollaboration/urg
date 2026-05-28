package com.urg.edge.tts

import org.json.JSONObject

/**
 * Piper-Plus の config.json をパースした結果。
 *
 * 必要なのは sample_rate、phoneme_id_map（音素→ID 配列）、
 * language_id_map（multi-lang モデル用、無ければ空）、num_speakers。
 */
data class PiperConfig(
    val sampleRate: Int,
    /** 音素文字列 → ID 配列。Piper は 1 音素に複数 ID をマップすることがある。 */
    val phonemeIdMap: Map<String, IntArray>,
    /** "ja" → 0 等。multi-lang モデルで lid 入力に使う。単一言語モデルなら空。 */
    val languageIdMap: Map<String, Int>,
    val numSpeakers: Int,
    val numLanguages: Int,
) {
    companion object {
        fun parse(json: String): PiperConfig {
            val root = JSONObject(json)

            // sample_rate は audio.sample_rate にあるのが標準だが、トップレベルにもフォールバック。
            val audio = root.optJSONObject("audio")
            val sampleRate = audio?.optInt("sample_rate", 0)?.takeIf { it > 0 }
                ?: root.optInt("sample_rate", 22050)

            val phonemeIdMap = mutableMapOf<String, IntArray>()
            root.optJSONObject("phoneme_id_map")?.let { pim ->
                val keys = pim.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = pim.optJSONArray(k) ?: continue
                    phonemeIdMap[k] = IntArray(arr.length()) { arr.getInt(it) }
                }
            }

            val languageIdMap = mutableMapOf<String, Int>()
            root.optJSONObject("language_id_map")?.let { lim ->
                val keys = lim.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    languageIdMap[k] = lim.optInt(k, -1)
                }
            }

            return PiperConfig(
                sampleRate = sampleRate,
                phonemeIdMap = phonemeIdMap,
                languageIdMap = languageIdMap,
                numSpeakers = root.optInt("num_speakers", 1),
                numLanguages = root.optInt("num_languages", 1),
            )
        }
    }
}
