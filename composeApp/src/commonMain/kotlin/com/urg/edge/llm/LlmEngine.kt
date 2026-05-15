package com.urg.edge.llm

interface LlmEngine : AutoCloseable {
    suspend fun generateStream(prompt: String, onToken: (partial: String, done: Boolean) -> Unit)
}
