package com.example.ai

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class UiState(
    val isModelLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val output: String = "",
    val error: String? = null,
    val statusText: String = "Model belum dimuat",
    val isEmbeddingLoaded: Boolean = false,
    val isIndexing: Boolean = false,
    val isVisionLoaded: Boolean = false
)

class LlamaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val EMBEDDING_MODEL_FILENAME = "embedding_model.gguf"
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MB
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @Volatile
    private var modelHandle: Long = 0L

    @Volatile
    private var embeddingHandle: Long = 0L

    @Volatile
    private var visionHandle: Long = 0L

    init {
        val context = getApplication<Application>()
        VectorStore.init(context)
        val initialStatus = if (ModelRepository.isModelReady(context)) {
            "Model siap. Tekan Load Model."
        } else {
            "Model belum diimpor. Pilih file .gguf dulu."
        }
        _uiState.update {
            it.copy(
                statusText = initialStatus,
                isEmbeddingLoaded = embeddingHandle != 0L || ModelHandleHolder.embeddingHandle != 0L
            )
        }
    }

    fun getModelHandle(): Long = modelHandle

    fun getEmbeddingHandle(): Long = embeddingHandle

    fun getVisionHandle(): Long = visionHandle

    fun isEmbeddingFileReady(): Boolean {
        val file = File(getApplication<Application>().filesDir, EMBEDDING_MODEL_FILENAME)
        return file.exists() && file.length() > 0L
    }

    fun importModel(uri: Uri) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                statusText = "Mengimpor model..."
            )
        }
        viewModelScope.launch {
            try {
                ModelRepository.importModelFromUri(getApplication(), uri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        statusText = "Model siap. Tekan Load Model."
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Gagal mengimpor model."
                    )
                }
            }
        }
    }

    fun loadModel() {
        val current = _uiState.value
        if (current.isLoading || current.isModelLoaded) return

        val modelFile = ModelRepository.getModelFile(getApplication())
        if (modelFile == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isModelLoaded = false,
                    error = "Model belum diimpor. Pilih file .gguf dulu.",
                    statusText = "Model belum diimpor"
                )
            }
            return
        }

        viewModelScope.launch {
            val settings = SettingsRepository.load(getApplication())
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    statusText = "Menginisialisasi native model (${settings.nThreads} threads, ctx ${settings.nCtx})..."
                )
            }

            try {
                val handle = withContext(Dispatchers.Default) {
                    if (!LlamaNativeBridge.isNativeLibLoaded) {
                        throw UnsatisfiedLinkError(
                            LlamaNativeBridge.nativeLoadError
                                ?: "Native library (.so) untuk ABI perangkat ini belum tersedia di jniLibs."
                        )
                    }
                    LlamaNativeBridge.nativeInitModel(
                        modelFile.absolutePath,
                        settings.nThreads,
                        settings.nCtx
                    )
                }

                if (handle == 0L) {
                    ModelHandleHolder.handle = 0L
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isModelLoaded = false,
                            error = "Gagal load model. Coba kurangi Context Size (${settings.nCtx}) atau Jumlah Thread (${settings.nThreads}). Kemungkinan RAM tidak cukup.",
                            statusText = "Gagal load model"
                        )
                    }
                    return@launch
                }

                modelHandle = handle
                ModelHandleHolder.handle = handle
                val sizeMb = modelFile.length() / (1024 * 1024)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isModelLoaded = true,
                        error = null,
                        statusText = "Model siap digunakan (Handle: 0x${handle.toString(16)} • ${sizeMb} MB • ${settings.nThreads} Threads • Ctx ${settings.nCtx})"
                    )
                }
            } catch (t: Throwable) {
                ModelHandleHolder.handle = 0L
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isModelLoaded = false,
                        error = "Error saat load model: ${t.message}. Coba kurangi Context Size atau Thread.",
                        statusText = "Gagal load"
                    )
                }
            }
        }
    }

    fun loadEmbeddingModel(uri: Uri? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    statusText = "Menyiapkan Model Embedding..."
                )
            }

            try {
                val context = getApplication<Application>()
                val targetFile = File(context.filesDir, EMBEDDING_MODEL_FILENAME)

                if (uri != null) {
                    withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("Tidak bisa membuka file model embedding")
                        inputStream.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                }
                                output.flush()
                            }
                        }
                    }
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmbeddingLoaded = false,
                            error = "File embedding_model.gguf belum dipilih. Pilih file .gguf terlebih dahulu.",
                            statusText = "Model Embedding belum dipilih"
                        )
                    }
                    return@launch
                }

                if (embeddingHandle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                    withContext(Dispatchers.Default) {
                        try {
                            LlamaNativeBridge.nativeFreeModel(embeddingHandle)
                        } catch (_: Throwable) {
                        }
                    }
                    embeddingHandle = 0L
                    ModelHandleHolder.embeddingHandle = 0L
                }

                val handle = withContext(Dispatchers.Default) {
                    if (!LlamaNativeBridge.isNativeLibLoaded) {
                        throw UnsatisfiedLinkError(
                            LlamaNativeBridge.nativeLoadError
                                ?: "Native library (.so) belum tersedia di jniLibs."
                        )
                    }
                    LlamaNativeBridge.nativeInitModel(targetFile.absolutePath, 2, 512)
                }

                if (handle == 0L) {
                    embeddingHandle = 0L
                    ModelHandleHolder.embeddingHandle = 0L
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmbeddingLoaded = false,
                            error = "Gagal load model embedding (handle = 0). Kemungkinan RAM tidak cukup.",
                            statusText = "Gagal load model embedding"
                        )
                    }
                    return@launch
                }

                embeddingHandle = handle
                ModelHandleHolder.embeddingHandle = handle
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmbeddingLoaded = true,
                        error = null,
                        statusText = "Model Embedding siap (Handle: 0x${handle.toString(16)})"
                    )
                }
            } catch (t: Throwable) {
                embeddingHandle = 0L
                ModelHandleHolder.embeddingHandle = 0L
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmbeddingLoaded = false,
                        error = "Gagal load model embedding: ${t.message}",
                        statusText = "Error load embedding"
                    )
                }
            }
        }
    }

    fun freeEmbeddingModel() {
        val handle = embeddingHandle
        embeddingHandle = 0L
        ModelHandleHolder.embeddingHandle = 0L

        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if (handle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                        LlamaNativeBridge.nativeFreeModel(handle)
                    }
                }
            } catch (_: Throwable) {
            }
            _uiState.update {
                it.copy(
                    isEmbeddingLoaded = false,
                    statusText = "Model Embedding dibebaskan dari memori"
                )
            }
        }
    }

    fun loadVisionModel(modelPath: String, mmprojPath: String) {
        val current = _uiState.value
        if (current.isLoading || current.isVisionLoaded) return

        viewModelScope.launch {
            val settings = SettingsRepository.load(getApplication())
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    statusText = "Memuat model Vision (VLM)..."
                )
            }

            try {
                if (visionHandle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                    withContext(Dispatchers.Default) {
                        try {
                            LlamaNativeBridge.nativeFreeVision(visionHandle)
                        } catch (_: Throwable) {
                        }
                    }
                    visionHandle = 0L
                    ModelHandleHolder.visionHandle = 0L
                }

                val handle = withContext(Dispatchers.Default) {
                    if (!LlamaNativeBridge.isNativeLibLoaded) {
                        throw UnsatisfiedLinkError(
                            LlamaNativeBridge.nativeLoadError
                                ?: "Native library (.so) belum tersedia di jniLibs."
                        )
                    }
                    LlamaNativeBridge.nativeInitVision(
                        modelPath = modelPath,
                        mmprojPath = mmprojPath,
                        nThreads = settings.nThreads,
                        nCtx = settings.nCtx
                    )
                }

                if (handle == 0L) {
                    visionHandle = 0L
                    ModelHandleHolder.visionHandle = 0L
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isVisionLoaded = false,
                            error = "Gagal memuat Vision Model (handle = 0). Cek file model & mmproj.",
                            statusText = "Gagal load Vision Model"
                        )
                    }
                    return@launch
                }

                visionHandle = handle
                ModelHandleHolder.visionHandle = handle
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isVisionLoaded = true,
                        error = null,
                        statusText = "Vision Model siap (Handle: 0x${handle.toString(16)})"
                    )
                }
            } catch (t: Throwable) {
                visionHandle = 0L
                ModelHandleHolder.visionHandle = 0L
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isVisionLoaded = false,
                        error = "Gagal memuat Vision Model: ${t.message}",
                        statusText = "Error load Vision"
                    )
                }
            }
        }
    }

    fun freeVisionModel() {
        val handle = visionHandle
        visionHandle = 0L
        ModelHandleHolder.visionHandle = 0L

        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    if (handle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                        LlamaNativeBridge.nativeFreeVision(handle)
                    }
                }
            } catch (_: Throwable) {
            }
            _uiState.update {
                it.copy(
                    isVisionLoaded = false,
                    statusText = "Vision Model dibebaskan dari memori"
                )
            }
        }
    }

    fun startIndexingFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val embHandle = if (embeddingHandle != 0L) embeddingHandle else ModelHandleHolder.embeddingHandle
        if (embHandle == 0L) {
            _uiState.update {
                it.copy(error = "Model Embedding belum dimuat. Load Model Embedding terlebih dahulu.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isIndexing = true, error = null) }
            IndexingManager.indexFiles(
                context = getApplication(),
                embeddingHandle = embHandle,
                files = uris,
                onProgress = { progressState ->
                    _uiState.update {
                        it.copy(
                            isIndexing = progressState.isIndexing,
                            error = progressState.error
                        )
                    }
                }
            )
            _uiState.update { it.copy(isIndexing = false) }
        }
    }

    fun generate(prompt: String) {
        val trimmedPrompt = prompt.trim()
        val current = _uiState.value
        if (!current.isModelLoaded || current.isGenerating || trimmedPrompt.isEmpty()) return

        val handle = modelHandle
        if (handle == 0L) {
            _uiState.update {
                it.copy(
                    isModelLoaded = false,
                    error = "Handle model tidak valid (0L). Silakan muat ulang model.",
                    statusText = "Model belum dimuat"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    error = null,
                    statusText = "Sedang menghasilkan respons..."
                )
            }

            val startTime = System.currentTimeMillis()
            try {
                val settings = SettingsRepository.load(getApplication())
                val formatted = ChatTemplate.applyChatML(
                    settings.chatmlTemplate,
                    settings.systemPrompt,
                    prompt
                )
                val result = withContext(Dispatchers.Default) {
                    LlamaNativeBridge.nativeGenerate(
                        handle = handle,
                        prompt = formatted,
                        maxTokens = settings.maxTokens,
                        temperature = settings.temperature,
                        topK = settings.topK
                    )
                }
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        output = result,
                        error = null,
                        statusText = String.format("Generate selesai dalam %.2f detik", elapsedSec)
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = t.localizedMessage ?: t.message ?: "Gagal menjalankan nativeGenerate.",
                        statusText = "Error saat generate teks"
                    )
                }
            }
        }
    }

    fun freeModel() {
        val handle = modelHandle
        if (handle == 0L && !_uiState.value.isModelLoaded) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    statusText = "Membebaskan memori model..."
                )
            }
            try {
                withContext(Dispatchers.Default) {
                    if (handle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                        LlamaNativeBridge.nativeFreeModel(handle)
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = t.localizedMessage ?: t.message ?: "Gagal membebaskan model."
                    )
                }
                return@launch
            } finally {
                modelHandle = 0L
                ModelHandleHolder.handle = 0L
            }

            _uiState.update {
                it.copy(
                    isModelLoaded = false,
                    isLoading = false,
                    isGenerating = false,
                    statusText = "Model dibebaskan dari memori"
                )
            }
        }
    }

    fun clearModel() {
        val handle = modelHandle
        modelHandle = 0L
        ModelHandleHolder.handle = 0L

        viewModelScope.launch {
            if (handle != 0L || _uiState.value.isModelLoaded) {
                try {
                    withContext(Dispatchers.Default) {
                        if (handle != 0L && LlamaNativeBridge.isNativeLibLoaded) {
                            LlamaNativeBridge.nativeFreeModel(handle)
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            withContext(Dispatchers.Default) {
                ModelRepository.clearModel(getApplication())
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isModelLoaded = false,
                    isGenerating = false,
                    error = null,
                    statusText = "Model dihapus"
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        val chatH = modelHandle
        val embH = embeddingHandle
        val visH = visionHandle
        modelHandle = 0L
        embeddingHandle = 0L
        visionHandle = 0L
        ModelHandleHolder.handle = 0L
        ModelHandleHolder.embeddingHandle = 0L
        ModelHandleHolder.visionHandle = 0L
        if ((chatH != 0L || embH != 0L || visH != 0L) && LlamaNativeBridge.isNativeLibLoaded) {
            Thread {
                try {
                    if (chatH != 0L) LlamaNativeBridge.nativeFreeModel(chatH)
                } catch (_: Throwable) {
                }
                try {
                    if (embH != 0L) LlamaNativeBridge.nativeFreeModel(embH)
                } catch (_: Throwable) {
                }
                try {
                    if (visH != 0L) LlamaNativeBridge.nativeFreeVision(visH)
                } catch (_: Throwable) {
                }
            }.start()
        }
        super.onCleared()
    }
}
