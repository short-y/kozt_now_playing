package com.example.koztnowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NowPlayingViewModel : ViewModel() {
    private val _song = MutableStateFlow("Loading...")
    val song = _song.asStateFlow()

    private val _artist = MutableStateFlow("")
    val artist = _artist.asStateFlow()

    private val _album = MutableStateFlow<String?>(null)
    val album = _album.asStateFlow()

    private val _label = MutableStateFlow<String?>(null)
    val label = _label.asStateFlow()

    private val _startTime = MutableStateFlow<String?>(null)
    val startTime = _startTime.asStateFlow()

    private val _imageUris = MutableStateFlow(ImageUris(null, null, null))
    val imageUris = _imageUris.asStateFlow()

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated = _lastUpdated.asStateFlow()

    private val _logMessages = MutableStateFlow<List<LogEntry>>(emptyList())
    val logMessages = _logMessages.asStateFlow()

    private val _showLogs = MutableStateFlow(false)
    val showLogs = _showLogs.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(false)
    val keepScreenOn = _keepScreenOn.asStateFlow()

    fun startFetching() {
        viewModelScope.launch {
            while (true) {
                val result = fetchNowPlaying()
                if (result.song != _song.value || result.artist != _artist.value) {
                    _song.value = result.song
                    _artist.value = result.artist
                    _album.value = result.album
                    _label.value = result.label
                    _startTime.value = result.startTime
                    _imageUris.value = result.imageUris
                    _lastUpdated.value = SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())
                }

                val logTimestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                val newLogs = _logMessages.value.toMutableList()
                newLogs.add(0, LogEntry(logTimestamp, result.logMessage))
                if (newLogs.size > 100) newLogs.removeLast()
                _logMessages.value = newLogs

                delay(15000)
            }
        }
    }

    fun toggleLogs() {
        _showLogs.value = !_showLogs.value
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        _keepScreenOn.value = keepOn
    }
}