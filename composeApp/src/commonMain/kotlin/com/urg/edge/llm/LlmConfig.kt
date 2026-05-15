package com.urg.edge.llm

data class LlmConfig(
    val modelFileName: String = "gemma3-1b-it-int4.task",
    val chatTemplate: ChatTemplate = GemmaTemplate,
    val topK: Int = 30,
    val topP: Float = 0.9f,
    val temperature: Float = 0.3f,
    val maxTokens: Int = 2048,
    val maxTopK: Int = 40
)
