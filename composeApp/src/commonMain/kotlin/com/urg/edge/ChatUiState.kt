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
)
