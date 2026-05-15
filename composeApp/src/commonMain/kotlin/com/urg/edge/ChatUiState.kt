package com.urg.edge

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val promptText: String = ""
)
