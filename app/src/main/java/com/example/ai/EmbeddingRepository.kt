package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

object EmbeddingRepository {

    suspend fun embed(handle: Long, text: String): FloatArray = withContext(Dispatchers.Default) {
        if (handle == 0L) {
            throw IllegalStateException("Model embedding belum dimuat (handle = 0).")
        }
        LlamaNativeBridge.nativeEmbed(handle, text)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0.0f
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in a.indices) {
            val va = a[i]
            val vb = b[i]
            dotProduct += va * vb
            normA += va * va
            normB += vb * vb
        }
        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator <= 1e-12f) return 0.0f
        return (dotProduct / denominator).coerceIn(0.0f, 1.0f)
    }
}
