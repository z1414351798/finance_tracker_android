package com.z.financetracker.entity

data class AiMessage(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val messages: List<AiMessage>
)