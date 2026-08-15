package com.naicson.alainz_mp3player.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * There's no free, ToS-compliant way to search Google Images from an app (no public API, and
 * scraping the results page would violate Google's terms and break without notice). The iTunes
 * Search API is a legitimate stand-in for the "search cover art by artist/track" use case: no
 * API key, no quota to manage, and it returns real official artwork instead of arbitrary web
 * images.
 */
@Singleton
class AlbumArtSearchService @Inject constructor() {
    suspend fun search(query: String): List<String> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=25")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseArtworkUrls(body)
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseArtworkUrls(json: String): List<String> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        val urls = LinkedHashSet<String>()
        for (i in 0 until results.length()) {
            val artwork = results.getJSONObject(i).optString("artworkUrl100")
            if (artwork.isBlank()) continue
            // iTunes artwork URLs encode the requested size in the path (e.g. "100x100bb.jpg");
            // swapping it is the standard trick to get a larger image than the thumbnail default.
            urls += artwork.replace("100x100bb", "600x600bb")
        }
        return urls.toList()
    }
}
