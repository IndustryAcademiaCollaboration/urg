package com.urg.edge.tts

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.piperplus.g2p.OpenJTalkDictionary
import com.piperplus.g2p.PiperPlusG2p
import java.io.File

/**
 * Piper-Plus による日本語 TTS エンジンを構築する。
 *
 * 構成:
 * - G2P: piper-plus-g2p-android（OpenJTalk 内蔵、Maven Central）
 * - 合成: Tsukuyomi-chan ONNX（onnxruntime-android で実行）
 * - 再生: [AudioPlayer]（既存）
 *
 * STT 側 [com.urg.edge.stt.createSttEngine] と対になる Factory。
 */
fun createTtsEngine(context: Context, config: TtsConfig = TtsConfig()): TtsEngine {
    val assets = context.assets

    // 1. Piper モデルファイルの存在確認
    val modelAvailable = runCatching { assets.list(config.modelDir) }.getOrNull()?.toSet().orEmpty()
    val missing = listOf(config.modelFile, config.configFile).filterNot { it in modelAvailable }
    if (missing.isNotEmpty()) {
        throw IllegalStateException(
            "Piper TTS モデルが assets/${config.modelDir}/ に見つかりません: ${missing.joinToString(", ")}"
        )
    }

    // 2. OpenJTalk 辞書（assets 直下のディレクトリ）の存在確認
    val dictEntries = runCatching { assets.list(config.dictAssetDir) }.getOrNull().orEmpty()
    if (dictEntries.isEmpty()) {
        throw IllegalStateException(
            "OpenJTalk 辞書が assets/${config.dictAssetDir}/ に見つかりません"
        )
    }

    // 3. OpenJTalk 辞書のハンドル取得（fromAssets が filesDir/open_jtalk_dic/ へ初回展開）
    val dict = OpenJTalkDictionary.fromAssets(context)

    // 4. G2P エンジン構築
    val g2p = PiperPlusG2p.create(context, dict)

    // 5. config.json を読み込み
    val piperConfig = assets.open("${config.modelDir}/${config.configFile}").use { stream ->
        PiperConfig.parse(stream.bufferedReader().readText())
    }

    // 6. ONNX モデルを filesDir へコピー（assets は直接ファイルパス参照不可）→ OrtSession 作成
    val ortEnv = OrtEnvironment.getEnvironment()
    val modelFile = File(context.filesDir, "${config.modelDir}/${config.modelFile}")
    if (!modelFile.exists()) {
        modelFile.parentFile?.mkdirs()
        assets.open("${config.modelDir}/${config.modelFile}").use { input ->
            modelFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
    val sessionOptions = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(config.numThreads)
    }
    val ortSession = ortEnv.createSession(modelFile.absolutePath, sessionOptions)

    return PiperTtsEngine(g2p, ortEnv, ortSession, piperConfig, config, AudioPlayer())
}
