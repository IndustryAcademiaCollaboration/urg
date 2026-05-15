package com.urg.edge.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaPipeLlmEngine(
    context: Context,
    modelPath: String,
    private val config: LlmConfig = LlmConfig()
) : LlmEngine {

    private val inference: LlmInference

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(config.maxTokens)
            .setMaxTopK(config.maxTopK)
            .build()
        inference = LlmInference.createFromOptions(context, options)
        Log.d("MediaPipeLlmEngine", "initialized")
    }

    override suspend fun generateStream(
        prompt: String,
        onToken: (partial: String, done: Boolean) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(config.topK)
            .setTopP(config.topP)
            .setTemperature(config.temperature)
            .build()

        val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
        continuation.invokeOnCancellation { session.close() }

        try {
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                onToken(partial, done)
                if (done) {
                    session.close()
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        } catch (e: Exception) {
            session.close()
            continuation.resumeWithException(e)
        }
    }

    override fun close() {
        inference.close()
    }
}
