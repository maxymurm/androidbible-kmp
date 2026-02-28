# Android Bible - KMP

Compose Multiplatform Bible app for Android and iOS.

## Architecture

- **Compose Multiplatform** for shared UI
- **SQLDelight** for local database (offline-first)
- **Ktor** for HTTP/WebSocket networking
- **Koin** for dependency injection
- **Voyager** for navigation
- **kotlinx.serialization** for JSON
- **kotlinx.datetime** for date handling

## Project Structure

```
composeApp/
├── src/
│   ├── commonMain/         # Shared code
│   │   ├── kotlin/org/androidbible/
│   │   │   ├── app/        # App entry point
│   │   │   ├── data/       # Data layer
│   │   │   │   ├── local/  # SQLDelight database
│   │   │   │   ├── remote/ # Ktor API client
│   │   │   │   ├── repository/ # Repository implementations
│   │   │   │   └── sync/   # Sync engine
│   │   │   ├── di/         # Koin modules
│   │   │   ├── domain/     # Domain models & interfaces
│   │   │   │   ├── model/  # Data classes
│   │   │   │   └── repository/ # Repository interfaces
│   │   │   ├── ui/         # Compose UI
│   │   │   │   ├── components/ # Reusable components
│   │   │   │   ├── navigation/ # Navigation
│   │   │   │   ├── screens/    # Screen composables
│   │   │   │   └── theme/  # Material3 theme
│   │   │   └── util/       # Utilities (ARI, etc.)
│   │   └── sqldelight/     # SQLDelight schema & queries
│   ├── androidMain/        # Android-specific code
│   └── iosMain/            # iOS-specific code
iosApp/                     # iOS Xcode project
```

## Key Concepts

### ARI (Absolute Reference Integer)
Encodes book, chapter, verse into a single integer:
```
ari = (bookId << 16) | (chapter << 8) | verse
```

### Sync Engine
- Offline-first with local SQLDelight database
- Mutation queue for offline changes
- Push/pull sync with Laravel backend
- WebSocket real-time updates via Laravel Reverb

### Marker System
- Bookmarks (kind=0), Notes (kind=1), Highlights (kind=2)
- Labels for organization
- UUID-based GIDs for sync

## Setup

1. Open in Android Studio / Fleet
2. Sync Gradle project
3. Run on Android or iOS

## Backend

API backend: [androidbible-api](https://github.com/maxymurm/androidbible-api)
