package com.urg.edge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var llmInference: LlmInference? = null
    private lateinit var retriever: KeywordRetriever

    private var promptText by mutableStateOf("")

    private var messages by mutableStateOf(listOf<Message>())

    private var isLoading by mutableStateOf(false)

    private fun copyModelToInternalStorage(): File {
        val outFile = File(filesDir, "gemma3-1b-it-int4.task")

        if (!outFile.exists()) {
            assets.open("models/gemma3-1b-it-int4.task").use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile
    }

    private fun generateResponse() {
        if (promptText.isBlank()) return
        if (isLoading) return

        messages = messages + Message("user", promptText)
        isLoading = true

        val currentMessages = messages

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val inference = llmInference ?: run {

                    withContext(Dispatchers.Main) {
                        messages = messages + Message("assistant", "ERROR: LLM is not initialized")
                        isLoading = false
                    }
                    return@launch
                }

                val retrievedChunks = retriever.retrieve(currentMessages.last().text, topK = 3)
                val ragSection = if (retrievedChunks.isNotEmpty()) {
                    "\n\n[参考情報]\n" + retrievedChunks.joinToString("\n") { "・${it.title}: ${it.text}" }
                } else ""

                val systemPrompt = """
あなたは災害時支援AIです。
一般市民向けに行動支援を行います。
医療診断は行いません。
必ず日本語で答えてください。
Be concise and brief.
Do not repeat sentences.
[参考情報]が存在する場合は、必ずその内容のみに基づいて回答してください。$ragSection
""".trimIndent()

                val historyPrompt = currentMessages.joinToString("") { msg ->
                    when (msg.role) {
                        "user"      -> "<start_of_turn>user\n${msg.text}<end_of_turn>\n"
                        "assistant" -> "<start_of_turn>model\n${msg.text}<end_of_turn>\n"
                        else        -> ""
                    }
                }
                val fullPrompt = "<start_of_turn>user\n$systemPrompt<end_of_turn>\n$historyPrompt<start_of_turn>model\n"

                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(30) // 次のトークン候補を確率上位K個に絞る設定。大きいと、文脈に無関係なワードが混在しやすくなる
                    .setTopP(0.9f) // 確率の累積が(引数 * 100)%に達するまでの候補のみに絞る設定
                    .setTemperature(0.3f) // 確率分布の「尖り」を調整: 高い:決定的で繰り返し 低い:多様、創造的、ハルシネーション増
                    .build()

                val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
                val response = try {
                    session.addQueryChunk(fullPrompt)
                    session.generateResponse()
                } finally {
                    session.close()
                }

                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", response)
                    isLoading = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages = messages + Message("assistant", "ERROR: ${e.message}")
                    isLoading = false
                }

                Log.e("LLM_RESPONSE", "FAILED", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { state ->
            val roles = state.getStringArrayList("message_roles") ?: emptyList<String>()
            val texts = state.getStringArrayList("message_texts") ?: emptyList<String>()
            if (roles.size == texts.size) {
                messages = roles.zip(texts).map { (role, text) -> Message(role, text) }
            }
        }

        val knowledgeChunks = KnowledgeLoader.load(assets)
        retriever = KeywordRetriever(knowledgeChunks)

        val modelFile = copyModelToInternalStorage()

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(this, options)

            Log.d("LLM_INIT", "SUCCESS")
        } catch (e: Exception) {
            messages = messages + Message("assistant", "ERROR: LLM initialization failed: ${e.message}")
            Log.e("LLM_INIT", "FAILED: ${e.message}", e)
        }

        setContent {
            App(
                prompt = promptText,
                messages = messages,
                isLoading = isLoading,
                onPromptChange = {
                    promptText = it
                },
                onSendClick = {
                    generateResponse()
                }
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("message_roles", ArrayList(messages.map { it.role }))
        outState.putStringArrayList("message_texts", ArrayList(messages.map { it.text }))
    }

    override fun onDestroy() {
        llmInference?.close()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        prompt = "",
        messages = listOf(
            Message("user", "こんにちは"),
            Message("assistant", "Hello")
        ),
        isLoading = false,
        onPromptChange = {},
        onSendClick = {}
    )
}