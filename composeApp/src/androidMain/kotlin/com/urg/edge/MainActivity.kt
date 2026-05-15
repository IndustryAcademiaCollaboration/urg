package com.urg.edge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.urg.edge.llm.LlmConfig
import com.urg.edge.llm.PlatformContext
import com.urg.edge.llm.createLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initLlmEngine()
        initKnowledgeRetriever()

        setContent {
            App(chatViewModel)
        }
    }

    private fun initLlmEngine() {
        try {
            val config = LlmConfig()
            chatViewModel.setLlmEngine(createLlmEngine(PlatformContext(this), config), config)
            Log.d("LLM_INIT", "SUCCESS")
        } catch (e: Exception) {
            chatViewModel.addSystemMessage("ERROR: LLM initialization failed: ${e.message}")
            Log.e("LLM_INIT", "FAILED", e)
        }
    }

    private fun initKnowledgeRetriever() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chunks = KnowledgeLoader.load(assets)
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("[初期化] 知識ベース読み込み完了 (${chunks.size} chunks)")
                }
                val retriever = EmbeddingRetriever(this@MainActivity, chunks)
                chatViewModel.setRetriever(retriever)
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("[初期化] 準備完了")
                }
                Log.d("EMBEDDING_INIT", "SUCCESS")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatViewModel.addSystemMessage("[初期化エラー] ${e::class.simpleName}: ${e.message}")
                }
                Log.e("EMBEDDING_INIT", "FAILED", e)
            }
        }
    }
}
