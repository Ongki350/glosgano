package com.example.ai

data class AgentTask(
    val id: String,
    val description: String,
    var isCompleted: Boolean = false
)

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val executionMode: String = "CUSTOM",
    val profileName: String = "general",
    val taskQueue: List<AgentTask> = emptyList(),
    val currentTaskIndex: Int = 0
)
