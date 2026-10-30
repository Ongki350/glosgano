package com.example.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class IndexingState(
    val isIndexing: Boolean = false,
    val currentFile: String = "",
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val processedFiles: Int = 0,
    val totalFiles: Int = 0,
    val error: String? = null
)

object IndexingManager {
    private val _indexingState = MutableStateFlow(IndexingState())
    val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    suspend fun indexFiles(
        context: Context,
        embeddingHandle: Long,
        files: List<Uri>,
        onProgress: (IndexingState) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        if (files.isEmpty()) return@withContext
        if (embeddingHandle == 0L) {
            val errState = IndexingState(
                isIndexing = false,
                error = "Model Embedding belum dimuat. Load Model Embedding terlebih dahulu."
            )
            _indexingState.value = errState
            onProgress(errState)
            return@withContext
        }

        VectorStore.init(context)

        val startState = IndexingState(
            isIndexing = true,
            currentFile = "",
            currentChunk = 0,
            totalChunks = 0,
            processedFiles = 0,
            totalFiles = files.size,
            error = null
        )
        _indexingState.value = startState
        onProgress(startState)

        try {
            for ((fileIndex, uri) in files.withIndex()) {
                val fileName = withContext(Dispatchers.IO) {
                    FileTextReader.getFileName(context, uri)
                }

                // Terima semua file .txt / teks tanpa filter nama "Kitab_*"
                val fullText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: FileTextReader.readTextFromUri(context, uri)
                }

                val (kitabName, penulisName) = TextChunker.detectKitabAndPenulis(fileName)
                val textChunks = TextChunker.chunkText(fullText, chunkSize = 500, overlap = 50)

                val fileInitialState = _indexingState.value.copy(
                    isIndexing = true,
                    currentFile = fileName,
                    currentChunk = 0,
                    totalChunks = textChunks.size,
                    processedFiles = fileIndex + 1,
                    totalFiles = files.size,
                    error = null
                )
                _indexingState.value = fileInitialState
                onProgress(fileInitialState)

                for ((chunkIdx, chunkContent) in textChunks.withIndex()) {
                    val vector = EmbeddingRepository.embed(embeddingHandle, chunkContent)
                    val (bab, ayat) = TextChunker.detectBabAndAyat(fileName, chunkContent, chunkIdx)

                    val vectorChunk = VectorChunk(
                        id = 0L,
                        kitabName = kitabName,
                        penulisName = penulisName,
                        bab = bab,
                        ayat = ayat,
                        text = chunkContent,
                        vector = vector
                    )

                    VectorStore.insertChunks(listOf(vectorChunk))

                    val progressState = _indexingState.value.copy(
                        isIndexing = true,
                        currentFile = fileName,
                        currentChunk = chunkIdx + 1,
                        totalChunks = textChunks.size,
                        processedFiles = fileIndex + 1,
                        totalFiles = files.size,
                        error = null
                    )
                    _indexingState.value = progressState
                    onProgress(progressState)
                }
            }

            val completedState = _indexingState.value.copy(
                isIndexing = false,
                error = null
            )
            _indexingState.value = completedState
            onProgress(completedState)
        } catch (t: Throwable) {
            val errorState = _indexingState.value.copy(
                isIndexing = false,
                error = t.localizedMessage ?: t.message ?: "Gagal melakukan indexing file."
            )
            _indexingState.value = errorState
            onProgress(errorState)
        }
    }

    fun resetError() {
        _indexingState.update { it.copy(error = null) }
    }
}
