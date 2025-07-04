package com.example.koztnowplaying

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
        "https://api-nowplaying.amperwave.net/api/v1/prtplus/nowplaying/2/4756/nowplaying.json",
        "https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json"
    )
    var lastErrorLog = ""

    for (url in urls) {
        try {
            val jsonString = URL(url).readText()
            val response = json.decodeFromString<NowPlayingResponse>(jsonString)
            val nowPlaying = response.performances.firstOrNull() ?: throw IllegalStateException("Source returned no performances.")
            if (nowPlaying.title.isBlank()) throw IllegalStateException("Source returned empty title (commercial break?).")
            val formattedTime = nowPlaying.time?.let { formatTimestamp(it) }
            val imageUris = ImageUris(nowPlaying.smallimage, nowPlaying.mediumimage, nowPlaying.largeimage)
            return@withContext FetchResult(nowPlaying.title, nowPlaying.artist, nowPlaying.album, nowPlaying.label, formattedTime, imageUris, "Success: Parsed data from JSON source.")
        } catch (e: Exception) {
            lastErrorLog += "URL $url failed: ${e.message}. "
        }
    }

    try {
        val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
        val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
        val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
        if (songTitle.isBlank() || artistName.isBlank()) throw IllegalStateException("Fallback source returned empty data.")
        FetchResult(songTitle, artistName, null, null, null, ImageUris(null, null, null), lastErrorLog)
    } catch (e2: Exception) {
        FetchResult("Now Playing", "(Data currently unavailable)", null, null, null, ImageUris(null, null, null), "Fatal: All sources failed. Retrying...")
    }
}

suspend fun fetchNowPlayingHistory(): List<SongHistoryItem> = withContext(Dispatchers.IO) {
    try {
        // Fetch 11 items to get the current song plus the last 10 played.
        val jsonString = URL("https://api-nowplaying.amperwave.net/api/v1/prtplus/nowplaying/2/4756/nowplaying.json").readText()
        val response = json.decodeFromString<NowPlayingResponse>(jsonString)
        // We skip the first item because it's the currently playing song.
        response.performances.drop(1).map {
            SongHistoryItem(
                title = it.title,
                artist = it.artist,
                time = it.time?.let { time -> formatTimestamp(time) }
            )
        }
    } catch (e: Exception) {
        // Return an empty list if the history fetch fails.
        emptyList()
    }
}

fun formatTimestamp(isoTimestamp: String): String? {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        val date = inputFormat.parse(isoTimestamp)
        date?.let { outputFormat.format(it) }
    } catch (e: Exception) { null }
}
