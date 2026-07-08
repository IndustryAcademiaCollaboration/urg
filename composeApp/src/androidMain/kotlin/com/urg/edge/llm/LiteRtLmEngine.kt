package com.urg.edge.llm

import android.util.Log
import com.google.ai.edge.litertlm.Backend
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

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU()
        )
        engine = Engine(engineConfig)
        engine.initialize()
        Log.d("LiteRtLmEngine", "initialized")
    }

    override suspend fun generateStream(
        prompt: String,
        onToken: (partial: String, done: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Conversation はステートフルで履歴（KVキャッシュ）を内部保持するため、
        // 使い回すとコンテキストが累積し続け、上限超過でネイティブクラッシュする。
        // このアプリは prompt に履歴を毎回組み込む方式なので、生成のたびに
        // 会話を作り直して KV キャッシュをリセットする（＝ステートレス運用）。
        val conv = engine.createConversation()
        try {
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
        } finally {
            conv.close()
        }
    }

    override fun close() {
        if (::engine.isInitialized) engine.close()
    }
}
