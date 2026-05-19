package com.urg.edge.stt

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig

fun createSttEngine(context: Context, config: SttConfig = SttConfig()): SttEngine {
    val required = listOf(config.encoderFile, config.decoderFile, config.joinerFile, config.tokensFile)
    val available = runCatching { context.assets.list(config.modelDir) }.getOrNull()?.toSet().orEmpty()
    val missing = required.filterNot { it in available }
    if (missing.isNotEmpty()) {
        throw IllegalStateException(
            "STTモデルファイルが assets/${config.modelDir}/ に見つかりません: ${missing.joinToString(", ")}"
        )
    }

    val modelConfig = OfflineModelConfig(
        transducer = OfflineTransducerModelConfig(
            encoder = "${config.modelDir}/${config.encoderFile}",
            decoder = "${config.modelDir}/${config.decoderFile}",
            joiner = "${config.modelDir}/${config.joinerFile}",
        ),
        tokens = "${config.modelDir}/${config.tokensFile}",
        modelType = "transducer",
        numThreads = config.numThreads,
    )
    val recognizerConfig = OfflineRecognizerConfig(
        featConfig = FeatureConfig(
            sampleRate = config.sampleRate,
            featureDim = config.featureDim,
        ),
        modelConfig = modelConfig,
    )
    val recognizer = OfflineRecognizer(
        assetManager = context.assets,
        config = recognizerConfig,
    )
    return SherpaSttEngine(recognizer, config.sampleRate)
}
