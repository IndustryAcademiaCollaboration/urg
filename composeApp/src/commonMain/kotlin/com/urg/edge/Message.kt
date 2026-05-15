package com.urg.edge

enum class MessageType { CHAT, TRIAGE, SYSTEM }

data class Message(
    val role: String,
    val text: String,
    val type: MessageType = MessageType.CHAT
)
