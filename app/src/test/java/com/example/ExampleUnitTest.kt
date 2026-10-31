package com.example.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun webSource_and_webSearchResult_creation() {
        val sources = listOf(
            WebSource(title = "DuckDuckGo", url = "https://duckduckgo.com"),
            WebSource(title = "Android Dev", url = "https://developer.android.com")
        )
        val result = WebSearchResult(
            contextText = "Sample search context",
            sources = sources
        )

        assertEquals("Sample search context", result.contextText)
        assertEquals(2, result.sources.size)
        assertEquals("DuckDuckGo", result.sources[0].title)
        assertEquals("https://duckduckgo.com", result.sources[0].url)
    }

    @Test
    fun chatMessage_with_webSources_serialization() {
        val originalMsg = ChatMessage(
            role = "assistant",
            content = "Jawaban dari internet",
            webSources = listOf(
                WebSource("Wikipedia Android", "https://id.wikipedia.org/wiki/Android"),
                WebSource("Kotlin Lang", "https://kotlinlang.org")
            )
        )

        // Serialize
        val msgObj = JSONObject().apply {
            put("id", originalMsg.id)
            put("role", originalMsg.role)
            put("content", originalMsg.content)
            put("timestamp", originalMsg.timestamp)
            if (!originalMsg.webSources.isNullOrEmpty()) {
                val wsArr = JSONArray()
                for (ws in originalMsg.webSources) {
                    val wsObj = JSONObject().apply {
                        put("title", ws.title)
                        put("url", ws.url)
                    }
                    wsArr.put(wsObj)
                }
                put("webSources", wsArr)
            }
        }

        // Deserialize
        val deserializedWebSources = mutableListOf<WebSource>()
        val webSourcesArray = msgObj.optJSONArray("webSources")
        assertNotNull(webSourcesArray)
        for (w in 0 until webSourcesArray!!.length()) {
            val wsObj = webSourcesArray.optJSONObject(w) ?: continue
            val wsTitle = wsObj.optString("title", "").trim()
            val wsUrl = wsObj.optString("url", "").trim()
            if (wsTitle.isNotEmpty() && wsUrl.isNotEmpty()) {
                deserializedWebSources.add(WebSource(title = wsTitle, url = wsUrl))
            }
        }

        assertEquals(2, deserializedWebSources.size)
        assertEquals("Wikipedia Android", deserializedWebSources[0].title)
        assertEquals("https://id.wikipedia.org/wiki/Android", deserializedWebSources[0].url)
    }
}
