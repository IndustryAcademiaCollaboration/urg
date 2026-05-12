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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private var llmInference: LlmInference? = null

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

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val inference = llmInference ?: run {

                    withContext(Dispatchers.Main) {
                        messages = messages + Message("assistant", "ERROR: LLM is not initialized")
                        isLoading = false
                    }
                    return@launch
                }

                val response = inference.generateResponse(promptText)

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

        val modelFile = copyModelToInternalStorage()

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(128)
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
                response = messages.lastOrNull { it.role == "assistant" }?.text ?: "",
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
        response = "Hello",
        isLoading = false,
        onPromptChange = {},
        onSendClick = {}
    )
}