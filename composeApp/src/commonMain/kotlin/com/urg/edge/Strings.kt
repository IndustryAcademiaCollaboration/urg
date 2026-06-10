package com.urg.edge

object Strings {
    const val ERROR_LLM_NOT_INITIALIZED = "ERROR: LLM is not initialized"
    const val RETRIEVER_INITIALIZING = "知識ベースの初期化中です。しばらくお待ちください。"
    const val NO_RELEVANT_INFO = "申し訳ありませんが、その状況に関する情報を持ち合わせていません。近くの救護所または避難所でご確認ください。"
    const val ERROR_PREFIX = "ERROR: "

    const val APP_TITLE = "Local LLM Demo"
    const val LOADING_LABEL = "Loading..."
    const val PROMPT_LABEL = "Prompt"
    const val TRIAGE_CALM_INTRO = "大丈夫です。落ち着いて、ゆっくり答えてください。一緒に確認していきましょう。\n\n"
    const val BUTTON_TRIAGE = "トリアージ開始"
    const val BUTTON_YES = "はい"
    const val BUTTON_NO = "いいえ"
    const val BUTTON_SEND = "送信"
    const val BUTTON_MIC = "音声入力"
    const val BUTTON_MIC_LISTENING = "録音中..."
    const val BUTTON_TEST_WAV_1 = "テストWAV1"
    const val BUTTON_TEST_WAV_2 = "テストWAV2"
    const val ERROR_TEST_WAV_PREFIX = "テスト認識エラー: "
    const val LISTENING_LABEL = "音声入力中..."

    const val INIT_LLM_ERROR_PREFIX = "ERROR: LLM initialization failed: "
    const val INIT_STT_ERROR_PREFIX = "ERROR: STT initialization failed: "
    const val INIT_TTS_ERROR_PREFIX = "ERROR: TTS initialization failed: "
    const val INIT_COMPLETE = "[初期化] 準備完了"
    const val INIT_ERROR_PREFIX = "[初期化エラー] "

    const val ERROR_MIC_PERMISSION = "マイクの使用許可が必要です"
    const val ERROR_STT_NOT_INITIALIZED = "音声認識エンジンの初期化中です"
    const val ERROR_RECORDING_FAILED = "録音の開始に失敗しました"
    const val ERROR_RECOGNITION_FAILED = "音声認識に失敗しました: "

    fun knowledgeBaseLoaded(count: Int): String = "[初期化] 知識ベース読み込み完了 ($count chunks)"
}
