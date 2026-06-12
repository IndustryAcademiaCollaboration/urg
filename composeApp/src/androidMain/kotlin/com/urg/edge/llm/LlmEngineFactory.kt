package com.urg.edge.llm

suspend fun createLlmEngine(config: LlmConfig): LlmEngine {
    val engine = LiteRtLmEngine(config.modelPath, config)
    engine.initialize()
    return engine
}
