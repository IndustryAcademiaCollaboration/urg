package com.urg.edge.llm

expect class PlatformContext

expect fun createLlmEngine(context: PlatformContext, config: LlmConfig): LlmEngine
