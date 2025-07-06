# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application that displays "Now Playing" information from KOZT radio station. The app fetches real-time data about currently playing songs and displays them with album artwork, artist information, and playback details.

## Build Commands

### Building the App
```bash
./gradlew :app:assembleRelease    # Build release APK
./gradlew :app:assembleDebug      # Build debug APK
```

### Running Tests
```bash
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest    # Run instrumentation tests
```

### Other Useful Commands
```bash
./gradlew clean                   # Clean build artifacts
./gradlew lint                    # Run Android lint checks
./gradlew :app:installDebug       # Install debug APK to connected device
```

## Architecture

### Core Components
- **MainActivity.kt**: Main UI entry point using Jetpack Compose
- **NowPlayingViewModel.kt**: ViewModel managing app state and data fetching logic
- **Network.kt**: Handles API calls to fetch now playing data from two sources
- **Data.kt**: Contains data classes for API responses and internal data structures

### Data Flow
1. **Primary Source**: Fetches JSON from `prt.amperwave.net` API containing detailed track info
2. **Fallback Source**: Scrapes HTML from `kozt.com/now-playing/` if primary fails
3. **Auto-refresh**: Data updates every 15 seconds when app is in foreground
4. **Background handling**: Stops fetching when app goes to background

### Key Features
- **Adaptive UI**: Different layouts for wide screens vs mobile
- **Dynamic backgrounds**: Album art colors generate gradient backgrounds using Palette API
- **Screen management**: Keep screen on toggle functionality
- **Logging**: Toggle-able debug logs for troubleshooting data fetching

## Project Structure

```
app/src/main/java/com/example/koztnowplaying/
├── MainActivity.kt          # Main UI and Compose components
├── NowPlayingViewModel.kt   # State management and business logic
├── Network.kt              # API calls and data fetching
└── Data.kt                 # Data classes and models
```

## Dependencies

### Key Libraries
- **Jetpack Compose**: Modern UI toolkit
- **Kotlinx Serialization**: JSON parsing for API responses
- **JSoup**: HTML parsing for fallback data source
- **Coil**: Image loading for album artwork
- **Palette**: Color extraction from album art
- **Coroutines**: Asynchronous programming

## Build Configuration

- **Target SDK**: 34
- **Min SDK**: 26
- **Kotlin Version**: 1.9.20
- **Compose BOM**: 2023.08.00
- **Build Tools**: 8.2.0

## CI/CD

The project uses GitHub Actions for continuous integration:
- Builds release APK on push to main branch
- Handles keystore signing using GitHub secrets
- Uploads signed APK as build artifact
- Verifies APK signature integrity

## Development Notes

### Environment Variables for Signing
When building locally for release, set these environment variables:
- `KEYSTORE_FILE`: Path to keystore file
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password

### Testing Data Sources
The app has built-in fallback mechanisms. To test:
1. Primary API: Monitor logs for "Success: Parsed data from primary source"
2. Fallback: Look for "Primary source failed" in logs
3. Both failed: Shows "Data currently unavailable" message