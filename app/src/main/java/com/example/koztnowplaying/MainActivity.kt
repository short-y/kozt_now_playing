package com.example.koztnowplaying

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// --- Data Classes ---
@Serializable
data class Performance(
    val artist: String, val title: String, val time: String? = null, 
    val album: String? = null, val label: String? = null, 
    val smallimage: String? = null, val mediumimage: String? = null, val largeimage: String? = null
)

@Serializable
data class NowPlayingResponse(val performances: List<Performance>)

data class FetchResult(
    val song: String, val artist: String, val album: String?, val label: String?, 
    val startTime: String?, val imageUris: ImageUris, val logMessage: String
)

data class ImageUris(val small: String?, val medium: String?, val large: String?)

data class LogEntry(val timestamp: String, val message: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KOZTNowPlayingTheme {
                NowPlayingScreen()
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
    var imageUris by remember { mutableStateOf(ImageUris(null, null, null)) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }
    val logMessages = remember { mutableStateListOf<LogEntry>() }
    var showLogs by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var isAppInForeground by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAppInForeground = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isAppInForeground = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    KeepScreenOn(keepScreenOn)

    LaunchedEffect(isAppInForeground) {
        while (isAppInForeground) {
            val result = fetchNowPlaying()
            song = result.song
            artist = result.artist
            album = result.album
            label = result.label
            startTime = result.startTime
            imageUris = result.imageUris
            lastUpdated = SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())

            val logTimestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            logMessages.add(0, LogEntry(logTimestamp, result.logMessage))
            if (logMessages.size > 100) logMessages.removeLast()

            delay(15000)
        }
    }

    AnimatedGradientBackground {
        BoxWithConstraints {
            val isWideScreen = maxWidth > 600.dp
            val activity = (LocalContext.current as? ComponentActivity)

            if (isWideScreen) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    NowPlayingInfo(
                        Modifier.weight(1f), song, artist, album, label, startTime, imageUris, lastUpdated, keepScreenOn,
                        { keepScreenOn = it }, { showLogs = !showLogs }, { activity?.finish() }
                    )
                    if (showLogs) {
                        LogDisplay(Modifier.weight(1f), logMessages)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    NowPlayingInfo(
                        Modifier.weight(1f), song, artist, album, label, startTime, imageUris, lastUpdated, keepScreenOn,
                        { keepScreenOn = it }, { showLogs = !showLogs }, { activity?.finish() }
                    )
                    if (showLogs) {
                        LogDisplay(Modifier.weight(0.4f), logMessages)
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingInfo(
    modifier: Modifier = Modifier,
    song: String, artist: String, album: String?, label: String?, startTime: String?, imageUris: ImageUris, lastUpdated: String?,
    keepScreenOn: Boolean, onKeepScreenOnChanged: (Boolean) -> Unit, onToggleLogs: () -> Unit, onExit: () -> Unit
) {
    SubcomposeLayout(modifier = modifier.padding(16.dp)) { constraints ->
        val infoColumnPlaceable = subcompose("info") {
            InfoColumn(song, artist, album, label, startTime, lastUpdated, keepScreenOn, onKeepScreenOnChanged, onToggleLogs, onExit)
        }.first().measure(constraints)

        val remainingHeight = constraints.maxHeight - infoColumnPlaceable.height
        val imageSize = minOf(constraints.maxWidth, remainingHeight)

        val imagePlaceable = subcompose("image") {
            AlbumArt(imageUris = imageUris, imageSize = imageSize.toDp())
        }.first().measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            val imageX = (constraints.maxWidth - imagePlaceable.width) / 2
            val infoX = (constraints.maxWidth - infoColumnPlaceable.width) / 2
            val infoY = constraints.maxHeight - infoColumnPlaceable.height
            
            imagePlaceable.placeRelative(imageX, 0)
            infoColumnPlaceable.placeRelative(infoX, infoY)
        }
    }
}

@Composable
private fun InfoColumn(
    song: String, artist: String, album: String?, label: String?, startTime: String?, lastUpdated: String?,
    keepScreenOn: Boolean, onKeepScreenOnChanged: (Boolean) -> Unit, onToggleLogs: () -> Unit, onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(text = song, fontSize = 40.sp, textAlign = TextAlign.Center, lineHeight = 48.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(text = "Artist: $artist", fontSize = 28.sp, textAlign = TextAlign.Center, lineHeight = 36.sp, color = Color.White)
        Spacer(Modifier.height(24.dp))

        if (!album.isNullOrBlank()) {
            Text(text = "Album: $album", fontSize = 16.sp, color = Color.LightGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
        if (!label.isNullOrBlank()) {
            Text(text = "Label: $label", fontSize = 16.sp, color = Color.LightGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
        if (startTime != null) {
            Text(text = "Started at $startTime", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF87CEEB), textAlign = TextAlign.Center)
        }
        if (lastUpdated != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = "(Updated at $lastUpdated)", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onToggleLogs, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))) { Text("Toggle Logs") }
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))) { Text("Exit") }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChanged)
            Spacer(Modifier.width(8.dp))
            Text("Keep Screen On", color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
        VersionInfo()
    }
}

@Composable
private fun AlbumArt(imageUris: ImageUris, imageSize: Dp) {
    val selectedImageUri = when {
        imageSize > 400.dp -> imageUris.large
        imageSize > 200.dp -> imageUris.medium
        else -> imageUris.small
    }
    AsyncImage(
        model = selectedImageUri,
        contentDescription = "Album Art",
        modifier = Modifier
            .size(imageSize)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun VersionInfo() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    Text(
        text = "Version: $versionName",
        fontSize = 12.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun LogDisplay(modifier: Modifier = Modifier, logMessages: List<LogEntry>) {
    val listState = rememberLazyListState()
    LaunchedEffect(logMessages.size) {
        if (logMessages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    Column(
        modifier = modifier.fillMaxHeight().background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
fun KeepScreenOn(keepOn: Boolean) {
    val context = LocalContext.current
    DisposableEffect(keepOn) {
        val window = context.findActivity()?.window
        if (keepOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AnimatedGradientBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedRadius = infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val colors = listOf(Color(0xFF0d47a1), Color(0xFF000000))

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().drawBehind { 
            drawRect(Brush.radialGradient(
                colors = colors,
                radius = animatedRadius.value
            ))
        }) {
            content()
        }
    }
}

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

@Composable
fun KOZTNowPlayingTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}