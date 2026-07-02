package com.urg.edge.model

object VoiceModelAssets {
    const val STT_DIR = "models/stt/sherpa-onnx-zipformer-ja-reazonspeech-2024-08-01"
    const val TTS_DIR = "models/tts/piper/tsukuyomi"

    val sttFiles = listOf(
        RemoteModelFile(
            fileName = "encoder-epoch-99-avg-1.int8.onnx",
            url = "https://huggingface.co/reazon-research/reazonspeech-k2-v2/resolve/main/encoder-epoch-99-avg-1.int8.onnx",
            expectedBytes = 154_670_139L,
        ),
        RemoteModelFile(
            fileName = "decoder-epoch-99-avg-1.onnx",
            url = "https://huggingface.co/reazon-research/reazonspeech-k2-v2/resolve/main/decoder-epoch-99-avg-1.onnx",
            expectedBytes = 11_767_836L,
        ),
        RemoteModelFile(
            fileName = "joiner-epoch-99-avg-1.int8.onnx",
            url = "https://huggingface.co/reazon-research/reazonspeech-k2-v2/resolve/main/joiner-epoch-99-avg-1.int8.onnx",
            expectedBytes = 2_696_970L,
        ),
        RemoteModelFile(
            fileName = "tokens.txt",
            url = "https://huggingface.co/reazon-research/reazonspeech-k2-v2/resolve/main/tokens.txt",
            expectedBytes = 45_754L,
        ),
    )

    val ttsFiles = listOf(
        RemoteModelFile(
            fileName = "tsukuyomi-chan-6lang-fp16.onnx",
            url = "https://huggingface.co/ayousanz/piper-plus-tsukuyomi-chan/resolve/main/tsukuyomi-chan-6lang-fp16.onnx",
            expectedBytes = 39_652_717L,
        ),
        RemoteModelFile(
            fileName = "config.json",
            url = "https://huggingface.co/ayousanz/piper-plus-tsukuyomi-chan/resolve/main/config.json",
            expectedBytes = 6_901L,
        ),
    )
}
