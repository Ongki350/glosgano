package com.example.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class CommandTemplate(
    val commandPattern: String,
    val successTemplate: String,
    val failureTemplate: String,
    val description: String = "",
    val profileName: String = "general"
)

object CommandDictionary {
    private const val TAG = "CommandDictionary"

    private val _templates = MutableStateFlow<List<CommandTemplate>>(emptyList())
    val templates: StateFlow<List<CommandTemplate>> = _templates.asStateFlow()

    private val _currentProfile = MutableStateFlow("general")
    val currentProfile: StateFlow<String> = _currentProfile.asStateFlow()

    private val _availableProfiles = MutableStateFlow<List<String>>(listOf("general", "whatsapp", "facebook"))
    val availableProfiles: StateFlow<List<String>> = _availableProfiles.asStateFlow()

    private val profileCache = mutableMapOf<String, List<CommandTemplate>>()

    suspend fun loadAvailableProfiles(context: Context): List<String> = withContext(Dispatchers.IO) {
        val profiles = linkedSetOf<String>()
        profiles.add("general")

        // 1. Dari assets/commands/
        try {
            val assetSubdirs = context.assets.list("commands") ?: emptyArray()
            for (sub in assetSubdirs) {
                val files = context.assets.list("commands/$sub")
                if (!files.isNullOrEmpty()) {
                    profiles.add(sub.lowercase())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing asset commands subdirs: ${e.message}")
        }

        // 2. Dari context.filesDir/commands/
        try {
            val userCommandsDir = File(context.filesDir, "commands")
            if (userCommandsDir.exists() && userCommandsDir.isDirectory) {
                userCommandsDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        profiles.add(file.name.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing user commands subdirs: ${e.message}")
        }

        // Default profiles
        profiles.add("whatsapp")
        profiles.add("facebook")

        val sorted = profiles.sortedWith { a, b ->
            when {
                a == "general" -> -1
                b == "general" -> 1
                else -> a.compareTo(b)
            }
        }
        _availableProfiles.value = sorted
        sorted
    }

    suspend fun loadFromAssets(context: Context) {
        loadAvailableProfiles(context)
        loadProfile(context, _currentProfile.value)
    }

    suspend fun loadProfile(context: Context, profileName: String): List<CommandTemplate> = withContext(Dispatchers.IO) {
        val normProfile = profileName.lowercase().trim().ifEmpty { "general" }
        _currentProfile.value = normProfile

        // 1. Baca templates general (fallback)
        val generalTemplates = loadRawProfileTemplates(context, "general")

        // 2. Baca templates spesifik profile jika bukan general
        val specificTemplates = if (normProfile != "general") {
            loadRawProfileTemplates(context, normProfile)
        } else {
            emptyMap()
        }

        // 3. Gabungkan: specific profile meng-override general
        val merged = mutableMapOf<String, CommandTemplate>()
        merged.putAll(generalTemplates)
        merged.putAll(specificTemplates)

        val resultList = merged.values.toList()
        profileCache[normProfile] = resultList
        _templates.value = resultList
        Log.d(TAG, "Loaded profile '$normProfile' with ${resultList.size} templates (${specificTemplates.size} specific, ${generalTemplates.size} general fallback)")
        resultList
    }

    private fun loadRawProfileTemplates(context: Context, profile: String): Map<String, CommandTemplate> {
        val result = mutableMapOf<String, CommandTemplate>()

        // A. Asset: assets/commands/{profile}/
        try {
            val assetFiles = context.assets.list("commands/$profile") ?: emptyArray()
            for (fileName in assetFiles) {
                if (!fileName.endsWith(".json", ignoreCase = true)) continue
                try {
                    val content = context.assets.open("commands/$profile/$fileName").bufferedReader().use { it.readText() }
                    val template = parseTemplate(content, profile)
                    if (template != null) {
                        result[template.commandPattern] = template
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal membaca asset template $profile/$fileName: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengakses assets/commands/$profile: ${e.message}")
        }

        // B. User filesDir: context.filesDir/commands/{profile}/ (prioritas override asset)
        try {
            val userProfileDir = File(File(context.filesDir, "commands"), profile)
            if (userProfileDir.exists() && userProfileDir.isDirectory) {
                val files = userProfileDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) } ?: emptyArray()
                for (file in files) {
                    try {
                        val content = file.readText()
                        val template = parseTemplate(content, profile)
                        if (template != null) {
                            result[template.commandPattern] = template
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal membaca user template $profile/${file.name}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengakses user dir commands/$profile: ${e.message}")
        }

        return result
    }

    private fun parseTemplate(jsonStr: String, profile: String): CommandTemplate? {
        return try {
            val json = JSONObject(jsonStr)
            val pattern = when {
                json.has("command_pattern") -> json.getString("command_pattern")
                json.has("commandPattern") -> json.getString("commandPattern")
                else -> return null
            }
            val success = when {
                json.has("success_template") -> json.getString("success_template")
                json.has("successTemplate") -> json.getString("successTemplate")
                else -> ""
            }
            val failure = when {
                json.has("failure_template") -> json.getString("failure_template")
                json.has("failureTemplate") -> json.getString("failureTemplate")
                else -> ""
            }
            val desc = json.optString("description", "")
            CommandTemplate(
                commandPattern = pattern.trim(),
                successTemplate = success,
                failureTemplate = failure,
                description = desc,
                profileName = profile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parse template JSON: ${e.message}")
            null
        }
    }

    fun findTemplate(command: String, profileName: String = "general"): CommandTemplate? {
        val normProfile = profileName.lowercase().trim().ifEmpty { "general" }
        val templateList = profileCache[normProfile] ?: _templates.value
        val trimmed = command.trim()

        val specificMatch = templateList
            .filter { it.commandPattern != "*" && (trimmed.startsWith(it.commandPattern, ignoreCase = true) || trimmed.contains(it.commandPattern, ignoreCase = true)) }
            .maxByOrNull { it.commandPattern.length }

        if (specificMatch != null) return specificMatch

        return templateList.firstOrNull { it.commandPattern == "*" }
    }

    fun formatFeedback(
        command: String,
        success: Boolean,
        output: String,
        error: String,
        profileName: String = "general"
    ): String {
        val template = findTemplate(command, profileName)
        val rawTemplate = if (template != null) {
            if (success) template.successTemplate else template.failureTemplate
        } else {
            if (success) {
                "✅ Perintah berhasil: `{command}`\nOutput: {output}"
            } else {
                "❌ Perintah gagal: `{command}`\nError: {error}"
            }
        }

        val outText = if (output.isNotBlank()) output.trim() else "tidak ada output"
        val errText = if (error.isNotBlank()) error.trim() else "tidak ada error"

        var formatted = rawTemplate
            .replace("{command}", command.trim())
            .replace("{output}", outText)
            .replace("{error}", errText)

        // {x}, {y} dari input tap X Y
        val tapMatch = Regex("""input\s+tap\s+(\d+)\s+(\d+)""", RegexOption.IGNORE_CASE).find(command)
        if (tapMatch != null) {
            val x = tapMatch.groupValues[1]
            val y = tapMatch.groupValues[2]
            formatted = formatted.replace("{x}", x).replace("{y}", y)
        }

        // {x1}, {y1}, {x2}, {y2} dari input swipe X1 Y1 X2 Y2
        val swipeMatch = Regex("""input\s+swipe\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)""", RegexOption.IGNORE_CASE).find(command)
        if (swipeMatch != null) {
            val x1 = swipeMatch.groupValues[1]
            val y1 = swipeMatch.groupValues[2]
            val x2 = swipeMatch.groupValues[3]
            val y2 = swipeMatch.groupValues[4]
            formatted = formatted
                .replace("{x1}", x1)
                .replace("{y1}", y1)
                .replace("{x2}", x2)
                .replace("{y2}", y2)
        }

        // {app_name} dari am start
        if (formatted.contains("{app_name}")) {
            val appRegex = Regex("""am\s+start\s+(?:.*-n\s+([^\s]+)|.*-a\s+([^\s]+)|(.*))""", RegexOption.IGNORE_CASE).find(command)
            val appName = appRegex?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
                ?: command.removePrefix("am start").trim().ifEmpty { "aplikasi" }
            formatted = formatted.replace("{app_name}", appName)
        }

        // {folder_path} dari mkdir
        if (formatted.contains("{folder_path}")) {
            val folderPath = command
                .replace(Regex("""^mkdir\s+(-p\s+)?""", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifEmpty { "folder" }
            formatted = formatted.replace("{folder_path}", folderPath)
        }

        // {file_path} dari screencap
        if (formatted.contains("{file_path}")) {
            val filePath = command
                .replace(Regex("""^screencap\s+(-p\s+)?""", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifEmpty { "screenshot.png" }
            formatted = formatted.replace("{file_path}", filePath)
        }

        return formatted
    }

    suspend fun importTemplateFile(context: Context, uri: Uri, targetProfile: String = "general"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normProfile = targetProfile.lowercase().trim().ifEmpty { "general" }
            val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(Exception("Tidak dapat membaca file"))

            val parsed = parseTemplate(jsonContent, normProfile)
                ?: return@withContext Result.failure(Exception("Format JSON template tidak valid. Wajib ada 'command_pattern'"))

            val profileDir = File(File(context.filesDir, "commands"), normProfile)
            if (!profileDir.exists()) {
                profileDir.mkdirs()
            }

            val safeName = parsed.commandPattern
                .replace(" ", "_")
                .replace("*", "default")
                .replace(Regex("""[^a-zA-Z0-9_\-]"""), "")
                .ifEmpty { "custom_${System.currentTimeMillis()}" }

            val destFile = File(profileDir, "${safeName}.json")
            destFile.writeText(jsonContent)

            // Reload profile & profile list
            loadAvailableProfiles(context)
            loadProfile(context, normProfile)

            Result.success("Template '${parsed.commandPattern}' berhasil diimpor ke profil '$normProfile'")
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal mengimpor template: ${t.message}", t)
            Result.failure(t)
        }
    }
}
