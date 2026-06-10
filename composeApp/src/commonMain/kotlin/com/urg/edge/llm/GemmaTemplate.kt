package com.urg.edge.llm

import com.urg.edge.Message

object GemmaTemplate : ChatTemplate {

    override fun formatChatPrompt(systemPrompt: String, messages: List<Message>): String {
        val history = messages.joinToString("") { msg ->
            when (msg.role) {
                "user"      -> "<start_of_turn>user\n${msg.text}<end_of_turn>\n"
                "assistant" -> "<start_of_turn>model\n${msg.text}<end_of_turn>\n"
                else        -> ""
            }
        }
        return "<start_of_turn>user\n$systemPrompt<end_of_turn>\n$history<start_of_turn>model\n"
    }

    override fun formatSinglePrompt(systemPrompt: String, userPrompt: String): String =
        "<start_of_turn>user\n$systemPrompt<end_of_turn>\n" +
            "<start_of_turn>user\n$userPrompt<end_of_turn>\n" +
            "<start_of_turn>model\n"

    override fun formatInstructionPrompt(instruction: String): String =
        "<start_of_turn>user\n$instruction<end_of_turn>\n<start_of_turn>model\n"
}
