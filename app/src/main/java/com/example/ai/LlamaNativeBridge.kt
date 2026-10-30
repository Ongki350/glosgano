package com.example.ai

import android.util.Log

object LlamaNativeBridge {
    private const val TAG = "LlamaNativeBridge"

    @Volatile
    var isNativeLibLoaded: Boolean = false
        private set

    var nativeLoadError: String? = null
        private set

    init {
        // 1. Coba muat 7 library (ARM64 dengan Vision)
        val libsVision = listOf(
            "llama", "ggml", "ggml-base", "ggml-cpu",
            "llama-common", "mtmd", "llama-jni"
        )

        // 2. Library fallback untuk ARM32 (Chat biasa)
        val libsBasic = listOf(
            "llama", "ggml", "ggml-base", "ggml-cpu", "llama-jni"
        )

        try {
            // Coba muat versi Vision dulu
            for (lib in libsVision) {
                System.loadLibrary(lib)
            }
            isNativeLibLoaded = true
            Log.i(TAG, "Loaded 7 libraries (Vision Mode enabled)")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Vision libraries not found. Falling back to 5 libraries (Chat Only).")
            try {
                // Fallback ke versi Chat biasa
                for (lib in libsBasic) {
                    System.loadLibrary(lib)
                }
                isNativeLibLoaded = true
                Log.i(TAG, "Loaded 5 libraries (Chat Only Mode)")
            } catch (e2: UnsatisfiedLinkError) {
                nativeLoadError = e2.message ?: "Gagal memuat native library (.so)"
                Log.e(TAG, "UnsatisfiedLinkError loading basic libraries: ${e2.message}", e2)
            }
        } catch (e: Throwable) {
            nativeLoadError = e.message ?: "Error tidak diketahui saat memuat library native"
            Log.e(TAG, "Error loading native libraries: ${e.message}", e)
        }
    }

    external fun nativeInitModel(modelPath: String, nThreads: Int, nCtx: Int): Long

    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int
    ): String

    external fun nativeGenerateStreaming(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        callback: TokenCallback
    )

    external fun nativeEmbed(handle: Long, text: String): FloatArray

    external fun nativeFreeModel(handle: Long)

    external fun nativeInitVision(modelPath: String, mmprojPath: String, nThreads: Int, nCtx: Int): Long

    external fun nativeGenerateVision(
        handle: Long,
        prompt: String,
        imageBytes: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        callback: TokenCallback
    )

    external fun nativeFreeVision(handle: Long)
}
