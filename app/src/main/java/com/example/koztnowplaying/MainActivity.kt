package com.example.koztnowplaying

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import java.text.SimpleDateFormat
import java.util.*

// Data class to match the JSON structure from the primary source
@Serializable
data class NowPlayingInfo(val artist: String, val title: String)

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
        modifier = Modifier.fillMaxSize(),
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

// A lenient Json parser for robustness
private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchNowPlaying(): Triple<String, String, String?> = withContext(Dispatchers.IO) {
    // 1. Try the primary JSON source first
    try {
        val jsonString = Jsoup.connect("https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json").ignoreContentType(true).execute().body()
        val nowPlaying = json.decodeFromString<NowPlayingInfo>(jsonString)
        return@withContext Triple(nowPlaying.title, nowPlaying.artist, null) // Success, no error message
    } catch (e: Exception) {
        // 2. If primary fails, prepare an error message and try the fallback
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val errorMsg = "Primary source failed at $timestamp. Using fallback."
        try {
            val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
            val songTitle = doc.selectFirst(".song-title")?.text() ?: "Unknown Song"
            val artistName = doc.selectFirst(".artist-name")?.text() ?: "Unknown Artist"
            Triple(songTitle, artistName, errorMsg)
        } catch (e2: Exception) {
            // 3. If both fail, return a hardcoded error
            Triple("Error", "Could not fetch data from any source.", null)
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
