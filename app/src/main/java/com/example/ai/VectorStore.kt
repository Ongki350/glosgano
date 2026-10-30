package com.example.ai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.sqrt

object VectorStore {
    private const val DB_NAME = "offline_ai_vectors.db"
    private const val DB_VERSION = 1
    private const val TABLE_CHUNKS = "chunks"

    @Volatile
    private var dbHelper: VectorDbHelper? = null

    private class VectorDbHelper(context: Context) :
        SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_CHUNKS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kitab TEXT NOT NULL,
                    penulis TEXT NOT NULL,
                    bab TEXT NOT NULL,
                    ayat TEXT NOT NULL,
                    text TEXT NOT NULL,
                    vector BLOB NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_kitab ON $TABLE_CHUNKS(kitab)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CHUNKS")
            onCreate(db)
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (dbHelper == null) {
            dbHelper = VectorDbHelper(context.applicationContext)
        }
    }

    private fun requireDb(context: Context? = null): SQLiteDatabase {
        if (dbHelper == null && context != null) {
            init(context)
        }
        val helper = dbHelper
            ?: throw IllegalStateException("VectorStore belum diinisialisasi. Panggil VectorStore.init(context) terlebih dahulu.")
        return helper.writableDatabase
    }

    fun insertChunks(chunks: List<VectorChunk>) {
        if (chunks.isEmpty()) return
        val db = requireDb()
        db.beginTransaction()
        try {
            for (chunk in chunks) {
                val values = ContentValues().apply {
                    if (chunk.id > 0L) {
                        put("id", chunk.id)
                    }
                    put("kitab", chunk.kitabName)
                    put("penulis", chunk.penulisName)
                    put("bab", chunk.bab)
                    put("ayat", chunk.ayat)
                    put("text", chunk.text)
                    put("vector", floatArrayToByteArray(chunk.vector))
                }
                db.insert(TABLE_CHUNKS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllChunks(): List<VectorChunk> {
        val result = ArrayList<VectorChunk>()
        val helper = dbHelper ?: return result
        val db = helper.readableDatabase
        val cursor = db.query(TABLE_CHUNKS, null, null, null, null, null, null)

        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val kitabIdx = it.getColumnIndexOrThrow("kitab")
            val penulisIdx = it.getColumnIndexOrThrow("penulis")
            val babIdx = it.getColumnIndexOrThrow("bab")
            val ayatIdx = it.getColumnIndexOrThrow("ayat")
            val textIdx = it.getColumnIndexOrThrow("text")
            val vectorIdx = it.getColumnIndexOrThrow("vector")

            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val kitab = it.getString(kitabIdx) ?: ""
                val penulis = it.getString(penulisIdx) ?: ""
                val bab = it.getString(babIdx) ?: ""
                val ayat = it.getString(ayatIdx) ?: ""
                val text = it.getString(textIdx) ?: ""
                val blob = it.getBlob(vectorIdx)
                val vector = if (blob != null) byteArrayToFloatArray(blob) else FloatArray(0)
                result.add(VectorChunk(id, kitab, penulis, bab, ayat, text, vector))
            }
        }
        return result
    }

    fun getIndexedFiles(): List<String> {
        val helper = dbHelper ?: return emptyList()
        val result = ArrayList<String>()
        val db = helper.readableDatabase
        db.rawQuery("SELECT DISTINCT kitab FROM $TABLE_CHUNKS ORDER BY kitab", null).use { cursor ->
            while (cursor.moveToNext()) {
                val kitab = cursor.getString(0)?.trim()
                if (!kitab.isNullOrEmpty()) {
                    result.add(kitab)
                }
            }
        }
        return result
    }

    fun getChunkCountByKitab(kitab: String): Int {
        val helper = dbHelper ?: return 0
        val db = helper.readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_CHUNKS WHERE kitab = ?", arrayOf(kitab)).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun searchSimilar(
        queryVector: FloatArray,
        topK: Int = 5,
        filterKitab: String? = null
    ): List<VectorChunk> {
        Log.d("VectorStore", "=== SEARCH START ===")
        Log.d("VectorStore", "Query size: ${queryVector.size}, filterKitab: $filterKitab")

        if (queryVector.isEmpty()) {
            Log.e("VectorStore", "Query kosong!")
            return emptyList()
        }

        val allChunks = getAllChunks()
        val targetChunks = if (!filterKitab.isNullOrBlank()) {
            allChunks.filter { it.kitabName == filterKitab }
        } else {
            allChunks
        }
        Log.d("VectorStore", "Total chunks: ${allChunks.size}, target chunks: ${targetChunks.size}")

        val scored = ArrayList<Pair<VectorChunk, Float>>()

        for ((index, chunk) in targetChunks.withIndex()) {
            Log.d("VectorStore", "Chunk $index: id=${chunk.id}, vectorSize=${chunk.vector.size}")

            if (chunk.vector.isEmpty()) {
                Log.w("VectorStore", "Chunk ${chunk.id} vektor kosong, skip")
                continue
            }

            if (chunk.vector.size != queryVector.size) {
                Log.w(
                    "VectorStore",
                    "Chunk ${chunk.id} size mismatch: ${chunk.vector.size} vs ${queryVector.size}, skip"
                )
                continue
            }

            val score = cosineSimilarity(queryVector, chunk.vector)
            Log.d("VectorStore", "Chunk ${chunk.id} score: $score")

            if (!score.isNaN() && !score.isInfinite()) {
                scored.add(chunk to score)
            }
        }

        Log.d("VectorStore", "Scored: ${scored.size}")

        val result = scored
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }

        Log.d("VectorStore", "=== SEARCH END: ${result.size} results ===")
        return result
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denom = sqrt(normA) * sqrt(normB)
        if (denom < 1e-8f) return 0f

        return dot / denom
    }

    fun clearAll() {
        val helper = dbHelper ?: return
        val db = helper.writableDatabase
        db.delete(TABLE_CHUNKS, null, null)
    }

    fun getChunkCount(): Int {
        val helper = dbHelper ?: return 0
        val db = helper.readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_CHUNKS", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun floatArrayToByteArray(arr: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(4 * arr.size)
        byteBuffer.asFloatBuffer().put(arr)
        return byteBuffer.array()
    }

    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        if (bytes.isEmpty() || bytes.size % 4 != 0) return FloatArray(0)
        val floatBuffer = ByteBuffer.wrap(bytes).asFloatBuffer()
        val arr = FloatArray(floatBuffer.remaining())
        floatBuffer.get(arr)
        return arr
    }
}
