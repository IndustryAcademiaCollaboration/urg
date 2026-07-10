package com.urg.edge

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val promptText: String = "",
    val isListening: Boolean = false,
    val showTriageButtons: Boolean = false,
    val triageFlowText: String = "",
    val isTriageFlowLoading: Boolean = false,
    val micAmplitude: Float = 0f,
    // モデル初期化バナー
    val llmProgress: Float = 0f,
    val sttProgress: Float = 0f,
    val ttsProgress: Float = 0f,
    val showInitBanner: Boolean = false,
    val initBannerExpanded: Boolean = true,
    // チャットスコープ
    val chatScope: ChatScope = ChatScope.All,
    // 地図遷移の確認待ち対象者ID（null = 非表示）
    val pendingMapNavVictimId: String? = null,
)
