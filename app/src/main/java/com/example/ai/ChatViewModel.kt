package com.example.ai

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.util.UUID

data class ChatListState(
    val sessions: List<ChatSession> = emptyList(),
    val isLoading: Boolean = false,
    val selectedExecutionMode: String = "CUSTOM",
    val selectedProfile: String = "general",
    val availableProfiles: List<String> = listOf("general", "whatsapp", "facebook"),
    val activeProfileTemplateCount: Int = 0,
    val newTaskInput: String = "",
    val isTaskRunning: Boolean = false
)

data class ChatDetailState(
    val session: ChatSession? = null,
    val isGenerating: Boolean = false,
    val streamingText: String? = null,
    val error: String? = null,
    val attachedFileName: String? = null,
    val attachedFileContent: String? = null,
    val attachedImageBytes: ByteArray? = null,
    val attachedImageWidth: Int = 0,
    val attachedImageHeight: Int = 0,
    val attachedImageUri: Uri? = null,
    val selectedKitab: String? = null,
    val availableKitabs: List<String> = emptyList(),
    val isRetrievalOnlyMode: Boolean = false,
    val rootAgentEnabled: Boolean = false,
    val isRootAvailable: Boolean = false,
    val isTaskRunning: Boolean = false,
    val newTaskInput: String = "",
    val isWebSearchEnabled: Boolean = false,
    val isSearchingWeb: Boolean = false,
    val isTurboModeEnabled: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _listState = MutableStateFlow(ChatListState())
    val listState: StateFlow<ChatListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(ChatDetailState())
    val detailState: StateFlow<ChatDetailState> = _detailState.asStateFlow()

    private var currentGenerationJob: Job? = null

    init {
        VectorStore.init(getApplication())
        checkRootStatus()
        viewModelScope.launch {
            val profiles = CommandDictionary.loadAvailableProfiles(getApplication())
            val templates = CommandDictionary.loadProfile(getApplication(), _listState.value.selectedProfile)
            _listState.update {
                it.copy(
                    availableProfiles = profiles,
                    activeProfileTemplateCount = templates.size
                )
            }
        }
    }

    fun checkRootStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasRoot = RootShellExecutor.checkRoot()
            _detailState.update { it.copy(isRootAvailable = hasRoot) }
        }
    }

    fun toggleRootAgent() {
        _detailState.update {
            if (!it.isRootAvailable) {
                it.copy(
                    rootAgentEnabled = false,
                    error = "Akses Root (su) tidak terdeteksi di perangkat ini."
                )
            } else {
                it.copy(rootAgentEnabled = !it.rootAgentEnabled)
            }
        }
    }

    fun toggleWebSearch() {
        _detailState.update { it.copy(isWebSearchEnabled = !it.isWebSearchEnabled) }
    }

    fun toggleTurboMode() {
        _detailState.update {
            if (!it.isRootAvailable) {
                it.copy(
                    isTurboModeEnabled = false,
                    error = "Root tidak tersedia. Turbo Mode membutuhkan akses Root."
                )
            } else {
                it.copy(isTurboModeEnabled = !it.isTurboModeEnabled)
            }
        }
    }

    suspend fun applyTurboBoost() = withContext(Dispatchers.IO) {
        try {
            RootShellExecutor.execute(
                "for i in 0 1 2 3 4 5 6 7; do echo performance > /sys/devices/system/cpu/cpu\$i/cpufreq/scaling_governor 2>/dev/null; done; sync; echo 3 > /proc/sys/vm/drop_caches"
            )
        } catch (t: Throwable) {
            Log.e("ChatViewModel", "Gagal apply turbo boost", t)
        }
    }

    suspend fun revertTurboBoost() = withContext(Dispatchers.IO) {
        try {
            RootShellExecutor.execute(
                "for i in 0 1 2 3 4 5 6 7; do echo schedutil > /sys/devices/system/cpu/cpu\$i/cpufreq/scaling_governor 2>/dev/null; done"
            )
        } catch (t: Throwable) {
            Log.e("ChatViewModel", "Gagal revert turbo boost", t)
        }
    }

    fun setRootAgentEnabled(enabled: Boolean) {
        _detailState.update {
            if (enabled && !it.isRootAvailable) {
                it.copy(
                    rootAgentEnabled = false,
                    error = "Akses Root (su) tidak terdeteksi di perangkat ini."
                )
            } else {
                it.copy(rootAgentEnabled = enabled)
            }
        }
    }

    fun stopRootAgent() {
        _detailState.update {
            it.copy(
                rootAgentEnabled = false,
                isGenerating = false,
                streamingText = null,
                isTaskRunning = false
            )
        }
        _listState.update { it.copy(isTaskRunning = false) }
        currentGenerationJob?.cancel()
        currentGenerationJob = null
    }

    fun updateNewTaskInput(text: String) {
        _listState.update { it.copy(newTaskInput = text) }
        _detailState.update { it.copy(newTaskInput = text) }
    }

    fun addTask(description: String, targetSessionId: String? = null) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return

        val sId = targetSessionId ?: _detailState.value.session?.id ?: return
        val currentSessions = _listState.value.sessions.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == sId }
        val targetSession = if (index >= 0) currentSessions[index] else _detailState.value.session ?: return

        val newTask = AgentTask(
            id = UUID.randomUUID().toString(),
            description = trimmed,
            isCompleted = false
        )
        val updatedQueue = targetSession.taskQueue + newTask
        val updatedSession = targetSession.copy(
            taskQueue = updatedQueue,
            updatedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            currentSessions[index] = updatedSession
        }
        _listState.update { it.copy(sessions = currentSessions, newTaskInput = "") }
        _detailState.update {
            if (it.session?.id == sId) {
                it.copy(session = updatedSession, newTaskInput = "")
            } else {
                it.copy(newTaskInput = "")
            }
        }
        viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), currentSessions)
        }
    }

    fun removeTask(taskId: String, targetSessionId: String? = null) {
        val sId = targetSessionId ?: _detailState.value.session?.id ?: return
        val currentSessions = _listState.value.sessions.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == sId }
        val targetSession = if (index >= 0) currentSessions[index] else _detailState.value.session ?: return

        val updatedQueue = targetSession.taskQueue.filterNot { it.id == taskId }
        val newIndex = targetSession.currentTaskIndex.coerceAtMost(maxOf(0, updatedQueue.size - 1))
        val updatedSession = targetSession.copy(
            taskQueue = updatedQueue,
            currentTaskIndex = if (updatedQueue.isEmpty()) 0 else newIndex,
            updatedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            currentSessions[index] = updatedSession
        }
        _listState.update { it.copy(sessions = currentSessions) }
        _detailState.update {
            if (it.session?.id == sId) {
                it.copy(session = updatedSession)
            } else it
        }
        viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), currentSessions)
        }
    }

    fun clearTasks(targetSessionId: String? = null) {
        val sId = targetSessionId ?: _detailState.value.session?.id ?: return
        val currentSessions = _listState.value.sessions.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == sId }
        val targetSession = if (index >= 0) currentSessions[index] else _detailState.value.session ?: return

        val updatedSession = targetSession.copy(
            taskQueue = emptyList(),
            currentTaskIndex = 0,
            updatedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            currentSessions[index] = updatedSession
        }
        _listState.update { it.copy(sessions = currentSessions) }
        _detailState.update {
            if (it.session?.id == sId) {
                it.copy(session = updatedSession)
            } else it
        }
        viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), currentSessions)
        }
    }

    fun stopTaskQueue() {
        _listState.update { it.copy(isTaskRunning = false) }
        _detailState.update { it.copy(isTaskRunning = false) }
    }

    fun startTaskQueue(sessionId: String) {
        val session = _listState.value.sessions.find { it.id == sessionId }
            ?: _detailState.value.session
            ?: return

        if (session.taskQueue.isEmpty()) {
            _detailState.update { it.copy(error = "Daftar antrean tugas masih kosong. Tambahkan tugas terlebih dahulu.") }
            return
        }

        _listState.update { it.copy(isTaskRunning = true) }
        _detailState.update {
            it.copy(
                rootAgentEnabled = true,
                isTaskRunning = true,
                error = null
            )
        }

        var currentTasks = session.taskQueue
        var targetIndex = session.currentTaskIndex
        if (targetIndex >= currentTasks.size || currentTasks.all { it.isCompleted }) {
            currentTasks = currentTasks.map { it.copy(isCompleted = false) }
            targetIndex = 0
            val resetSession = session.copy(taskQueue = currentTasks, currentTaskIndex = 0)
            val updatedList = _listState.value.sessions.map { if (it.id == sessionId) resetSession else it }
            _listState.update { it.copy(sessions = updatedList) }
            _detailState.update { it.copy(session = resetSession) }
            viewModelScope.launch {
                ChatStorage.saveSessions(getApplication(), updatedList)
            }
        }

        val firstTask = currentTasks[targetIndex]
        val taskPrompt = "Tugas ${targetIndex + 1}: ${firstTask.description}. Berikan perintah [SHELL] untuk mengeksekusinya."

        sendMessage(sessionId, taskPrompt)
    }

    suspend fun loadSessions() {
        _listState.update { it.copy(isLoading = true) }
        val loaded = ChatStorage.loadSessions(getApplication())
        _listState.update {
            it.copy(
                sessions = loaded,
                isLoading = false
            )
        }
        val currentDetailId = _detailState.value.session?.id
        if (currentDetailId != null) {
            val updatedDetailSession = loaded.find { it.id == currentDetailId }
            if (updatedDetailSession != null) {
                _detailState.update { it.copy(session = updatedDetailSession) }
            }
        }
    }

    fun refreshAvailableKitabs() {
        viewModelScope.launch {
            val kitabs = try {
                withContext(Dispatchers.IO) {
                    VectorStore.init(getApplication())
                    VectorStore.getIndexedFiles()
                }
            } catch (_: Throwable) {
                emptyList()
            }
            _detailState.update { state ->
                val validSelected = if (state.selectedKitab != null && state.selectedKitab in kitabs) {
                    state.selectedKitab
                } else {
                    null
                }
                state.copy(
                    availableKitabs = kitabs,
                    selectedKitab = validSelected
                )
            }
        }
    }

    fun setSelectedKitab(kitab: String?) {
        _detailState.update { it.copy(selectedKitab = kitab) }
    }

    fun setRetrievalOnlyMode(enabled: Boolean) {
        _detailState.update { it.copy(isRetrievalOnlyMode = enabled) }
    }

    fun toggleRetrievalOnlyMode() {
        _detailState.update { it.copy(isRetrievalOnlyMode = !it.isRetrievalOnlyMode) }
    }

    fun setExecutionMode(mode: String) {
        _listState.update { it.copy(selectedExecutionMode = mode) }
    }

    fun setProfile(profile: String) {
        val norm = profile.lowercase().trim().ifEmpty { "general" }
        viewModelScope.launch {
            val templates = CommandDictionary.loadProfile(getApplication(), norm)
            _listState.update {
                it.copy(
                    selectedProfile = norm,
                    activeProfileTemplateCount = templates.size
                )
            }
        }
    }

    fun loadAvailableProfiles() {
        viewModelScope.launch {
            val profiles = CommandDictionary.loadAvailableProfiles(getApplication())
            _listState.update { it.copy(availableProfiles = profiles) }
        }
    }

    fun createNewSession(): String {
        val now = System.currentTimeMillis()
        val mode = _listState.value.selectedExecutionMode
        val profile = _listState.value.selectedProfile
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "Percakapan Baru",
            messages = emptyList(),
            createdAt = now,
            updatedAt = now,
            executionMode = mode,
            profileName = profile,
            taskQueue = emptyList(),
            currentTaskIndex = 0
        )
        val updatedList = listOf(newSession) + _listState.value.sessions
        _listState.update { it.copy(sessions = updatedList) }

        val kitabs = try {
            VectorStore.init(getApplication())
            VectorStore.getIndexedFiles()
        } catch (_: Throwable) {
            emptyList()
        }

        _detailState.update {
            it.copy(
                session = newSession,
                streamingText = null,
                error = null,
                attachedFileName = null,
                attachedFileContent = null,
                selectedKitab = null,
                availableKitabs = kitabs,
                isTaskRunning = false
            )
        }

        viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), updatedList)
        }
        return newSession.id
    }

    fun openSession(sessionId: String) {
        viewModelScope.launch {
            val kitabs = try {
                withContext(Dispatchers.IO) {
                    VectorStore.init(getApplication())
                    VectorStore.getIndexedFiles()
                }
            } catch (_: Throwable) {
                emptyList()
            }

            val existing = _listState.value.sessions.find { it.id == sessionId }
            if (existing != null) {
                _detailState.update { state ->
                    val validSelected = if (state.selectedKitab != null && state.selectedKitab in kitabs) {
                        state.selectedKitab
                    } else {
                        null
                    }
                    state.copy(
                        session = existing,
                        error = null,
                        availableKitabs = kitabs,
                        selectedKitab = validSelected
                    )
                }
            } else {
                val allSessions = ChatStorage.loadSessions(getApplication())
                _listState.update { it.copy(sessions = allSessions) }
                val target = allSessions.find { it.id == sessionId }
                _detailState.update { state ->
                    val validSelected = if (state.selectedKitab != null && state.selectedKitab in kitabs) {
                        state.selectedKitab
                    } else {
                        null
                    }
                    state.copy(
                        session = target,
                        error = null,
                        availableKitabs = kitabs,
                        selectedKitab = validSelected
                    )
                }
            }
        }
    }

    fun attachFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val name = withContext(Dispatchers.IO) {
                    FileTextReader.getFileName(context, uri)
                }
                val content = FileTextReader.readTextFromUri(context, uri)
                _detailState.update {
                    it.copy(
                        attachedFileName = name,
                        attachedFileContent = content,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                _detailState.update {
                    it.copy(error = "Gagal baca file: ${t.message}")
                }
            }
        }
    }

    fun clearAttachment() {
        _detailState.update {
            it.copy(
                attachedFileName = null,
                attachedFileContent = null
            )
        }
    }

    fun attachImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val rgbData = withContext(Dispatchers.IO) {
                    val inputStream = if (uri.scheme == "file") {
                        java.io.FileInputStream(java.io.File(uri.path ?: ""))
                    } else {
                        context.contentResolver.openInputStream(uri)
                    } ?: throw IOException("Tidak dapat membaca gambar dari URI")
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (originalBitmap == null) {
                        throw IOException("Gagal mendecode gambar")
                    }

                    // Resize ke ukuran yang wajar untuk VLM (maksimal 768px pada sisi terpanjang)
                    val maxDim = 768
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDim || height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(width, height)
                        val newW = (width * scale).toInt().coerceAtLeast(1)
                        val newH = (height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                    } else {
                        originalBitmap
                    }

                    val finalW = scaledBitmap.width
                    val finalH = scaledBitmap.height
                    val pixels = IntArray(finalW * finalH)
                    scaledBitmap.getPixels(pixels, 0, finalW, 0, 0, finalW, finalH)

                    val rgbBytes = ByteArray(finalW * finalH * 3)
                    var byteIdx = 0
                    for (p in pixels) {
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        rgbBytes[byteIdx++] = r.toByte()
                        rgbBytes[byteIdx++] = g.toByte()
                        rgbBytes[byteIdx++] = b.toByte()
                    }
                    Triple(rgbBytes, finalW, finalH)
                }

                _detailState.update {
                    it.copy(
                        attachedImageBytes = rgbData.first,
                        attachedImageWidth = rgbData.second,
                        attachedImageHeight = rgbData.third,
                        attachedImageUri = uri,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                Log.e("ChatViewModel", "Gagal memuat gambar", t)
                _detailState.update {
                    it.copy(error = "Gagal memuat gambar: ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun attachBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val rgbData = withContext(Dispatchers.IO) {
                    val maxDim = 768
                    val width = bitmap.width
                    val height = bitmap.height
                    val scaledBitmap = if (width > maxDim || height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(width, height)
                        val newW = (width * scale).toInt().coerceAtLeast(1)
                        val newH = (height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                    } else {
                        bitmap
                    }

                    val finalW = scaledBitmap.width
                    val finalH = scaledBitmap.height
                    val pixels = IntArray(finalW * finalH)
                    scaledBitmap.getPixels(pixels, 0, finalW, 0, 0, finalW, finalH)

                    val rgbBytes = ByteArray(finalW * finalH * 3)
                    var byteIdx = 0
                    for (p in pixels) {
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        rgbBytes[byteIdx++] = r.toByte()
                        rgbBytes[byteIdx++] = g.toByte()
                        rgbBytes[byteIdx++] = b.toByte()
                    }

                    val cacheFile = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(cacheFile).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val fileUri = Uri.fromFile(cacheFile)

                    Triple(rgbBytes, finalW, finalH) to fileUri
                }

                _detailState.update {
                    it.copy(
                        attachedImageBytes = rgbData.first.first,
                        attachedImageWidth = rgbData.first.second,
                        attachedImageHeight = rgbData.first.third,
                        attachedImageUri = rgbData.second,
                        error = null
                    )
                }
            } catch (t: Throwable) {
                Log.e("ChatViewModel", "Gagal memproses foto kamera", t)
                _detailState.update {
                    it.copy(error = "Gagal memproses foto kamera: ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun clearAttachedImage() {
        _detailState.update {
            it.copy(
                attachedImageBytes = null,
                attachedImageWidth = 0,
                attachedImageHeight = 0,
                attachedImageUri = null
            )
        }
    }

    private fun buildRagContext(relevantChunks: List<VectorChunk>): String {
        val ragBuilder = StringBuilder()
        ragBuilder.append("=== KONTEKS DARI KITAB GLOSGANISME ===\n\n")
        relevantChunks.forEachIndexed { idx, chunk ->
            val sourceLabel = "${chunk.kitabName}, ${chunk.bab} ${chunk.ayat}".trim()
            ragBuilder.append("[Sumber ${idx + 1} — $sourceLabel]\n")
            ragBuilder.append(chunk.text.trim())
            ragBuilder.append("\n\n")
        }
        ragBuilder.append("=== AKHIR KONTEKS ===")
        return ragBuilder.toString()
    }

    fun sendMessage(
        sessionId: String,
        userText: String,
        filterKitab: String? = _detailState.value.selectedKitab
    ) {
        val trimmedText = userText.trim()
        if (trimmedText.isEmpty() || _detailState.value.isGenerating) return

        val handle = ModelHandleHolder.handle
        val embeddingHandle = ModelHandleHolder.embeddingHandle
        val visionHandle = ModelHandleHolder.visionHandle
        val currentDetail = _detailState.value
        val attachedImgBytes = currentDetail.attachedImageBytes
        val attachedImgW = currentDetail.attachedImageWidth
        val attachedImgH = currentDetail.attachedImageHeight
        val isVisionReady = attachedImgBytes != null && visionHandle != 0L
        val isRetrievalOnly = currentDetail.isRetrievalOnlyMode || (handle == 0L && embeddingHandle != 0L)

        if (attachedImgBytes != null && visionHandle == 0L) {
            try {
                android.widget.Toast.makeText(
                    getApplication(),
                    "Fitur Vision tidak tersedia di perangkat ini (32-bit). Silakan gunakan HP 64-bit.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (_: Throwable) {
            }
            _detailState.update {
                it.copy(
                    error = "Fitur Vision tidak tersedia di perangkat ini (32-bit). Silakan gunakan HP 64-bit."
                )
            }
            return
        }

        if (!isRetrievalOnly && handle == 0L && !isVisionReady) {
            if (embeddingHandle != 0L) {
                _detailState.update { it.copy(isRetrievalOnlyMode = true) }
            } else {
                _detailState.update {
                    it.copy(error = "Model belum dimuat. Buka pengaturan Model dulu.")
                }
                return
            }
        }

        if (isRetrievalOnly && embeddingHandle == 0L) {
            _detailState.update {
                it.copy(error = "Model embedding belum dimuat. Buka pengaturan Model untuk memuat Model Embedding.")
            }
            return
        }

        val activeFilterKitab = filterKitab ?: currentDetail.selectedKitab
        val attachedFileName = currentDetail.attachedFileName
        val attachedFileContent = currentDetail.attachedFileContent

        val currentSessions = _listState.value.sessions.toMutableList()
        val sessionIndex = currentSessions.indexOfFirst { it.id == sessionId }
        val baseSession = if (sessionIndex >= 0) {
            currentSessions[sessionIndex]
        } else {
            currentDetail.session ?: return
        }

        val now = System.currentTimeMillis()
        val attachmentDisplayName = when {
            attachedFileName != null -> attachedFileName
            attachedImgBytes != null -> "📷 Gambar (${attachedImgW}x${attachedImgH})"
            else -> null
        }
        val userMessage = ChatMessage(
            id = now,
            role = "user",
            content = trimmedText,
            timestamp = now,
            attachmentName = attachmentDisplayName,
            attachmentContent = attachedFileContent
        )

        val newTitle = if (baseSession.title == "Percakapan Baru") {
            trimmedText.take(30)
        } else {
            baseSession.title
        }

        val sessionWithUserMsg = baseSession.copy(
            title = newTitle,
            messages = baseSession.messages + userMessage,
            updatedAt = now
        )

        if (sessionIndex >= 0) {
            currentSessions[sessionIndex] = sessionWithUserMsg
        } else {
            currentSessions.add(0, sessionWithUserMsg)
        }
        val sortedAfterUser = currentSessions.sortedByDescending { it.updatedAt }

        _listState.update { it.copy(sessions = sortedAfterUser) }
        _detailState.update {
            it.copy(
                session = sessionWithUserMsg,
                isGenerating = true,
                streamingText = "",
                error = null,
                attachedFileName = null,
                attachedFileContent = null,
                attachedImageBytes = null,
                attachedImageWidth = 0,
                attachedImageHeight = 0,
                attachedImageUri = null
            )
        }

        val isRootAgentActive = currentDetail.rootAgentEnabled || currentDetail.isTaskRunning

        currentGenerationJob = viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), sortedAfterUser)

            if (isRetrievalOnly) {
                try {
                    val result = RetrievalOnlyEngine.searchOnly(
                        context = getApplication(),
                        embeddingHandle = embeddingHandle,
                        query = trimmedText,
                        topK = 3,
                        filterKitab = activeFilterKitab
                    )

                    val finalContent = if (!activeFilterKitab.isNullOrBlank() && result.chunks.isNotEmpty()) {
                        "🔍 Mencari di: $activeFilterKitab\n\n${result.answer}"
                    } else {
                        result.answer
                    }

                    val replyTime = System.currentTimeMillis()
                    val assistantMessage = ChatMessage(
                        id = replyTime,
                        role = "assistant",
                        content = finalContent,
                        timestamp = replyTime,
                        sources = result.sources
                    )

                    val finalSession = sessionWithUserMsg.copy(
                        messages = sessionWithUserMsg.messages + assistantMessage,
                        updatedAt = replyTime
                    )

                    val latestList = _listState.value.sessions.map {
                        if (it.id == sessionId) finalSession else it
                    }.sortedByDescending { it.updatedAt }

                    _listState.update { it.copy(sessions = latestList) }
                    _detailState.update {
                        it.copy(
                            session = finalSession,
                            streamingText = null,
                            isGenerating = false,
                            error = null,
                            attachedFileName = null,
                            attachedFileContent = null
                        )
                    }

                    ChatStorage.saveSessions(getApplication(), latestList)
                } catch (t: Throwable) {
                    _detailState.update {
                        it.copy(
                            streamingText = null,
                            isGenerating = false,
                            error = "Gagal mencari di kitab: ${t.message ?: "Unknown error"}"
                        )
                    }
                }
                return@launch
            }

            val settings = SettingsRepository.load(getApplication())

            // Cek apakah RAG aktif (embeddingHandle != 0 dan VectorStore.getChunkCount() > 0)
            var ragContext: String? = null
            val retrievedSources = mutableListOf<String>()

            val activeEmbeddingHandle = ModelHandleHolder.embeddingHandle
            if (activeEmbeddingHandle != 0L) {
                try {
                    val chunkCount = withContext(Dispatchers.IO) {
                        VectorStore.init(getApplication())
                        VectorStore.getChunkCount()
                    }
                    if (chunkCount > 0) {
                        val queryVector = EmbeddingRepository.embed(activeEmbeddingHandle, trimmedText)
                        val relevantChunks = try {
                            VectorStore.searchSimilar(
                                queryVector = queryVector,
                                topK = 5,
                                filterKitab = activeFilterKitab
                            )
                        } catch (t: Throwable) {
                            Log.e("ChatViewModel", "RAG error", t)
                            emptyList()
                        }

                        ragContext = if (relevantChunks.isNotEmpty()) {
                            for (chunk in relevantChunks) {
                                val displaySource = "${chunk.kitabName} ${chunk.bab.lowercase()} ${chunk.ayat.lowercase()}"
                                if (!retrievedSources.contains(displaySource)) {
                                    retrievedSources.add(displaySource)
                                }
                            }
                            buildRagContext(relevantChunks)
                        } else {
                            null
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("ChatViewModel", "RAG error: ${t.message}", t)
                    ragContext = null
                    retrievedSources.clear()
                }
            }

            var webSearchContext: String? = null
            val currentWebSources = mutableListOf<WebSource>()
            if (currentDetail.isWebSearchEnabled) {
                _detailState.update { it.copy(isSearchingWeb = true, streamingText = "🌐 Mencari di internet...") }
                try {
                    val searchResult = WebSearchRepository.search(trimmedText)
                    if (searchResult.contextText.isNotBlank()) {
                        webSearchContext = searchResult.contextText
                        retrievedSources.add("🌐 Web (DuckDuckGo)")
                        currentWebSources.addAll(searchResult.sources)
                    }
                } catch (t: Throwable) {
                    Log.e("ChatViewModel", "Web search failed", t)
                } finally {
                    _detailState.update { it.copy(isSearchingWeb = false, streamingText = "") }
                }
            }

            val formattedPrompt = buildMultiTurnChatMLPrompt(
                messages = sessionWithUserMsg.messages,
                settings = settings,
                ragContext = ragContext,
                webSearchContext = webSearchContext,
                rootAgentEnabled = isRootAgentActive,
                executionMode = baseSession.executionMode
            )

            val isTurboActive = _detailState.value.isTurboModeEnabled && _detailState.value.isRootAvailable
            if (isTurboActive) {
                applyTurboBoost()
            }

            try {
                // Step 1: Jalankan inferensi AI awal
                val initialRaw = if (isVisionReady && visionHandle != 0L && attachedImgBytes != null) {
                    runNativeVisionInference(
                        handle = visionHandle,
                        prompt = formattedPrompt,
                        imageBytes = attachedImgBytes,
                        imageWidth = attachedImgW,
                        imageHeight = attachedImgH,
                        settings = settings
                    ) { token ->
                        _detailState.update { state ->
                            val accumulated = (state.streamingText ?: "") + token
                            state.copy(streamingText = accumulated.substringBefore("<|im_end|>"))
                        }
                    }
                } else {
                    runNativeInference(handle, formattedPrompt, settings) { token ->
                        _detailState.update { state ->
                            val accumulated = (state.streamingText ?: "") + token
                            state.copy(streamingText = accumulated.substringBefore("<|im_end|>"))
                        }
                    }
                }

                val initialCleaned = initialRaw.substringBefore("<|im_end|>").trim()
                val initialText = if (initialCleaned.isNotEmpty()) initialCleaned else initialRaw.trim()

                var shellOutput: String? = null
                var finalAssistantContent = initialText

                var nextTaskPromptToTrigger: String? = null
                var updatedTaskQueue = baseSession.taskQueue
                var updatedTaskIndex = baseSession.currentTaskIndex

                // Alur Eksekusi Shell untuk Root Agent & Task Queue:
                if (isRootAgentActive && initialText.contains("[SHELL]")) {
                    val shellRegex = Regex("""\[SHELL\]\s*([^\r\n`]+)""")
                    val commands = shellRegex.findAll(initialText)
                        .map { it.groupValues[1].trim() }
                        .filter { it.isNotEmpty() }
                        .toList()

                    if (commands.isNotEmpty()) {
                        val commandDisplay = commands.joinToString("; ")
                        _detailState.update {
                            it.copy(streamingText = "⚙️ Menjalankan perintah: `$commandDisplay`...")
                        }

                        val executedOutputs = mutableListOf<String>()
                        val sessionExecutionMode = baseSession.executionMode
                        for (cmd in commands) {
                            if (!_detailState.value.rootAgentEnabled && !_detailState.value.isTaskRunning) {
                                executedOutputs.add("Eksekusi dibatalkan (STOP ditekan).")
                                break
                            }
                            val out = withContext(Dispatchers.IO) {
                                if (sessionExecutionMode == "CUSTOM") {
                                    val result = CommandShellExecutor.execute(cmd)
                                    CommandDictionary.formatFeedback(
                                        command = cmd,
                                        success = result.exitCode == 0,
                                        output = result.stdout,
                                        error = result.stderr,
                                        profileName = baseSession.profileName
                                    )
                                } else {
                                    RootShellExecutor.execute(cmd)
                                }
                            }
                            executedOutputs.add(out)
                        }

                        val rawShellOutput = if (commands.size == 1) {
                            executedOutputs.first()
                        } else {
                            commands.zip(executedOutputs).joinToString("\n\n") { (c, r) -> "$ $c\n$r" }
                        }
                        shellOutput = rawShellOutput

                        // Cek jika tombol STOP ditekan saat menjalankan shell
                        if (!_detailState.value.rootAgentEnabled && !_detailState.value.isTaskRunning) {
                            _detailState.update {
                                it.copy(
                                    isGenerating = false,
                                    streamingText = null,
                                    isTaskRunning = false
                                )
                            }
                            _listState.update { it.copy(isTaskRunning = false) }
                            return@launch
                        }

                        val isQueueRunning = _detailState.value.isTaskRunning
                        val hasError = rawShellOutput.contains("Error:", ignoreCase = true) ||
                                rawShellOutput.contains("Timeout", ignoreCase = true)

                        if (isQueueRunning) {
                            if (hasError) {
                                // Eksekusi GAGAL: Hentikan antrean
                                _listState.update { it.copy(isTaskRunning = false) }
                                _detailState.update { it.copy(isTaskRunning = false) }
                                finalAssistantContent = "Perintah: `$commandDisplay`\n\n❌ Tugas gagal. Antrean dihentikan. Output: $rawShellOutput"
                            } else {
                                // Eksekusi SUKSES: Tandai selesai, lanjut ke tugas berikutnya
                                val cIdx = baseSession.currentTaskIndex
                                updatedTaskQueue = baseSession.taskQueue.mapIndexed { idx, task ->
                                    if (idx == cIdx) task.copy(isCompleted = true) else task
                                }
                                val nextIdx = cIdx + 1
                                updatedTaskIndex = nextIdx

                                val followUpContent = if (rawShellOutput.contains("\n")) {
                                    "Hasil eksekusi shell:\n$rawShellOutput\nTolong rangkum untuk user."
                                } else {
                                    "Hasil eksekusi shell: $rawShellOutput. Tolong rangkum untuk user."
                                }

                                val followUpMessages = sessionWithUserMsg.messages + listOf(
                                    ChatMessage(role = "assistant", content = initialText),
                                    ChatMessage(role = "user", content = followUpContent)
                                )

                                val summaryPrompt = buildMultiTurnChatMLPrompt(
                                    messages = followUpMessages,
                                    settings = settings,
                                    ragContext = null,
                                    rootAgentEnabled = false,
                                    executionMode = baseSession.executionMode
                                )

                                _detailState.update {
                                    it.copy(streamingText = "Perintah: `$commandDisplay`\n⚙️ Merangkum output...\n\n")
                                }

                                val summaryRaw = try {
                                    runNativeInference(handle, summaryPrompt, settings) { token ->
                                        _detailState.update { state ->
                                            val accumulated = (state.streamingText ?: "") + token
                                            state.copy(streamingText = accumulated.substringBefore("<|im_end|>"))
                                        }
                                    }
                                } catch (_: Throwable) {
                                    "Output shell berhasil: $rawShellOutput"
                                }

                                val summaryCleaned = summaryRaw.substringBefore("<|im_end|>").trim()
                                val finalSummary = if (summaryCleaned.isNotEmpty()) summaryCleaned else summaryRaw.trim()

                                if (nextIdx < updatedTaskQueue.size) {
                                    val nextTask = updatedTaskQueue[nextIdx]
                                    nextTaskPromptToTrigger = "Tugas sebelumnya berhasil. Lanjut ke tugas berikutnya: ${nextTask.description}. Berikan perintah [SHELL] untuk mengeksekusinya."
                                    finalAssistantContent = "Perintah: `$commandDisplay`\n\n$finalSummary"
                                } else {
                                    // Semua tugas telah selesai
                                    _listState.update { it.copy(isTaskRunning = false) }
                                    _detailState.update { it.copy(isTaskRunning = false) }
                                    finalAssistantContent = "Perintah: `$commandDisplay`\n\n$finalSummary\n\n✅ Semua tugas dalam antrean telah diselesaikan."
                                }
                            }
                        } else {
                            // Non-queue single shell execution
                            val followUpContent = if (rawShellOutput.contains("\n")) {
                                "Hasil eksekusi shell:\n$rawShellOutput\nTolong rangkum untuk user."
                            } else {
                                "Hasil eksekusi shell: $rawShellOutput. Tolong rangkum untuk user."
                            }

                            val followUpMessages = sessionWithUserMsg.messages + listOf(
                                ChatMessage(role = "assistant", content = initialText),
                                ChatMessage(role = "user", content = followUpContent)
                            )

                            val summaryPrompt = buildMultiTurnChatMLPrompt(
                                messages = followUpMessages,
                                settings = settings,
                                ragContext = null,
                                rootAgentEnabled = false,
                                executionMode = baseSession.executionMode
                            )

                            _detailState.update {
                                it.copy(streamingText = "Perintah: `$commandDisplay`\n⚙️ Merangkum output...\n\n")
                            }

                            val summaryRaw = try {
                                runNativeInference(handle, summaryPrompt, settings) { token ->
                                    _detailState.update { state ->
                                        val accumulated = (state.streamingText ?: "") + token
                                        state.copy(streamingText = accumulated.substringBefore("<|im_end|>"))
                                    }
                                }
                            } catch (_: Throwable) {
                                "Output shell berhasil: $rawShellOutput"
                            }

                            val summaryCleaned = summaryRaw.substringBefore("<|im_end|>").trim()
                            val finalSummary = if (summaryCleaned.isNotEmpty()) summaryCleaned else summaryRaw.trim()

                            finalAssistantContent = "Perintah: `$commandDisplay`\n\n$finalSummary"
                        }
                    }
                } else if (!activeFilterKitab.isNullOrBlank() && retrievedSources.isNotEmpty()) {
                    finalAssistantContent = "🔍 Mencari di: $activeFilterKitab\n\n$initialText"
                }

                val replyTime = System.currentTimeMillis()
                val assistantMessage = ChatMessage(
                    id = replyTime,
                    role = "assistant",
                    content = finalAssistantContent,
                    timestamp = replyTime,
                    sources = retrievedSources.toList(),
                    shellOutput = shellOutput,
                    webSources = if (currentWebSources.isNotEmpty()) currentWebSources.toList() else null
                )

                val finalSession = sessionWithUserMsg.copy(
                    messages = sessionWithUserMsg.messages + assistantMessage,
                    updatedAt = replyTime,
                    taskQueue = updatedTaskQueue,
                    currentTaskIndex = updatedTaskIndex
                )

                val latestList = _listState.value.sessions.map {
                    if (it.id == sessionId) finalSession else it
                }.sortedByDescending { it.updatedAt }

                _listState.update { it.copy(sessions = latestList) }
                _detailState.update {
                    it.copy(
                        session = finalSession,
                        streamingText = null,
                        isGenerating = false,
                        error = null,
                        attachedFileName = null,
                        attachedFileContent = null
                    )
                }

                ChatStorage.saveSessions(getApplication(), latestList)

                // Jika ada tugas berikutnya dalam antrean yang perlu dijalankan:
                if (nextTaskPromptToTrigger != null && _detailState.value.isTaskRunning) {
                    sendMessage(sessionId, nextTaskPromptToTrigger, activeFilterKitab)
                }

            } catch (_: kotlinx.coroutines.CancellationException) {
                _detailState.update {
                    it.copy(
                        isGenerating = false,
                        streamingText = null,
                        isTaskRunning = false
                    )
                }
                _listState.update { it.copy(isTaskRunning = false) }
            } catch (t: Throwable) {
                _detailState.update {
                    it.copy(
                        streamingText = null,
                        isGenerating = false,
                        isTaskRunning = false,
                        error = t.message ?: "Gagal menghasilkan respons model."
                    )
                }
                _listState.update { it.copy(isTaskRunning = false) }
            } finally {
                if (isTurboActive) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        revertTurboBoost()
                    }
                }
            }
        }
    }

    private suspend fun runNativeInference(
        handle: Long,
        prompt: String,
        settings: GenerationSettings,
        onToken: (String) -> Unit
    ): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        var completed = false
        var failure: Throwable? = null

        val callback = object : TokenCallback {
            override fun onToken(token: String) {
                sb.append(token)
                onToken(token)
            }

            override fun onComplete(fullText: String) {
                completed = true
                if (fullText.isNotEmpty() && sb.isEmpty()) {
                    sb.append(fullText)
                }
            }

            override fun onError(error: Throwable) {
                completed = true
                failure = error
            }
        }

        try {
            LlamaNativeBridge.nativeGenerateStreaming(
                handle = handle,
                prompt = prompt,
                maxTokens = settings.maxTokens,
                temperature = settings.temperature,
                topK = settings.topK,
                callback = callback
            )
        } catch (_: UnsatisfiedLinkError) {
            return@withContext LlamaNativeBridge.nativeGenerate(
                handle = handle,
                prompt = prompt,
                maxTokens = settings.maxTokens,
                temperature = settings.temperature,
                topK = settings.topK
            )
        } catch (t: Throwable) {
            failure = t
        }

        if (failure != null && sb.isEmpty()) {
            try {
                return@withContext LlamaNativeBridge.nativeGenerate(
                    handle = handle,
                    prompt = prompt,
                    maxTokens = settings.maxTokens,
                    temperature = settings.temperature,
                    topK = settings.topK
                )
            } catch (_: Throwable) {
                throw failure ?: Throwable("Gagal menghasilkan respons model.")
            }
        }

        sb.toString()
    }

    private suspend fun runNativeVisionInference(
        handle: Long,
        prompt: String,
        imageBytes: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        settings: GenerationSettings,
        onToken: (String) -> Unit
    ): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        var completed = false
        var failure: Throwable? = null

        val callback = object : TokenCallback {
            override fun onToken(token: String) {
                sb.append(token)
                onToken(token)
            }

            override fun onComplete(fullText: String) {
                completed = true
                if (fullText.isNotEmpty() && sb.isEmpty()) {
                    sb.append(fullText)
                }
            }

            override fun onError(error: Throwable) {
                completed = true
                failure = error
            }
        }

        try {
            LlamaNativeBridge.nativeGenerateVision(
                handle = handle,
                prompt = prompt,
                imageBytes = imageBytes,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                maxTokens = settings.maxTokens,
                temperature = settings.temperature,
                topK = settings.topK,
                callback = callback
            )
        } catch (t: Throwable) {
            failure = t
        }

        if (failure != null && sb.isEmpty()) {
            throw failure ?: Throwable("Gagal menjalankan Vision model.")
        }

        sb.toString()
    }

    private fun buildMultiTurnChatMLPrompt(
        messages: List<ChatMessage>,
        settings: GenerationSettings,
        ragContext: String? = null,
        webSearchContext: String? = null,
        rootAgentEnabled: Boolean = false,
        executionMode: String = "CUSTOM"
    ): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n")
        sb.append(settings.systemPrompt)
        if (rootAgentEnabled) {
            val agentPrompt = settings.agentSystemPrompt.ifBlank {
                GenerationSettingsDefaults.DEFAULT_AGENT_SYSTEM_PROMPT
            }
            if (agentPrompt.isNotBlank()) {
                sb.append("\n\n")
                sb.append(agentPrompt)
            }
        }
        if (!ragContext.isNullOrBlank()) {
            sb.append("\n\n")
            sb.append(ragContext)
        }
        if (!webSearchContext.isNullOrBlank()) {
            sb.append("\n\nKonteks Internet:\n")
            sb.append(webSearchContext)
            sb.append("\n\nGunakan konteks internet berikut untuk menjawab pertanyaan user.")
        }
        sb.append("<|im_end|>\n")
        for (msg in messages) {
            when (msg.role) {
                "user" -> {
                    val promptWithAttachment = if (!msg.attachmentContent.isNullOrEmpty()) {
                        """
                        [Lampiran: ${msg.attachmentName ?: "file.txt"}]
                        ---
                        ${msg.attachmentContent}
                        ---
                        
                        ${msg.content}
                        """.trimIndent()
                    } else {
                        msg.content
                    }
                    sb.append("<|im_start|>user\n")
                    sb.append(promptWithAttachment)
                    sb.append("<|im_end|>\n")
                }
                "assistant" -> {
                    sb.append("<|im_start|>assistant\n")
                    sb.append(msg.content)
                    sb.append("<|im_end|>\n")
                }
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    fun deleteSession(sessionId: String) {
        val updated = _listState.value.sessions.filterNot { it.id == sessionId }
        _listState.update { it.copy(sessions = updated) }
        if (_detailState.value.session?.id == sessionId) {
            _detailState.update {
                it.copy(
                    session = null,
                    streamingText = null,
                    error = null,
                    attachedFileName = null,
                    attachedFileContent = null,
                    isTaskRunning = false
                )
            }
        }
        viewModelScope.launch {
            ChatStorage.deleteSession(getApplication(), sessionId)
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return

        val now = System.currentTimeMillis()
        val updated = _listState.value.sessions.map { session ->
            if (session.id == sessionId) {
                session.copy(title = trimmed, updatedAt = now)
            } else {
                session
            }
        }.sortedByDescending { it.updatedAt }

        _listState.update { it.copy(sessions = updated) }
        if (_detailState.value.session?.id == sessionId) {
            val updatedDetail = updated.find { it.id == sessionId }
            _detailState.update { it.copy(session = updatedDetail) }
        }

        viewModelScope.launch {
            ChatStorage.saveSessions(getApplication(), updated)
        }
    }

    fun clearDetailError() {
        _detailState.update { it.copy(error = null) }
    }
}
