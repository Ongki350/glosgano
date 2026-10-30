package com.example.ai

import android.os.Build
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

data class WebSource(
    val title: String,
    val url: String
)

data class WebSearchResult(
    val contextText: String,
    val sources: List<WebSource> = emptyList()
)

object WebSearchRepository {

    suspend fun search(query: String, maxResults: Int = 4): WebSearchResult = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@withContext WebSearchResult("")

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val targetUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val url = URL(targetUrl)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext WebSearchResult("")
            }

            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val (items, sources) = extractResultsAndSources(html, maxResults)

            if (items.isEmpty()) {
                return@withContext WebSearchResult("")
            }

            val sb = StringBuilder()
            sb.append("Hasil Pencarian Internet untuk '$trimmedQuery':\n")
            items.forEachIndexed { index, (title, snippet) ->
                val line = if (title.isNotBlank() && snippet.isNotBlank()) {
                    "${index + 1}. $title — $snippet"
                } else if (title.isNotBlank()) {
                    "${index + 1}. $title"
                } else {
                    "${index + 1}. $snippet"
                }
                sb.append(line).append("\n")
            }

            WebSearchResult(
                contextText = sb.toString().trim(),
                sources = sources.take(3)
            )
        } catch (_: Throwable) {
            WebSearchResult("")
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun extractResultsAndSources(
        html: String,
        maxResults: Int
    ): Pair<List<Pair<String, String>>, List<WebSource>> {
        val items = mutableListOf<Pair<String, String>>()
        val sources = mutableListOf<WebSource>()

        val fullTitleTagRegex = Regex(
            """<a\s+([^>]*class="[^"]*result__a[^"]*"[^>]*)>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val snippetRegex = Regex(
            """<a\s+[^>]*class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val hrefAttrRegex = Regex("""href="([^"]+)"""")

        val matchedTitles = fullTitleTagRegex.findAll(html).toList()
        val snippets = snippetRegex.findAll(html)
            .map { cleanHtml(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()

        val count = minOf(maxResults, maxOf(matchedTitles.size, snippets.size))
        for (i in 0 until count) {
            val titleMatch = matchedTitles.getOrNull(i)
            val rawTitle = titleMatch?.groupValues?.getOrNull(2) ?: ""
            val attributes = titleMatch?.groupValues?.getOrNull(1) ?: ""
            val title = cleanHtml(rawTitle)

            val rawHref = hrefAttrRegex.find(attributes)?.groupValues?.getOrNull(1) ?: ""
            val cleanUrl = decodeDuckDuckGoUrl(rawHref)

            val snippet = snippets.getOrNull(i) ?: ""

            if (title.isNotBlank() || snippet.isNotBlank()) {
                items.add(title to snippet)
            }

            if (cleanUrl.isNotBlank() && title.isNotBlank() && sources.none { it.url == cleanUrl }) {
                sources.add(WebSource(title = title, url = cleanUrl))
            }
        }

        return items to sources
    }

    private fun decodeDuckDuckGoUrl(rawHref: String): String {
        if (rawHref.isBlank()) return ""
        var href = rawHref.trim()
        if (href.startsWith("//")) {
            href = "https:$href"
        }

        if (href.contains("uddg=")) {
            val encodedPart = href.substringAfter("uddg=").substringBefore("&")
            try {
                val decoded = URLDecoder.decode(encodedPart, "UTF-8")
                if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                    return decoded
                }
            } catch (_: Throwable) {}
        }

        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href
        }

        if (href.startsWith("/")) {
            return "https://duckduckgo.com$href"
        }

        return "https://$href"
    }

    private fun cleanHtml(raw: String): String {
        return try {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(raw).toString()
            }
            decoded.replace(Regex("""\s+"""), " ").trim()
        } catch (_: Throwable) {
            raw.replace(Regex("""<[^>]*>"""), "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }
    }
}
