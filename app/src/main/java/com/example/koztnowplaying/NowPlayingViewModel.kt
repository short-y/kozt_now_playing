package com.example.koztnowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.koztnowplaying.Constants.FETCH_INTERVAL_MS
import kotlinx.coroutines.Job
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

    private val _resetBackground = MutableStateFlow(false)
    val resetBackground = _resetBackground.asStateFlow()

    private val _songHistory = MutableStateFlow<List<SongHistoryItem>>(emptyList())
    val songHistory = _songHistory.asStateFlow()

    private var fetchJob: Job? = null
    private val lastUpdatedFormat = SimpleDateFormat("h:mm:ss a", Locale.US)
    private val logTimestampFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun startFetching(shouldFetch: Boolean) {
        if (shouldFetch && fetchJob?.isActive != true) {
            fetchJob = viewModelScope.launch {
                while (true) {
                    val result = fetchNowPlaying()
                    _lastUpdated.value = lastUpdatedFormat.format(Date())

                    if (result.song != _song.value || result.artist != _artist.value) {
                        _song.value = result.song
                        _artist.value = result.artist
                        _album.value = result.album
                        _label.value = result.label
                        _startTime.value = result.startTime
                        _imageUris.value = result.imageUris
                        if (result.imageUris.large == null) {
                            _resetBackground.value = true
                        }
                    }

                    val logTimestamp = logTimestampFormat.format(Date())
                    val newLogs = _logMessages.value.toMutableList()
                    newLogs.add(0, LogEntry(logTimestamp, result.logMessage))
                    if (newLogs.size > 100) newLogs.removeLast()
                    _logMessages.value = newLogs

                    delay(FETCH_INTERVAL_MS)
                }
            }
        } else if (!shouldFetch) {
            fetchJob?.cancel()
            fetchJob = null
        }
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _songHistory.value = fetchNowPlayingHistory()
        }
    }

    fun clearHistory() {
        _songHistory.value = emptyList()
    }

    fun backgroundResetHandled() {
        _resetBackground.value = false
    }

    fun toggleLogs() {
        _showLogs.value = !_showLogs.value
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        _keepScreenOn.value = keepOn
    }
}
