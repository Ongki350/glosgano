package com.example.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RetrievalOnlyEngine {

    data class RetrievalResult(
        val chunks: List<VectorChunk>,
        val answer: String,
        val sources: List<String>
    )

    suspend fun searchOnly(
        context: Context,
        embeddingHandle: Long,
        query: String,
        topK: Int = 3,
        filterKitab: String? = null
    ): RetrievalResult = withContext(Dispatchers.Default) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = "Pertanyaan tidak boleh kosong.",
                sources = emptyList()
            )
        }

        if (embeddingHandle == 0L) {
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = "Model embedding belum dimuat. Silakan muat model embedding terlebih dahulu di pengaturan Model.",
                sources = emptyList()
            )
        }

        withContext(Dispatchers.IO) {
            VectorStore.init(context)
        }

        val totalChunks = withContext(Dispatchers.IO) {
            try {
                VectorStore.getChunkCount()
            } catch (t: Throwable) {
                Log.e("RetrievalOnlyEngine", "Gagal membaca chunk count", t)
                0
            }
        }

        if (totalChunks == 0) {
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = "Belum ada dokumen atau kitab yang diindeks. Silakan lakukan indeks file .txt di Pengaturan Lanjutan terlebih dahulu.",
                sources = emptyList()
            )
        }

        val queryVector = try {
            EmbeddingRepository.embed(embeddingHandle, trimmedQuery)
        } catch (t: Throwable) {
            Log.e("RetrievalOnlyEngine", "Gagal embed query", t)
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = "Gagal menghasilkan embedding: ${t.message ?: "Unknown error"}",
                sources = emptyList()
            )
        }

        val chunks = try {
            withContext(Dispatchers.IO) {
                VectorStore.searchSimilar(
                    queryVector = queryVector,
                    topK = topK,
                    filterKitab = filterKitab
                )
            }
        } catch (t: Throwable) {
            Log.e("RetrievalOnlyEngine", "Gagal searchSimilar", t)
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = "Gagal mencari di database vektor: ${t.message ?: "Unknown error"}",
                sources = emptyList()
            )
        }

        if (chunks.isEmpty()) {
            val emptyMsg = if (!filterKitab.isNullOrBlank()) {
                "Tidak ditemukan kutipan atau ayat yang relevan di kitab \"$filterKitab\"."
            } else {
                "Tidak ditemukan kutipan atau ayat yang relevan di dalam kitab yang diindeks."
            }
            return@withContext RetrievalResult(
                chunks = emptyList(),
                answer = emptyMsg,
                sources = emptyList()
            )
        }

        val sources = chunks.map { chunk ->
            listOf(chunk.kitabName, chunk.bab.lowercase(), chunk.ayat.lowercase())
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }.distinct()

        val answer = if (chunks.size == 1) {
            // Kalau 1 chunk: tampilkan langsung
            chunks[0].text.trim()
        } else {
            // Kalau >1 chunk: tampilkan dengan format: Menurut kitab:\n\n1. ${chunk1.kitab} ...
            val sb = StringBuilder()
            sb.append("Menurut kitab:\n\n")
            chunks.forEachIndexed { index, chunk ->
                val ref = listOf(chunk.kitab, chunk.bab, chunk.ayat)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                sb.append("${index + 1}. $ref:\n")
                sb.append("\"${chunk.text.trim()}\"\n\n")
            }
            sb.toString().trimEnd()
        }

        RetrievalResult(
            chunks = chunks,
            answer = answer,
            sources = sources
        )
    }
}
