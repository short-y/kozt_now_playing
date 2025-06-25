package com.example.koztnowplaying

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// Data classes to match the actual JSON structure
@Serializable
data class Performance(val artist: String, val title: String)

@Serializable
data class NowPlayingResponse(val performances: List<Performance>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KOZTNowPlayingTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NowPlayingScreen()
                }
            }
        }
    }
}

@Composable
fun NowPlayingScreen() {
    var song by remember { mutableStateOf("Loading...") }
    var artist by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val (newSong, newArtist, newError) = fetchNowPlaying()
            song = newSong
            artist = newArtist
            errorMessage = newError
            delay(10000) // Refresh every 10 seconds
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = song,
            fontSize = 40.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = artist,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        errorMessage?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = Color.Yellow,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchNowPlaying(): Triple<String, String, String?> = withContext(Dispatchers.IO) {
    // 1. Try the primary JSON source first
    try {
        val jsonString = URL("https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json").readText()
        val response = json.decodeFromString<NowPlayingResponse>(jsonString)
        val nowPlaying = response.performances.firstOrNull()
            ?: throw IllegalStateException("Primary source returned no performances.")
        
        if (nowPlaying.title.isBlank()) {
            throw IllegalStateException("Primary source returned empty title.")
        }
        return@withContext Triple(nowPlaying.title, nowPlaying.artist, null) // Success
    } catch (e: Exception) {
        // 2. If primary fails, prepare an error message and try the fallback
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val errorMsg = "Primary source failed at $timestamp. Using fallback."
        try {
            val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
            val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
            val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
            if (songTitle.isBlank() || artistName.isBlank()) {
                throw IllegalStateException("Fallback source returned empty data.")
            }
            Triple(songTitle, artistName, errorMsg)
        } catch (e2: Exception) {
            // 3. If both fail, return a hardcoded error
            Triple("Now Playing", "(Data currently unavailable)", "Both sources failed to provide data.")
        }
    }
}

@Composable
fun KOZTNowPlayingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color.Black,
            onBackground = Color.White
        ),
        content = content
    )
}
