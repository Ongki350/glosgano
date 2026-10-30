package com.example.ai

object ModelHandleHolder {
    @Volatile
    var handle: Long = 0L

    @Volatile
    var embeddingHandle: Long = 0L

    @Volatile
    var visionHandle: Long = 0L
}
