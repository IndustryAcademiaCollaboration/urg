package com.urg.edge.stt

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File

fun createSttEngine(
    context: Context,
    config: SttConfig = SttConfig(),
    modelDir: File? = null,
): SttEngine {
    val required = listOf(config.encoderFile, config.decoderFile, config.joinerFile, config.tokensFile)
    val missing = if (modelDir != null) {
        required.filterNot { File(modelDir, it).isFile }
    } else {
        val available = runCatching { context.assets.list(config.modelDir) }.getOrNull()?.toSet().orEmpty()
        required.filterNot { it in available }
    }
    if (missing.isNotEmpty()) {
        val location = modelDir?.absolutePath ?: "assets/${config.modelDir}"
        throw IllegalStateException(
            "STTモデルファイルが $location に見つかりません: ${missing.joinToString(", ")}"
        )
    }

    fun path(fileName: String): String {
        return modelDir?.let { File(it, fileName).absolutePath } ?: "${config.modelDir}/$fileName"
    }

    val modelConfig = OfflineModelConfig(
        transducer = OfflineTransducerModelConfig(
            encoder = path(config.encoderFile),
            decoder = path(config.decoderFile),
            joiner = path(config.joinerFile),
        ),
        tokens = path(config.tokensFile),
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
    val recognizer = if (modelDir != null) {
        OfflineRecognizer(config = recognizerConfig)
    } else {
        OfflineRecognizer(
            assetManager = context.assets,
            config = recognizerConfig,
        )
    }
    return SherpaSttEngine(recognizer, config.sampleRate)
}
