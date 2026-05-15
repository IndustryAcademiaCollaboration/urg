package com.urg.edge.llm

import android.content.Context
import java.io.File

actual class PlatformContext(internal val context: Context)

actual fun createLlmEngine(context: PlatformContext, config: LlmConfig): LlmEngine {
    val androidContext = context.context
    val modelFile = File(androidContext.filesDir, config.modelFileName).also { file ->
        if (!file.exists()) {
            androidContext.assets.open("models/${config.modelFileName}").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
    return MediaPipeLlmEngine(androidContext, modelFile.absolutePath, config)
}
