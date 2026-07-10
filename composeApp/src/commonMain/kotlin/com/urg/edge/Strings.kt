package com.urg.edge

object Strings {
    const val ERROR_LLM_NOT_INITIALIZED = "ERROR: LLM is not initialized"
    const val RETRIEVER_INITIALIZING = "知識ベースの初期化中です。しばらくお待ちください。"
    const val ERROR_PREFIX = "ERROR: "

    const val TRIAGE_CALM_INTRO = "大丈夫です。落ち着いて、ゆっくり答えてください。一緒に確認していきましょう。\n\n"
    const val BUTTON_TRIAGE = "トリアージ開始"
    const val BUTTON_YES = "はい"
    const val BUTTON_NO = "いいえ"
    const val BUTTON_MIC = "音声入力"
    const val BUTTON_MIC_LISTENING = "録音中..."
    const val ERROR_TEST_WAV_PREFIX = "テスト認識エラー: "

    const val INIT_LLM_ERROR_PREFIX = "ERROR: LLM initialization failed: "
    const val LLM_MODEL_FILE_NAME = "model.litertlm"
    const val LLM_MODEL_DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    const val INIT_STT_ERROR_PREFIX = "ERROR: STT initialization failed: "
    const val INIT_TTS_ERROR_PREFIX = "ERROR: TTS initialization failed: "
    const val INIT_ERROR_PREFIX = "[初期化エラー] "

    const val ERROR_MIC_PERMISSION = "マイクの使用許可が必要です"
    const val ERROR_STT_NOT_INITIALIZED = "音声認識エンジンの初期化中です"
    const val ERROR_RECORDING_FAILED = "録音の開始に失敗しました"
    const val ERROR_RECOGNITION_FAILED = "音声認識に失敗しました: "
}
