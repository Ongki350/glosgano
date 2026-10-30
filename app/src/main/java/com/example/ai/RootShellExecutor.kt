package com.example.ai

import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

object RootShellExecutor {
    private const val TAG = "RootShellExecutor"
    private const val TIMEOUT_SECONDS = 5L

    fun checkRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exited = process.waitFor(2, TimeUnit.SECONDS)
            if (exited && process.exitValue() == 0) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                output.contains("uid=0")
            } else {
                process.destroyForcibly()
                checkSuBinaryExists()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "checkRoot failed via su: ${e.message}")
            checkSuBinaryExists()
        }
    }

    private fun checkSuBinaryExists(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    fun execute(command: String): String {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return "Perintah kosong."

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", trimmed))
            val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return "Error: Timeout (melebihi 5 detik)."
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.exitValue()

            when {
                output.isNotBlank() && error.isNotBlank() -> "$output\n$error"
                output.isNotBlank() -> output
                error.isNotBlank() -> error
                exitCode == 0 -> "Perintah berhasil dieksekusi tanpa output."
                else -> "Perintah selesai dengan kode keluar: $exitCode."
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Eksekusi shell error: ${t.message}", t)
            process?.destroyForcibly()
            "Error: ${t.message ?: "Gagal menjalankan perintah root."}"
        }
    }
}
