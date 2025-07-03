package com.example.koztnowplaying

import kotlinx.serialization.Serializable

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

data class SongHistoryItem(val title: String, val artist: String, val time: String?)
