package com.example.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatStorage {
    private const val FILE_NAME = "chat_sessions.json"

    suspend fun loadSessions(context: Context): List<ChatSession> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            return@withContext emptyList()
        }

        try {
            val rawJson = file.readText(Charsets.UTF_8).trim()
            if (rawJson.isEmpty()) return@withContext emptyList()

            val sessionsArray = JSONArray(rawJson)
            val sessions = mutableListOf<ChatSession>()

            for (i in 0 until sessionsArray.length()) {
                val sessionObj = sessionsArray.optJSONObject(i) ?: continue
                val id = sessionObj.optString("id", "")
                if (id.isEmpty()) continue

                val title = sessionObj.optString("title", "Percakapan Baru")
                val createdAt = sessionObj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = sessionObj.optLong("updatedAt", createdAt)
                val executionMode = sessionObj.optString("executionMode", "CUSTOM").ifEmpty { "CUSTOM" }
                val profileName = sessionObj.optString("profileName", "general").ifEmpty { "general" }
                val currentTaskIndex = sessionObj.optInt("currentTaskIndex", 0)

                val taskQueueArray = sessionObj.optJSONArray("taskQueue")
                val taskQueue = mutableListOf<AgentTask>()
                if (taskQueueArray != null) {
                    for (t in 0 until taskQueueArray.length()) {
                        val taskObj = taskQueueArray.optJSONObject(t) ?: continue
                        val taskId = taskObj.optString("id", "")
                        val taskDesc = taskObj.optString("description", "")
                        val isCompleted = taskObj.optBoolean("isCompleted", false)
                        if (taskId.isNotEmpty() && taskDesc.isNotEmpty()) {
                            taskQueue.add(
                                AgentTask(
                                    id = taskId,
                                    description = taskDesc,
                                    isCompleted = isCompleted
                                )
                            )
                        }
                    }
                }

                val messagesArray = sessionObj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()

                for (j in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.optJSONObject(j) ?: continue
                    val msgId = msgObj.optLong("id", System.currentTimeMillis() + j)
                    val role = msgObj.optString("role", "user")
                    val content = msgObj.optString("content", "")
                    val timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())

                    val attachmentName = if (msgObj.has("attachmentName") && !msgObj.isNull("attachmentName")) {
                        msgObj.optString("attachmentName").takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    val attachmentContent = if (msgObj.has("attachmentContent") && !msgObj.isNull("attachmentContent")) {
                        msgObj.optString("attachmentContent").takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }

                    val sourcesList = mutableListOf<String>()
                    val sourcesArray = msgObj.optJSONArray("sources")
                    if (sourcesArray != null) {
                        for (k in 0 until sourcesArray.length()) {
                            val src = sourcesArray.optString(k, "").trim()
                            if (src.isNotEmpty()) {
                                sourcesList.add(src)
                            }
                        }
                    }

                    val shellOutput = if (msgObj.has("shellOutput") && !msgObj.isNull("shellOutput")) {
                        msgObj.optString("shellOutput").takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }

                    val webSourcesList = mutableListOf<WebSource>()
                    val webSourcesArray = msgObj.optJSONArray("webSources")
                    if (webSourcesArray != null) {
                        for (w in 0 until webSourcesArray.length()) {
                            val wsObj = webSourcesArray.optJSONObject(w) ?: continue
                            val wsTitle = wsObj.optString("title", "").trim()
                            val wsUrl = wsObj.optString("url", "").trim()
                            if (wsTitle.isNotEmpty() && wsUrl.isNotEmpty()) {
                                webSourcesList.add(WebSource(title = wsTitle, url = wsUrl))
                            }
                        }
                    }

                    messages.add(
                        ChatMessage(
                            id = msgId,
                            role = role,
                            content = content,
                            timestamp = timestamp,
                            attachmentName = attachmentName,
                            attachmentContent = attachmentContent,
                            sources = sourcesList,
                            shellOutput = shellOutput,
                            webSources = if (webSourcesList.isNotEmpty()) webSourcesList else null
                        )
                    )
                }

                sessions.add(
                    ChatSession(
                        id = id,
                        title = title,
                        messages = messages,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        executionMode = executionMode,
                        profileName = profileName,
                        taskQueue = taskQueue,
                        currentTaskIndex = currentTaskIndex
                    )
                )
            }

            sessions.sortedByDescending { it.updatedAt }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun saveSessions(context: Context, sessions: List<ChatSession>) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        val sessionsArray = JSONArray()

        for (session in sessions) {
            val sessionObj = JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("createdAt", session.createdAt)
                put("updatedAt", session.updatedAt)
                put("executionMode", session.executionMode)
                put("profileName", session.profileName)
                put("currentTaskIndex", session.currentTaskIndex)

                val taskQueueArray = JSONArray()
                for (task in session.taskQueue) {
                    val taskObj = JSONObject().apply {
                        put("id", task.id)
                        put("description", task.description)
                        put("isCompleted", task.isCompleted)
                    }
                    taskQueueArray.put(taskObj)
                }
                put("taskQueue", taskQueueArray)

                val messagesArray = JSONArray()
                for (msg in session.messages) {
                    val msgObj = JSONObject().apply {
                        put("id", msg.id)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("timestamp", msg.timestamp)
                        if (msg.attachmentName != null) {
                            put("attachmentName", msg.attachmentName)
                        }
                        if (msg.attachmentContent != null) {
                            put("attachmentContent", msg.attachmentContent)
                        }
                        if (msg.sources.isNotEmpty()) {
                            val srcArr = JSONArray()
                            msg.sources.forEach { srcArr.put(it) }
                            put("sources", srcArr)
                        }
                        if (msg.shellOutput != null) {
                            put("shellOutput", msg.shellOutput)
                        }
                        if (!msg.webSources.isNullOrEmpty()) {
                            val wsArr = JSONArray()
                            for (ws in msg.webSources) {
                                val wsObj = JSONObject().apply {
                                    put("title", ws.title)
                                    put("url", ws.url)
                                }
                                wsArr.put(wsObj)
                            }
                            put("webSources", wsArr)
                        }
                    }
                    messagesArray.put(msgObj)
                }
                put("messages", messagesArray)
            }
            sessionsArray.put(sessionObj)
        }

        file.writeText(sessionsArray.toString(2), Charsets.UTF_8)
    }

    suspend fun deleteSession(context: Context, sessionId: String) = withContext(Dispatchers.IO) {
        val current = loadSessions(context)
        val filtered = current.filterNot { it.id == sessionId }
        saveSessions(context, filtered)
    }
}
