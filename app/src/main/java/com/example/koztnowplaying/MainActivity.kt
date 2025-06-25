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
data class Performance(
    val artist: String,
    val title: String,
    val time: String? = null, // e.g., "2025-06-25T18:28:33+0000"
    val album: String? = null,
    val label: String? = null
)

@Serializable
data class NowPlayingResponse(val performances: List<Performance>)

data class FetchResult(
    val song: String,
    val artist: String,
    val album: String?,
    val label: String?,
    val startTime: String?,
    val logMessage: String
)

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
    var album by remember { mutableStateOf<String?>(null) }
    var label by remember { mutableStateOf<String?>(null) }
    var startTime by remember { mutableStateOf<String?>(null) }
    val logMessages = remember { mutableStateListOf<LogEntry>() }

    LaunchedEffect(Unit) {
        while (true) {
            val result = fetchNowPlaying()
            song = result.song
            artist = result.artist
            album = result.album
            label = result.label
            startTime = result.startTime

            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            logMessages.add(0, LogEntry(timestamp, result.logMessage))
            if (logMessages.size > 100) {
                logMessages.removeLast()
            }

            delay(15000) // Refresh every 15 seconds
        }
    }

    BoxWithConstraints {
        val isWideScreen = maxWidth > 600.dp
        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NowPlayingInfo(modifier = Modifier.weight(1f), song, artist, album, label, startTime)
                LogDisplay(modifier = Modifier.weight(1f), logMessages)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                NowPlayingInfo(modifier = Modifier.weight(1f), song, artist, album, label, startTime)
                LogDisplay(modifier = Modifier.weight(1f), logMessages)
            }
        }
    }
}

@Composable
fun NowPlayingInfo(modifier: Modifier = Modifier, song: String, artist: String, album: String?, label: String?, startTime: String?) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = song, fontSize = 40.sp, textAlign = TextAlign.Center, lineHeight = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = artist, fontSize = 28.sp, textAlign = TextAlign.Center, lineHeight = 36.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (!album.isNullOrBlank()) {
            Text(text = "Album: $album", fontSize = 16.sp, color = Color.LightGray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (!label.isNullOrBlank()) {
            Text(text = "Label: $label", fontSize = 16.sp, color = Color.LightGray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (startTime != null) {
            Text(text = "Started at $startTime", fontSize = 16.sp, color = Color.LightGray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LogDisplay(modifier: Modifier = Modifier, logMessages: List<LogEntry>) {
    val listState = rememberLazyListState()
    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    Column(
        modifier = modifier.fillMaxHeight().background(Color.DarkGray).padding(horizontal = 8.dp, vertical = 4.dp)
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
        
        val formattedTime = nowPlaying.time?.let { formatTimestamp(it) }

        FetchResult(nowPlaying.title, nowPlaying.artist, nowPlaying.album, nowPlaying.label, formattedTime, "Success: Parsed data from primary source.")
    } catch (e: Exception) {
        val errorLog = "Primary source failed: ${e.message}. Using fallback."
        try {
            val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
            val songTitle = doc.selectFirst(".song-title")?.text() ?: ""
            val artistName = doc.selectFirst(".artist-name")?.text() ?: ""
            if (songTitle.isBlank() || artistName.isBlank()) {
                throw IllegalStateException("Fallback source returned empty data.")
            }
            FetchResult(songTitle, artistName, null, null, null, errorLog)
        } catch (e2: Exception) {
            FetchResult("Now Playing", "(Data currently unavailable)", null, null, null, "Fatal: Both sources failed. Retrying...")
        }
    }
}

fun formatTimestamp(isoTimestamp: String): String? {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        val date = inputFormat.parse(isoTimestamp)
        date?.let { outputFormat.format(it) }
    } catch (e: Exception) {
        null // Return null if parsing fails
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
