package com.example.ai

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,        // "user" atau "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val attachmentContent: String? = null,
    val sources: List<String> = emptyList(),
    val shellOutput: String? = null,
    val webSources: List<WebSource>? = null
)

