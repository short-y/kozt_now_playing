package com.example.koztnowplaying

import com.example.koztnowplaying.Constants.HISTORY_BACKUP_URL
import com.example.koztnowplaying.Constants.HISTORY_PRIMARY_URL
import com.example.koztnowplaying.Constants.NOW_PLAYING_BACKUP_URL
import com.example.koztnowplaying.Constants.NOW_PLAYING_FALLBACK_URL
import com.example.koztnowplaying.Constants.NOW_PLAYING_PRIMARY_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchNowPlaying(): FetchResult = withContext(Dispatchers.IO) {
    val urls = listOf(
        NOW_PLAYING_PRIMARY_URL to "primary",
        NOW_PLAYING_BACKUP_URL to "backup"
    )
    var lastErrorLog = ""

    for ((url, sourceType) in urls) {
        try {
            val jsonString = URL(url).readText()
            val response = json.decodeFromString<NowPlayingResponse>(jsonString)
            val nowPlaying = response.performances.firstOrNull() ?: throw IllegalStateException("Source returned no performances.")
            if (nowPlaying.title.isBlank()) throw IllegalStateException("Source returned empty title (commercial break?).")
            val formattedTime = nowPlaying.time?.let { formatTimestamp(it) }
            val imageUris = ImageUris(nowPlaying.smallimage, nowPlaying.mediumimage, nowPlaying.largeimage)
            return@withContext FetchResult(nowPlaying.title, nowPlaying.artist, nowPlaying.album, nowPlaying.label, formattedTime, imageUris, "Success: Parsed data from $sourceType JSON source.")
        } catch (e: Exception) {
            lastErrorLog += "URL $url ($sourceType) failed: ${e.message}. "
        }
    }

    try {
        val doc = Jsoup.connect(NOW_PLAYING_FALLBACK_URL).get()
        val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
        val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
        if (songTitle.isBlank() || artistName.isBlank()) throw IllegalStateException("Fallback source returned empty data.")
        FetchResult(songTitle, artistName, null, null, null, ImageUris(null, null, null), lastErrorLog)
    } catch (e2: Exception) {
        FetchResult("Now Playing", "(Data currently unavailable)", null, null, null, ImageUris(null, null, null), "Fatal: All sources failed. Retrying...")
    }
}

suspend fun fetchNowPlayingHistory(): List<SongHistoryItem> = withContext(Dispatchers.IO) {
    val urls = listOf(
        HISTORY_PRIMARY_URL to "primary",
        HISTORY_BACKUP_URL to "backup"
    )
    var lastError: Exception? = null

    for ((url, _) in urls) {
        try {
            val jsonString = URL(url).readText()
            val response = json.decodeFromString<NowPlayingResponse>(jsonString)
            // We skip the first item because it's the currently playing song.
            return@withContext response.performances.drop(1).map {
                SongHistoryItem(
                    title = it.title,
                    artist = it.artist,
                    time = it.time?.let { time -> formatTimestamp(time) }
                )
            }
        } catch (e: Exception) {
            lastError = e
            // Try the next URL
        }
    }
    // If we're here, all history fetches failed. Log the last error.
    lastError?.let {
        // A more robust logging mechanism would be used in a real app
        println("Failed to fetch history from all sources: ${it.message}")
    }
    emptyList()
}

fun formatTimestamp(isoTimestamp: String): String? {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        val date = inputFormat.parse(isoTimestamp)
        date?.let { outputFormat.format(it) }
    } catch (e: Exception) { null }
}
