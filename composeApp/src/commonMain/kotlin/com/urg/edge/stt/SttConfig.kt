package com.urg.edge.stt

data class SttConfig(
    val modelDir: String = "sherpa-onnx-zipformer-ja-reazonspeech-2024-08-01",
    val encoderFile: String = "encoder-epoch-99-avg-1.int8.onnx",
    val decoderFile: String = "decoder-epoch-99-avg-1.onnx",
    val joinerFile: String = "joiner-epoch-99-avg-1.int8.onnx",
    val tokensFile: String = "tokens.txt",
    val sampleRate: Int = 16000,
    val featureDim: Int = 80,
    val numThreads: Int = 2,
)
