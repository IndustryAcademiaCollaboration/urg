package com.urg.edge.llm

import com.urg.edge.Message

interface ChatTemplate {
    fun formatChatPrompt(systemPrompt: String, messages: List<Message>): String
    fun formatSinglePrompt(systemPrompt: String, userPrompt: String): String
    fun formatInstructionPrompt(instruction: String): String
}
