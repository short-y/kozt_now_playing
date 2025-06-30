package com.example.koztnowplaying

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {
    private val viewModel: NowPlayingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KOZTNowPlayingTheme {
                NowPlayingScreen(viewModel)
            }
        }
    }
}

@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel) {
    val song by viewModel.song.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val album by viewModel.album.collectAsState()
    val label by viewModel.label.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val imageUris by viewModel.imageUris.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val logMessages by viewModel.logMessages.collectAsState()
    val showLogs by viewModel.showLogs.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val resetBackground by viewModel.resetBackground.collectAsState()

    var gradientColors by remember { mutableStateOf<List<Color>>(listOf(Color(0xFF0d47a1), Color.Black)) }
    var textColor by remember { mutableStateOf(Color.White) }

    KeepScreenOn(keepScreenOn)

    LaunchedEffect(Unit) {
        viewModel.startFetching()
    }

    if (resetBackground) {
        gradientColors = listOf(Color(0xFF0d47a1), Color.Black)
        textColor = Color.White
        viewModel.backgroundResetHandled()
    }

    val onImageLoaded: (Drawable) -> Unit = { drawable ->
        Palette.from(drawable.toBitmap()).generate { palette ->
            val dominantSwatch = palette?.dominantSwatch
            if (dominantSwatch != null) {
                val dominantColor = Color(dominantSwatch.rgb)
                val contrastingColor = palette.darkMutedSwatch?.rgb?.let { Color(it) } ?: Color.Black
                gradientColors = listOf(dominantColor, contrastingColor)
                textColor = Color(dominantSwatch.bodyTextColor)
            } else {
                gradientColors = listOf(Color(0xFF0d47a1), Color.Black)
                textColor = Color.White
            }
        }
    }

    AnimatedGradientBackground(gradientColors) {
        val context = LocalContext.current
        BoxWithConstraints {
            val isWideScreen = maxWidth > 600.dp

            if (isWideScreen) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    NowPlayingInfo(
                        modifier = Modifier.weight(1f),
                        song = song, artist = artist, album = album, label = label, startTime = startTime,
                        imageUris = imageUris, lastUpdated = lastUpdated, keepScreenOn = keepScreenOn,
                        textColor = textColor,
                        onKeepScreenOnChanged = { viewModel.setKeepScreenOn(it) },
                        onToggleLogs = { viewModel.toggleLogs() },
                        onHistoryClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/user/a7inchsleeve/library"))
                            context.startActivity(intent)
                        },
                        onImageLoaded = onImageLoaded
                    )
                    if (showLogs) {
                        LogDisplay(Modifier.weight(1f), logMessages)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NowPlayingInfo(
                        modifier = Modifier.weight(1f),
                        song = song, artist = artist, album = album, label = label, startTime = startTime,
                        imageUris = imageUris, lastUpdated = lastUpdated, keepScreenOn = keepScreenOn,
                        textColor = textColor,
                        onKeepScreenOnChanged = { viewModel.setKeepScreenOn(it) },
                        onToggleLogs = { viewModel.toggleLogs() },
                        onHistoryClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/user/a7inchsleeve/library"))
                            context.startActivity(intent)
                        },
                        onImageLoaded = onImageLoaded
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
    song: String, artist: String, album: String?, label: String?, startTime: String?,
    imageUris: ImageUris, lastUpdated: String?, keepScreenOn: Boolean, textColor: Color,
    onKeepScreenOnChanged: (Boolean) -> Unit, onToggleLogs: () -> Unit, onHistoryClick: () -> Unit,
    onImageLoaded: (Drawable) -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AlbumArt(imageUris = imageUris, onImageLoaded = onImageLoaded)
        }
        Spacer(Modifier.height(16.dp))
        InfoColumn(
            song, artist, album, label, startTime, lastUpdated,
            keepScreenOn, onKeepScreenOnChanged, onToggleLogs, onHistoryClick, textColor
        )
    }
}

@Composable
private fun InfoColumn(
    song: String, artist: String, album: String?, label: String?, startTime: String?, lastUpdated: String?,
    keepScreenOn: Boolean, onKeepScreenOnChanged: (Boolean) -> Unit, onToggleLogs: () -> Unit, onHistoryClick: () -> Unit,
    textColor: Color
) {
    val textShadow = androidx.compose.ui.graphics.Shadow(
        color = Color.Black.copy(alpha = 0.7f),
        offset = androidx.compose.ui.geometry.Offset(4f, 4f),
        blurRadius = 8f
    )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(text = song, fontSize = 40.sp, textAlign = TextAlign.Center, lineHeight = 48.sp, color = textColor, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
        Spacer(Modifier.height(8.dp))
        Text(text = "Artist: $artist", fontSize = 28.sp, textAlign = TextAlign.Center, lineHeight = 36.sp, color = textColor, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
        Spacer(Modifier.height(24.dp))

        if (!album.isNullOrBlank()) {
            Text(text = "Album: $album", fontSize = 16.sp, color = textColor.copy(alpha = 0.7f), textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
            Spacer(Modifier.height(4.dp))
        }
        if (!label.isNullOrBlank()) {
            Text(text = "Label: $label", fontSize = 16.sp, color = textColor.copy(alpha = 0.7f), textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
            Spacer(Modifier.height(4.dp))
        }
        if (startTime != null) {
            Text(text = "Started at $startTime", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
        }
        if (lastUpdated != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = "(Updated at $lastUpdated)", fontSize = 14.sp, color = textColor.copy(alpha = 0.5f), textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onToggleLogs, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))) { Text("Toggle Logs") }
            Button(onClick = onHistoryClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))) { Text("History") }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChanged)
            Spacer(Modifier.width(8.dp))
            Text("Keep Screen On", color = textColor, style = androidx.compose.ui.text.TextStyle(shadow = textShadow))
        }
        Spacer(Modifier.height(16.dp))
        VersionInfo(textColor)
    }
}

@Composable
private fun AlbumArt(imageUris: ImageUris, onImageLoaded: (Drawable) -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints {
        val imageSize = maxWidth
        val selectedImageUri = when {
            imageSize > 400.dp -> imageUris.large
            imageSize > 200.dp -> imageUris.medium
            else -> imageUris.small
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(selectedImageUri)
                .allowHardware(false) // Required for Palette
                .build(),
            contentDescription = "Album Art",
            onSuccess = { result -> onImageLoaded(result.result.drawable) },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun VersionInfo(textColor: Color) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    Text(
        text = "Version: $versionName",
        fontSize = 12.sp,
        color = textColor,
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
fun AnimatedGradientBackground(colors: List<Color>, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedRadius = infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = colors,
                            radius = animatedRadius.value
                        )
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun KOZTNowPlayingTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}