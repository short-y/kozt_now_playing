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
    try {
        val jsonString = URL("https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json").readText()
        val response = json.decodeFromString<NowPlayingResponse>(jsonString)
        val nowPlaying = response.performances.firstOrNull() ?: throw IllegalStateException("Primary source returned no performances.")
        if (nowPlaying.title.isBlank()) throw IllegalStateException("Primary source returned empty title (commercial break?).")
        val formattedTime = nowPlaying.time?.let { formatTimestamp(it) }
        val imageUris = ImageUris(nowPlaying.smallimage, nowPlaying.mediumimage, nowPlaying.largeimage)
        FetchResult(nowPlaying.title, nowPlaying.artist, nowPlaying.album, nowPlaying.label, formattedTime, imageUris, "Success: Parsed data from primary source.")
    } catch (e: Exception) {
        val errorLog = "Primary source failed: ${e.message}. Using fallback."
        try {
            val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
            val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
            val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
            if (songTitle.isBlank() || artistName.isBlank()) throw IllegalStateException("Fallback source returned empty data.")
            FetchResult(songTitle, artistName, null, null, null, ImageUris(null, null, null), errorLog)
        } catch (e2: Exception) {
            FetchResult("Now Playing", "(Data currently unavailable)", null, null, null, ImageUris(null, null, null), "Fatal: Both sources failed. Retrying...")
        }
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