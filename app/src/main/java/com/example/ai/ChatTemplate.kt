package com.example.ai

object ChatTemplate {
    fun applyChatML(
        template: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        return template
            .replace("{system}", systemPrompt)
            .replace("{user}", userPrompt)
    }

    fun applyChatML(systemPrompt: String, userPrompt: String): String {
        return applyChatML(
            template = GenerationSettingsDefaults.DEFAULT_CHATML_TEMPLATE,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt
        )
    }
}
