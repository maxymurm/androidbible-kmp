# Android Bible KMP — Project Documentation

**Repository:** https://github.com/maxymurm/androidbible-kmp  
**Type:** Kotlin Multiplatform (KMP) + Compose Multiplatform mobile app  
**Platforms:** Android (API 21+), iOS 14+  
**Status:** Active development — Phases 1-12 complete, Phase 13+ in progress  
**Last Updated:** 2026-03-12  
**Project Board:** https://github.com/users/maxymurm/projects/6  

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Database Schema (SQLDelight)](#database-schema)
6. [Screen Map](#screen-map)
7. [Completed Phases (1–12)](#completed-phases)
8. [Roadmap (Phase 13+)](#roadmap)
9. [Development Setup](#development-setup)
10. [Testing](#testing)

---

## Overview

Android Bible KMP is the Compose Multiplatform client for the Android Bible app. It runs natively on Android and iOS from a shared Kotlin codebase, with platform-specific implementations for audio, file I/O, and notifications via `expect`/`actual` declarations.

**Core values:**
- Offline-first (full local database, sync when connected)
- SWORD module support (read standard Bible study software modules)
- Shared Kotlin logic for both platforms
- Material 3 design on Android, adapted for iOS

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│             Compose Multiplatform UI              │
│  (composeApp/src/commonMain/ui/screens/)          │
└──────────────────────┬───────────────────────────┘
                       │ StateFlow
┌──────────────────────▼───────────────────────────┐
│              Shared ViewModel Layer               │
│  (shared/src/commonMain/presentation/)            │
└──────────────────────┬───────────────────────────┘
                       │ suspend / Flow
┌──────────────────────▼───────────────────────────┐
│               Domain / Use Cases                  │
│  (shared/src/commonMain/domain/)                  │
└──────────────────────┬───────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
┌───────▼────────┐           ┌────────▼────────┐
│   Local Data   │           │   Remote Data   │
│  (SQLDelight)  │           │   (Ktor HTTP)   │
│  offline-first │           │   androidbible  │
│                │           │      -api       │
└────────────────┘           └─────────────────┘
```

### Sync Strategy
1. All mutations → local SQLDelight first (immediate)
2. Enqueue mutation in `sync_queue` table
3. On network: POST `/api/v1/sync/push` (batch)
4. On reconnect / WebSocket event: GET `/api/v1/sync/pull`
5. Conflict resolution: last-writer-wins (by `updated_at`)

---

## Technology Stack

| Component                | Technology                     | Version    |
|--------------------------|-------------------------------|------------|
| Language                 | Kotlin                        | 2.x        |
| Multiplatform            | Kotlin Multiplatform (KMP)    | 2.x        |
| UI                       | Compose Multiplatform         | 1.7+       |
| Design System            | Material 3                    | latest     |
| Local Database           | SQLDelight                    | 2.x        |
| HTTP Client              | Ktor                          | 3.x        |
| Serialization            | kotlinx.serialization         | 1.7+       |
| Dependency Injection     | Koin                          | 3.x        |
| Image Loading            | Coil 3                        | 3.x        |
| Navigation               | Compose Navigation            | 2.7+       |
| Concurrency              | Kotlin Coroutines             | 1.9+       |
| Build                    | Gradle (Kotlin DSL)           | 8.x        |
| iOS interop              | Swift (minimal shell)         | 5.9+       |

---

## Project Structure

```
androidbible-kmp/
├── .github/
│   ├── instructions/memory.instruction.md
│   ├── ISSUE_TEMPLATE/
│   ├── pull_request_template.md
│   └── workflows/ci.yml
├── agents/
├── docs/
│   └── PROJECT_DOCUMENTATION.md
├── gradle/
│   └── libs.versions.toml              ← Version catalog
├── shared/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/
│       │   │   ├── data/
│       │   │   │   ├── local/          ← SQLDelight DAOs
│       │   │   │   ├── remote/         ← Ktor API client
│       │   │   │   └── repository/     ← Repository impls
│       │   │   ├── domain/
│       │   │   │   ├── model/          ← Domain models
│       │   │   │   └── usecase/        ← Use cases
│       │   │   ├── presentation/
│       │   │   │   └── viewmodel/      ← ViewModels + UiState
│       │   │   └── di/
│       │   │       └── AppModule.kt    ← Koin modules
│       │   └── sqldelight/
│       │       └── *.sq                ← SQLDelight schemas
│       ├── androidMain/                ← Android expect/actual
│       └── iosMain/                    ← iOS expect/actual
├── composeApp/
│   ├── build.gradle.kts
│   └── src/commonMain/
│       └── kotlin/
│           └── ui/
│               ├── screens/            ← Compose screens
│               ├── components/         ← Reusable composables
│               └── theme/
│                   ├── Theme.kt
│                   ├── Color.kt
│                   └── Typography.kt
├── androidApp/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/
│           └── MainActivity.kt
└── iosApp/
    └── iosApp/
        ├── iOSApp.swift
        └── ContentView.swift
```

---

## Database Schema

### SQLDelight Tables

```sql
-- Bible versions
CREATE TABLE BibleVersion (
    id INTEGER PRIMARY KEY,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    language TEXT NOT NULL,
    is_installed INTEGER NOT NULL DEFAULT 0
);

-- Bookmarks / Notes / Highlights (unified Marker)
CREATE TABLE Marker (
    id INTEGER PRIMARY KEY,
    gid TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL,
    book_num INTEGER NOT NULL,
    chapter_num INTEGER NOT NULL,
    verse_num INTEGER NOT NULL,
    ari INTEGER NOT NULL,
    kind INTEGER NOT NULL,   -- 0=bookmark, 1=note, 2=highlight
    color TEXT,
    note TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0
);

-- Labels / Collections
CREATE TABLE MarkerLabel (
    id INTEGER PRIMARY KEY,
    gid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    color TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- Reading Plans
CREATE TABLE ReadingPlan (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    days_total INTEGER NOT NULL,
    current_day INTEGER NOT NULL DEFAULT 0,
    started_at TEXT
);

-- Sync queue
CREATE TABLE SyncQueueItem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_kind TEXT NOT NULL,
    payload TEXT NOT NULL,   -- JSON
    created_at TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- Reading history
CREATE TABLE ReadingHistoryItem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ari INTEGER NOT NULL,
    opened_at TEXT NOT NULL,
    duration_ms INTEGER
);
```

### Planned Tables (Phase 13+)

```sql
-- Pins
CREATE TABLE Pin (
    id INTEGER PRIMARY KEY,
    gid TEXT NOT NULL UNIQUE,
    ari INTEGER NOT NULL,
    note TEXT,
    created_at TEXT NOT NULL
);

-- Bookmark folders
CREATE TABLE BookmarkFolder (
    id INTEGER PRIMARY KEY,
    gid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    parent_id INTEGER,
    color TEXT,
    FOREIGN KEY (parent_id) REFERENCES BookmarkFolder(id)
);

-- SWORD modules
CREATE TABLE SwordModule (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,   -- 'Bible', 'Commentary', 'Dictionary', 'GenBook'
    language TEXT NOT NULL,
    is_installed INTEGER NOT NULL DEFAULT 0,
    install_path TEXT
);
```

---

## Screen Map

### Implemented Screens (Phases 1–12)

| Screen                  | File                          | Status      |
|-------------------------|-------------------------------|-------------|
| Login                   | LoginScreen.kt                | ✅ Complete |
| Register                | RegisterScreen.kt             | ✅ Complete |
| Home / Dashboard        | HomeScreen.kt                 | ✅ Complete |
| Bible Reader            | BibleReaderScreen.kt          | ✅ Complete |
| Book Selector           | BookSelectorScreen.kt         | ✅ Complete |
| Chapter Selector        | ChapterSelectorScreen.kt      | ✅ Complete |
| Version Selector        | VersionSelectorScreen.kt      | ✅ Complete |
| Bookmarks               | BookmarksScreen.kt            | ✅ Complete |
| Notes                   | NotesScreen.kt                | ✅ Complete |
| Labels                  | LabelsScreen.kt               | ✅ Complete |
| Search                  | SearchScreen.kt               | ✅ Complete |
| Reading Plans           | ReadingPlansScreen.kt         | ✅ Complete |
| Devotionals             | DevotionalsScreen.kt          | ✅ Complete |
| Songs                   | SongsScreen.kt                | ✅ Complete |
| Settings                | SettingsScreen.kt             | ✅ Complete |

### Planned Screens (Phase 13+)

| Screen                  | File                          | Phase      |
|-------------------------|-------------------------------|------------|
| Onboarding (3-step)     | OnboardingScreen.kt           | Phase 13   |
| Module Browser          | ModuleBrowserScreen.kt        | Phase 13   |
| Module Downloader       | ModuleDownloadScreen.kt       | Phase 13   |
| Pins                    | PinsScreen.kt                 | Phase 13   |
| Bookmark Folders        | BookmarkFoldersScreen.kt      | Phase 14   |
| Commentary Panel        | CommentaryScreen.kt           | Phase 14   |
| Dictionary              | DictionaryScreen.kt           | Phase 14   |
| Strong's Word Study     | WordStudyScreen.kt            | Phase 14   |
| History                 | HistoryScreen.kt              | Phase 15   |
| Profile                 | ProfileScreen.kt              | Phase 15   |
| Reading Statistics      | StatisticsScreen.kt           | Phase 15   |
| Audio Player            | AudioPlayerScreen.kt          | Phase 15   |
| Verse Image Generator   | VerseImageScreen.kt           | Phase 16   |
| Data Export             | ExportScreen.kt               | Phase 16   |
| Advanced Search         | AdvancedSearchScreen.kt       | Phase 16   |
| Parallel Translation    | ParallelTranslationScreen.kt  | Phase 16   |

---

## Completed Phases

### Phase 1: KMP Project Setup
- Gradle multiplatform configuration
- Module structure: shared, composeApp, androidApp, iosApp
- Version catalog (`libs.versions.toml`)
- Base Material 3 theme

### Phase 2: Core Data Layer
- SQLDelight schema (all core tables)
- Repository interfaces (BibleRepository, MarkerRepository)
- Domain models (Verse, Marker, ReadingPlan)

### Phase 3: Network & DI
- Ktor HTTP client (shared)
- kotlinx.serialization DTOs
- Koin module setup
- API client (`ApiClient.kt`)

### Phase 4: Authentication UI
- LoginScreen + LoginViewModel
- RegisterScreen
- Token storage (DataStore)
- AuthRepository + Koin integration

### Phase 5: Bible Reader Core
- BibleReaderScreen with verse list
- Book / Chapter selectors
- Version switcher
- ARI-based navigation
- Offline verse loading from SQLDelight

### Phase 6: Markers & Annotations
- Bookmark / Note / Highlight CRUD
- Marker bottom sheet (long-press on verse)
- Labels system
- Sync integration (queue items on mutation)

### Phase 7: Reading Plans
- ReadingPlansScreen
- Plan progress tracking
- Daily reading navigation

### Phase 8: Devotionals & Songs
- DevotionalsScreen (date-based)
- SongsScreen (hymnal)

### Phase 9: Modern UI
- Material 3 dark / light theme
- Custom typography (Gentium for verse text)
- Animation and transitions
- Verse sharing composable

### Phase 10: Testing
- 20+ unit tests (`shared:allTests`)
- ViewModel tests with Turbine
- Repository tests with in-memory SQLDelight

### Phase 11: CI/CD
- GitHub Actions workflow (build + test on PRs)
- Fastlane configuration (Android + iOS)
- Signed release builds

### Phase 12: Documentation
- README with screenshots
- KDoc on all public APIs
- Architecture diagram
- Changelog

---

## Roadmap

### Phase 13: SWORD Engine & Modules
- Pure-Kotlin SWORD engine (zText, RawCom, RawLD4, zLD binary readers)
- `SwordVersification.kt` (KJV + 7 additional systems)
- `SwordModuleConfig.kt` (.conf file parser)
- Module browser screen (list installed + available)
- Module download manager (progress tracking)
- Pins feature (quick-access verse pins)
- Onboarding flow (3-step setup wizard)

### Phase 14: Study Tools
- Commentary screen / panel (per verse)
- Dictionary / Lexicon screen
- Strong's word study panel (tap a word → Strong's entry)
- Bookmark folders (hierarchical, with colors)
- Enhanced highlights (6-color palette)
- Rich-text note editor (Markdown)

### Phase 15: User Profile & History
- Profile screen
- Reading history screen (calendar view)
- Reading statistics (streaks, chapters/day)
- Audio player (expect/actual per platform)

### Phase 16: Advanced Features
- Advanced search (phrase, Strong's, morphology filters)
- Parallel translation view (two versions side by side)
- Verse image generation and sharing
- Data export (DOCX/PDF)
- Tags / collection system

---

## Development Setup

### Prerequisites
- Android Studio Hedgehog+ (or IntelliJ IDEA with KMP plugin)
- JDK 17+
- Xcode 15+ (for iOS builds)
- Android SDK API 21+

### Quick Start
```bash
git clone https://github.com/maxymurm/androidbible-kmp.git
cd androidbible-kmp

# Android
./gradlew :androidApp:installDebug

# iOS (macOS only)
./gradlew :iosApp:build

# Shared tests
./gradlew :shared:allTests
```

### Environment
Copy `.env.example` to `.env.local` and set `API_BASE_URL` to your API endpoint.

---

## Testing

```bash
# All shared tests
./gradlew :shared:allTests

# Android-specific tests
./gradlew :androidApp:test

# With HTML report
./gradlew :shared:allTests --tests "*" --info
```

### Test Patterns
- `*RepositoryTest.kt` — Repository tests with in-memory SQLDelight
- `*ViewModelTest.kt` — ViewModel tests with Turbine for Flow
- `*UseCaseTest.kt` — Use case unit tests
