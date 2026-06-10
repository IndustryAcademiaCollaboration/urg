package com.urg.edge.llm

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

class LiteRtLmEngine(
    private val modelPath: String,
    private val config: LlmConfig
) : LlmEngine {

    private lateinit var engine: Engine
    private var conversation: Conversation? = null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU()
        )
        engine = Engine(engineConfig)
        engine.initialize()
        conversation = engine.createConversation()
        Log.d("LiteRtLmEngine", "initialized")
    }

    override suspend fun generateStream(
        prompt: String,
        onToken: (partial: String, done: Boolean) -> Unit
    ) {
        val conv = conversation ?: error("LiteRtLmEngine not initialized")
        var accumulated = ""
        // sendMessageAsync が累積テキストを emit するか差分トークンを emit するかは
        // APIの実装依存のため、どちらでも正しく動作するよう差分を計算して渡す
        conv.sendMessageAsync(prompt)
            .catch { e -> throw e }
            .collect { message ->
                val fullText = message.toString()
                val delta = fullText.removePrefix(accumulated)
                accumulated = fullText
                onToken(delta, false)
            }
        onToken("", true)
    }

    override fun close() {
        conversation?.close()
        conversation = null
        if (::engine.isInitialized) engine.close()
    }
}
