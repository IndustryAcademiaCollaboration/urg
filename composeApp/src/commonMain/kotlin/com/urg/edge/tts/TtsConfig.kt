package com.urg.edge.tts

/**
 * 日本語TTSの設定。
 *
 * テキスト→音素変換は piper-plus-g2p-android (OpenJTalk 内蔵)、
 * 音声合成は Piper-Plus の Tsukuyomi-chan ONNX を onnxruntime-android で実行する。
 * Androidではモデルを初回起動時に端末ストレージへダウンロードし、OpenJTalk 辞書は assets に同梱する。
 */
data class TtsConfig(
    /** assets 配下の Piper モデルディレクトリ。 */
    val modelDir: String = "piper/tsukuyomi",
    val modelFile: String = "tsukuyomi-chan-6lang-fp16.onnx",
    val configFile: String = "config.json",
    /** OpenJTalk 辞書を置く assets 直下のディレクトリ名。初回起動時に filesDir へ展開される。 */
    val dictAssetDir: String = "open_jtalk_dic",
    /** G2P 対象の言語コード（"ja", "en", "zh"...）。Tsukuyomi は 6 言語対応。 */
    val language: String = "ja",
    /** 話者 ID。Tsukuyomi-chan は 1 話者（sid=0）のみ。 */
    val speakerId: Int = 0,
    /** 発話の長さスケール。MB-iSTFT 版は 1.5 推奨（1.0 だと早口）。少し速めにする調整値。 */
    val lengthScale: Float = 1.5f,
    /** ノイズスケール（抑揚の強さ）。Piper 標準は 0.667。 */
    val noiseScale: Float = 0.667f,
    /** 音素時間長のノイズ。Piper 標準は 0.8。 */
    val noiseW: Float = 0.8f,
    /** ONNX 推論のスレッド数。 */
    val numThreads: Int = 2,
)
