package com.example.ai

import java.util.Locale

object TextChunker {

    fun chunkText(text: String, chunkSize: Int = 500, overlap: Int = 50): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val safeChunkSize = chunkSize.coerceAtLeast(1)
        val safeOverlap = overlap.coerceIn(0, (safeChunkSize - 1).coerceAtLeast(0))
        val step = (safeChunkSize - safeOverlap).coerceAtLeast(1)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size) {
            val end = (start + safeChunkSize).coerceAtMost(words.size)
            val chunk = words.subList(start, end).joinToString(" ")
            if (chunk.isNotBlank()) {
                chunks.add(chunk)
            }
            if (end >= words.size) break
            start += step
        }
        return chunks
    }

    fun detectKitabAndPenulis(fileName: String): Pair<String, String> {
        val withoutExt = fileName.substringBeforeLast('.').trim()
        if (withoutExt.isEmpty()) return "Dokumen" to "Penulis Tidak Diketahui"

        val normalized = withoutExt
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val rawWords = normalized.split(" ").filter { it.isNotBlank() }
        if (rawWords.isEmpty()) return "Dokumen" to "Penulis Tidak Diketahui"

        // Deteksi penulis: cari kata yang mengandung "Penulis" (misal "PenulisA" atau "Penulis A")
        var detectedPenulis = "Penulis Tidak Diketahui"
        val penulisWordIndex = rawWords.indexOfFirst { it.startsWith("Penulis", ignoreCase = true) }
        if (penulisWordIndex >= 0) {
            val word = rawWords[penulisWordIndex]
            if (word.length > 7) {
                val suffix = word.substring(7).trim()
                detectedPenulis = "Penulis $suffix"
            } else if (penulisWordIndex + 1 < rawWords.size) {
                detectedPenulis = "Penulis ${rawWords[penulisWordIndex + 1]}"
            } else {
                detectedPenulis = "Penulis"
            }
        }

        // Jika diawali "Kitab", ambil 2-3 kata sebagai nama kitab (sebelum kata Penulis/bab jika ada)
        val kitabWords = if (rawWords[0].equals("Kitab", ignoreCase = true)) {
            val limit = if (penulisWordIndex > 0) {
                minOf(penulisWordIndex, 3)
            } else {
                val babIndex = rawWords.indexOfFirst { it.startsWith("bab", ignoreCase = true) }
                if (babIndex > 0) minOf(babIndex, 3) else minOf(rawWords.size, 3)
            }
            rawWords.take(limit.coerceAtLeast(2).coerceAtMost(rawWords.size))
        } else {
            rawWords
        }

        val formattedKitab = kitabWords.joinToString(" ") { w ->
            w.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }.ifBlank { withoutExt }

        return formattedKitab to detectedPenulis
    }

    fun detectBabAndAyat(fileName: String, chunkText: String, chunkIndex: Int): Pair<String, String> {
        val babRegex = Regex("""(?i)\bbab[\s_-]*(\d+)""")
        val ayatRegex = Regex("""(?i)\bayat[\s_-]*(\d+)""")

        val babFromText = babRegex.find(chunkText)?.groupValues?.getOrNull(1)
            ?: babRegex.find(fileName)?.groupValues?.getOrNull(1)
        val ayatFromText = ayatRegex.find(chunkText)?.groupValues?.getOrNull(1)

        val babLabel = if (!babFromText.isNullOrBlank()) "Bab $babFromText" else "Bab 1"
        val ayatLabel = if (!ayatFromText.isNullOrBlank()) {
            "Ayat $ayatFromText"
        } else {
            "Bagian ${chunkIndex + 1}"
        }
        return babLabel to ayatLabel
    }
}
