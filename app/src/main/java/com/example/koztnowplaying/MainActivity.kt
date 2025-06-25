package com.example.koztnowplaying

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

// --- Data Classes for State and JSON Parsing ---
@Serializable
data class Performance(val artist: String, val title: String)

@Serializable
data class NowPlayingResponse(val performances: List<Performance>)

data class FetchResult(val song: String, val artist: String, val logMessage: String)

data class LogEntry(val timestamp: String, val message: String)

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
    val logMessages = remember { mutableStateListOf<LogEntry>() }

    LaunchedEffect(Unit) {
        while (true) {
            val result = fetchNowPlaying()
            song = result.song
            artist = result.artist

            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            logMessages.add(0, LogEntry(timestamp, result.logMessage))
            if (logMessages.size > 100) {
                logMessages.removeLast()
            }

            delay(15000) // Refresh every 15 seconds
        }
    }

    // Adaptive layout that changes based on screen width
    BoxWithConstraints {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            // For wide screens, use a Row
            Row(modifier = Modifier.fillMaxSize()) {
                NowPlayingInfo(modifier = Modifier.weight(1f), song = song, artist = artist)
                LogDisplay(modifier = Modifier.weight(1f), logMessages = logMessages)
            }
        } else {
            // For narrow screens, use a Column
            Column(modifier = Modifier.fillMaxSize()) {
                NowPlayingInfo(modifier = Modifier.weight(1f), song = song, artist = artist)
                LogDisplay(modifier = Modifier.weight(1f), logMessages = logMessages)
            }
        }
    }
}

@Composable
fun NowPlayingInfo(modifier: Modifier = Modifier, song: String, artist: String) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = song, fontSize = 40.sp, textAlign = TextAlign.Center, lineHeight = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = artist, fontSize = 28.sp, textAlign = TextAlign.Center, lineHeight = 36.sp)
    }
}

@Composable
fun LogDisplay(modifier: Modifier = Modifier, logMessages: List<LogEntry>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.DarkGray)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("Logs", fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn(state = listState) {
            items(logMessages) { entry ->
                Text(
                    text = "${entry.timestamp}: ${entry.message}",
                    color = if (entry.message.contains("Success")) Color.Green else if (entry.message.contains("failed")) Color.Yellow else Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchNowPlaying(): FetchResult = withContext(Dispatchers.IO) {
    try {
        val jsonString = URL("https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json").readText()
        val response = json.decodeFromString<NowPlayingResponse>(jsonString)
        val nowPlaying = response.performances.firstOrNull()
            ?: throw IllegalStateException("Primary source returned no performances.")

        if (nowPlaying.title.isBlank()) {
            throw IllegalStateException("Primary source returned empty title (commercial break?).")
        }
        FetchResult(nowPlaying.title, nowPlaying.artist, "Success: Parsed data from primary source.")
    } catch (e: Exception) {
        val errorLog = "Primary source failed: ${e.message}. Using fallback."
        try {
            val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
            val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
            val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
            if (songTitle.isBlank() || artistName.isBlank()) {
                throw IllegalStateException("Fallback source returned empty data.")
            }
            FetchResult(songTitle, artistName, errorLog)
        } catch (e2: Exception) {
            FetchResult("Now Playing", "(Data currently unavailable)", "Fatal: Both sources failed. Retrying...")
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