package com.example.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ModelRepository {
    const val MODEL_FILENAME = "model.gguf"
    const val PREF_NAME = "offline_ai_prefs"
    const val KEY_MODEL_URI = "model_uri"
    const val KEY_MODEL_READY = "model_ready"

    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    fun saveModelUri(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_MODEL_URI, uri.toString())
            .putBoolean(KEY_MODEL_READY, true)
            .apply()
    }

    fun getSavedModelUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_MODEL_URI, null)
        return if (!value.isNullOrEmpty()) Uri.parse(value) else null
    }

    fun isModelReady(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MODEL_READY, false)
    }

    suspend fun importModelFromUri(context: Context, sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, MODEL_FILENAME)
        if (target.exists() && target.length() > 0L) {
            saveModelUri(context, sourceUri)
            return@withContext target
        }

        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Tidak bisa membuka file sumber")

        inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }

        saveModelUri(context, sourceUri)
        target
    }

    fun getModelFile(context: Context): File? {
        val file = File(context.filesDir, MODEL_FILENAME)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun clearModel(context: Context) {
        val file = File(context.filesDir, MODEL_FILENAME)
        if (file.exists()) {
            file.delete()
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_MODEL_URI)
            .putBoolean(KEY_MODEL_READY, false)
            .apply()
    }
}
