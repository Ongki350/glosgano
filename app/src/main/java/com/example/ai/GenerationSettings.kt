package com.example.ai

data class GenerationSettings(
    val systemPrompt: String,
    val chatmlTemplate: String,
    val maxTokens: Int,
    val temperature: Float,
    val topK: Int,
    val nCtx: Int = 2048,
    val nThreads: Int = 4,
    val appTheme: Int = 0, // 0 = System Default, 1 = Light, 2 = Dark
    val agentSystemPrompt: String = GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
)

object GenerationSettingsDefaults {
    val DEFAULT_SYSTEM_PROMPT: String = """
        Anda adalah asisten AI yang bermanfaat, jujur, dan ringkas. Jawab hanya pertanyaan terakhir. Abaikan pertanyaan lama kecuali jika terkait dengan pertanyaan terakhir.
    """.trimIndent()

    val DEFAULT_AGENT_SYSTEM_PROMPT: String = """
        ANDA ADALAH AGEN KONTROL LAYAR (AGEN ANDROID).
        Setiap perintah [SHELL] yang Anda kirim akan dieksekusi.
        Anda akan menerima feedback yang sudah dalam bentuk kata-kata manusia.
        Lanjutkan ke perintah berikutnya jika belum selesai.
        Ketik SELESAI jika tujuan tercapai.

        Untuk menjalankan perintah, kembalikan jawaban dengan format:
        [SHELL] perintah_anda_di_sini
        Contoh: [SHELL] input tap 500 1000

        Perintah yang tersedia:
        - input tap X Y
        - input swipe X1 Y1 X2 Y2
        - input text "teks"
        - input keyevent KEYCODE
        - am start -n package/activity
        - screencap -p /sdcard/screenshot.png
        - dumpsys window displays
        - free
        - mkdir path
        Gunakan perintah ini untuk membantu user.
    """.trimIndent()

    val DEFAULT_CHATML_TEMPLATE: String = """
        <|im_start|>system
        {system}<|im_end|>
        <|im_start|>user
        {user}<|im_end|>
        <|im_start|>assistant
        
    """.trimIndent()

    const val DEFAULT_MAX_TOKENS: Int = 800512
    const val DEFAULT_TEMPERATURE: Float = 0.3f
    const val DEFAULT_TOP_K: Int = 40
    const val DEFAULT_N_CTX: Int = 2048
    const val DEFAULT_N_THREADS: Int = 4
    const val DEFAULT_APP_THEME: Int = 0

    fun createDefault(): GenerationSettings {
        return GenerationSettings(
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            chatmlTemplate = DEFAULT_CHATML_TEMPLATE,
            maxTokens = DEFAULT_MAX_TOKENS,
            temperature = DEFAULT_TEMPERATURE,
            topK = DEFAULT_TOP_K,
            nCtx = DEFAULT_N_CTX,
            nThreads = DEFAULT_N_THREADS,
            appTheme = DEFAULT_APP_THEME,
            agentSystemPrompt = DEFAULT_AGENT_SYSTEM_PROMPT
        )
    }
}
