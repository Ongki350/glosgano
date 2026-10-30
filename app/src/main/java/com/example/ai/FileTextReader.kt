package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FileTextReader {
    private const val MAX_CHAR_LENGTH = 4000
    private val ALLOWED_EXTENSIONS = listOf(".txt", ".md", ".json", ".csv", ".log")

    suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Tidak bisa membuka file")

        val fullText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (fullText.length > MAX_CHAR_LENGTH) {
            fullText.take(MAX_CHAR_LENGTH) + "\n\n[... teks dipotong ...]"
        } else {
            fullText
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "file.txt"
        } catch (_: Throwable) {
            "file.txt"
        }
    }

    fun isTextFile(uri: Uri): Boolean {
        val pathOrSegment = (uri.lastPathSegment ?: uri.toString()).lowercase()
        return ALLOWED_EXTENSIONS.any { pathOrSegment.endsWith(it) } ||
            pathOrSegment.contains("text")
    }

    fun isTextFile(context: Context, uri: Uri): Boolean {
        val mimeType = try {
            context.contentResolver.getType(uri)?.lowercase()
        } catch (_: Throwable) {
            null
        }
        if (mimeType != null && mimeType.contains("text/")) {
            return true
        }
        val fileName = getFileName(context, uri).lowercase()
        return ALLOWED_EXTENSIONS.any { fileName.endsWith(it) } || isTextFile(uri)
    }
}
