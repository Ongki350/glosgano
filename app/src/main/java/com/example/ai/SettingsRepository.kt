package com.example.ai

import android.content.Context

object SettingsRepository {
    private const val PREF_NAME = "offline_ai_settings"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_CHATML_TEMPLATE = "chatml_template"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_TOP_K = "top_k"
    private const val KEY_N_CTX = "n_ctx"
    private const val KEY_N_THREADS = "n_threads"
    private const val KEY_APP_THEME = "app_theme"
    private const val KEY_AGENT_SYSTEM_PROMPT = "agent_system_prompt"

    fun load(context: Context): GenerationSettings {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val systemPrompt = prefs.getString(
            KEY_SYSTEM_PROMPT,
            GenerationSettingsDefaults.DEFAULT_SYSTEM_PROMPT
        ) ?: GenerationSettingsDefaults.DEFAULT_SYSTEM_PROMPT

        val agentSystemPrompt = prefs.getString(
            KEY_AGENT_SYSTEM_PROMPT,
            GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
        ) ?: GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT

        val chatmlTemplate = prefs.getString(
            KEY_CHATML_TEMPLATE,
            GenerationSettingsDefaults.DEFAULT_CHATML_TEMPLATE
        ) ?: GenerationSettingsDefaults.DEFAULT_CHATML_TEMPLATE

        val maxTokens = prefs.getInt(
            KEY_MAX_TOKENS,
            GenerationSettingsDefaults.DEFAULT_MAX_TOKENS
        ).coerceIn(32, 800512)

        val temperature = prefs.getFloat(
            KEY_TEMPERATURE,
            GenerationSettingsDefaults.DEFAULT_TEMPERATURE
        ).coerceIn(0.0f, 2.0f)

        val topK = prefs.getInt(
            KEY_TOP_K,
            GenerationSettingsDefaults.DEFAULT_TOP_K
        ).coerceIn(0, 100)

        val nCtx = prefs.getInt(
            KEY_N_CTX,
            GenerationSettingsDefaults.DEFAULT_N_CTX
        ).coerceAtLeast(128)

        val nThreads = prefs.getInt(
            KEY_N_THREADS,
            GenerationSettingsDefaults.DEFAULT_N_THREADS
        ).coerceAtLeast(1)

        val appTheme = prefs.getInt(
            KEY_APP_THEME,
            GenerationSettingsDefaults.DEFAULT_APP_THEME
        ).coerceIn(0, 2)

        return GenerationSettings(
            systemPrompt = systemPrompt,
            chatmlTemplate = chatmlTemplate,
            maxTokens = maxTokens,
            temperature = temperature,
            topK = topK,
            nCtx = nCtx,
            nThreads = nThreads,
            appTheme = appTheme,
            agentSystemPrompt = agentSystemPrompt
        )
    }

    fun save(context: Context, settings: GenerationSettings) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
            .putString(KEY_AGENT_SYSTEM_PROMPT, settings.agentSystemPrompt)
            .putString(KEY_CHATML_TEMPLATE, settings.chatmlTemplate)
            .putInt(KEY_MAX_TOKENS, settings.maxTokens.coerceIn(32, 800512))
            .putFloat(KEY_TEMPERATURE, settings.temperature.coerceIn(0.0f, 2.0f))
            .putInt(KEY_TOP_K, settings.topK.coerceIn(0, 100))
            .putInt(KEY_N_CTX, settings.nCtx.coerceAtLeast(128))
            .putInt(KEY_N_THREADS, settings.nThreads.coerceAtLeast(1))
            .putInt(KEY_APP_THEME, settings.appTheme.coerceIn(0, 2))
            .apply()
    }

    fun getAppTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_APP_THEME, GenerationSettingsDefaults.DEFAULT_APP_THEME).coerceIn(0, 2)
    }

    fun setAppTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_APP_THEME, themeMode.coerceIn(0, 2))
            .apply()
    }

    fun reset(context: Context): GenerationSettings {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        return GenerationSettingsDefaults.createDefault()
    }
}
