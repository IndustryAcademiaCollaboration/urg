package com.urg.edge.llm

import android.content.Context
import java.io.File

actual typealias PlatformContext = Context

actual fun createLlmEngine(context: PlatformContext, config: LlmConfig): LlmEngine {
    val modelFile = File(context.filesDir, config.modelFileName).also { file ->
        if (!file.exists()) {
            context.assets.open("models/${config.modelFileName}").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
    return MediaPipeLlmEngine(context, modelFile.absolutePath, config)
}
