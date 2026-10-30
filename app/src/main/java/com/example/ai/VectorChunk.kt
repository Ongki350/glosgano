package com.example.ai

data class VectorChunk(
    val id: Long = 0L,
    val kitabName: String,        // "Kitab Glosgan"
    val penulisName: String,      // "Penulis A"
    val bab: String,              // "Bab 3"
    val ayat: String,             // "Ayat 20"
    val text: String,             // teks asli
    val vector: FloatArray        // embedding vector
) {
    val kitab: String get() = kitabName
    val penulis: String get() = penulisName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorChunk) return false
        return id == other.id &&
            kitabName == other.kitabName &&
            penulisName == other.penulisName &&
            bab == other.bab &&
            ayat == other.ayat &&
            text == other.text &&
            vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kitabName.hashCode()
        result = 31 * result + penulisName.hashCode()
        result = 31 * result + bab.hashCode()
        result = 31 * result + ayat.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
