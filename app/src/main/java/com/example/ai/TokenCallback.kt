package com.example.ai

interface TokenCallback {
    fun onToken(token: String)
    fun onComplete(fullText: String)
    fun onError(error: Throwable)
}
