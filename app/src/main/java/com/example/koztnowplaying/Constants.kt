package com.example.koztnowplaying

object Constants {
    const val NOW_PLAYING_PRIMARY_URL = "https://api-nowplaying.amperwave.net/api/v1/prtplus/nowplaying/2/4756/nowplaying.json"
    const val NOW_PLAYING_BACKUP_URL = "https://prt.amperwave.net/prt/nowplaying/2/2/3438/nowplaying.json"
    const val NOW_PLAYING_FALLBACK_URL = "https://kozt.com/now-playing/"

    const val HISTORY_PRIMARY_URL = "https://api-nowplaying.amperwave.net/api/v1/prtplus/nowplaying/11/4756/nowplaying.json"
    const val HISTORY_BACKUP_URL = "https://prt.amperwave.net/prt/nowplaying/2/11/3438/nowplaying.json"

    const val FETCH_INTERVAL_MS = 15000L
}
