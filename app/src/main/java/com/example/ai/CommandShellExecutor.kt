package com.example.ai

import android.util.Log
import java.util.concurrent.TimeUnit

data class CommandResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val feedback: String
)

object CommandShellExecutor {
    private const val TAG = "CommandShellExecutor"
    private const val TIMEOUT_SECONDS = 5L

    private val DANGEROUS_PATTERNS = listOf(
        "rm -rf /",
        "rm -rf /*",
        "rm -r /",
        ":(){ :|:& };:",
        "reboot",
        "reboot recovery",
        "reboot bootloader",
        "fastboot",
        "mkfs",
        "format /data",
        "format /system",
        "dd if=/dev/zero",
        "dd if=/dev/urandom"
    )

    fun isValidCommand(command: String): Boolean {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        return DANGEROUS_PATTERNS.none { lower.contains(it) }
    }

    fun execute(command: String, goal: String? = null): CommandResult {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return CommandResult(
                command = command,
                stdout = "",
                stderr = "Perintah kosong.",
                exitCode = -1,
                feedback = "Error: Perintah tidak boleh kosong."
            )
        }

        if (!isValidCommand(trimmed)) {
            val blockedMsg = "Perintah diblokir oleh sistem keamanan Custom Shell (potensi destruktif)."
            return CommandResult(
                command = trimmed,
                stdout = "",
                stderr = blockedMsg,
                exitCode = -1,
                feedback = "Blokir Keamanan: $blockedMsg"
            )
        }

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", trimmed))
            val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                val timeoutMsg = "Timeout eksekusi melebihi $TIMEOUT_SECONDS detik."
                return CommandResult(
                    command = trimmed,
                    stdout = "",
                    stderr = timeoutMsg,
                    exitCode = -1,
                    feedback = "Error: $timeoutMsg"
                )
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.exitValue()

            val feedback = buildFeedback(trimmed, stdout, stderr, exitCode, goal)

            CommandResult(
                command = trimmed,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                feedback = feedback
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal mengeksekusi custom shell: ${t.message}", t)
            process?.destroyForcibly()
            val errMsg = t.message ?: "Gagal menjalankan perintah root."
            CommandResult(
                command = trimmed,
                stdout = "",
                stderr = errMsg,
                exitCode = -1,
                feedback = "Error eksekusi: $errMsg"
            )
        }
    }

    private fun buildFeedback(
        command: String,
        stdout: String,
        stderr: String,
        exitCode: Int,
        goal: String?
    ): String {
        val sb = StringBuilder()
        if (!goal.isNullOrBlank()) {
            sb.append("[Target: $goal]\n")
        }

        when {
            stdout.isNotBlank() && stderr.isNotBlank() -> {
                sb.append("Output:\n$stdout\n\nStderr:\n$stderr (Exit code: $exitCode)")
            }
            stdout.isNotBlank() -> {
                sb.append("Output:\n$stdout (Exit code: $exitCode)")
            }
            stderr.isNotBlank() -> {
                sb.append("Stderr: $stderr (Exit code: $exitCode)")
            }
            exitCode == 0 -> {
                sb.append("Perintah '$command' berhasil dieksekusi (tidak ada output teks - normal, Exit code: 0).")
            }
            else -> {
                sb.append("Perintah '$command' selesai dengan exit code: $exitCode (tidak ada output teks).")
            }
        }
        return sb.toString()
    }
}
