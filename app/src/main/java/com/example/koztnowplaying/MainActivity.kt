package com.example.koztnowplaying

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

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

    LaunchedEffect(Unit) {
        while (true) {
            val nowPlaying = fetchNowPlaying()
            song = nowPlaying.first
            artist = nowPlaying.second
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
    }
}

suspend fun fetchNowPlaying(): Pair<String, String> = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect("https://kozt.com/now-playing/").get()
        val songTitle = doc.selectFirst(".song-title")?.text() ?: "Unknown Song"
        val artistName = doc.selectFirst(".artist-name")?.text() ?: "Unknown Artist"
        Pair(songTitle, artistName)
    } catch (e: Exception) {
        Pair("Error", "Could not fetch data")
    }
}

@Composable
fun KOZTNowPlayingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = androidx.compose.ui.graphics.Color.Black,
            onBackground = androidx.compose.ui.graphics.Color.White
        ),
        content = content
    )
}