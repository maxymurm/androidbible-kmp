# Project Documentation  androidbible-kmp

**Repository:** https://github.com/maxymurm/androidbible-kmp  
**Type:** Compose Multiplatform Bible App (Android + iOS + Desktop)  
**Reference source:** androidbible native Java/Android app  
**Reference backend:** goldenBowl / androidbible-api  
**Language:** 100% Kotlin  
**Date:** 2026-03-12

---

## Project Overview

androidbible-kmp is **BibleCMP**  a one-to-one port of the native `androidbible` Java/Android app to Kotlin Multiplatform + Compose Multiplatform. It targets Android, iOS, and Desktop from a single shared Kotlin codebase.

The app reads Bible text from **YES2/YES1 binary files** using pure Kotlin ports of `BintexReader`, `SnappyCodec`, and `Yes2Reader` (originally Java in androidbible). All user data (markers, labels, reading progress) is synced to the **androidbible-api** Laravel backend following the **goldenBowl** sync protocol.

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform (Material3) |
| Language | Kotlin Multiplatform 2.x |
| DI | Koin |
| Database (Local) | SQLDelight (multiplatform SQLite) |
| Networking | Ktor Client |
| Serialization | kotlinx.serialization |
| Real-time | Pusher protocol via Ktor WebSocket |
| Auth | Sanctum Bearer tokens, Google/Apple SDK |
| Background sync | WorkManager (Android), BGTasks (iOS) |
| Testing | JUnit5 + kotlin.test |
| CI/CD | GitHub Actions |

---

## Phases Completed (112)

| Phase | Description | Issues |
|-------|-------------|--------|
| 1 | KMP Foundation & Project Structure | 8 |
| 2 | Basic DB schema + Koin setup | 8 |
| 3 | Auth UI (login, register, OAuth) | 8 |
| 4 | Core Bible feature (initial reader) | 9 |
| 5 | Basic markers (bookmark/highlight/note) | 8 |
| 6 | Sync (initial) | 6 |
| 7 | Reading plans (initial) | 5 |
| 8 | Song books (initial) | 4 |
| 9 | UI/UX (initial) | 8 |
| 10 | Testing | 5 |
| 11 | DevOps (CI/CD) | 6 |
| 12 | Documentation | 5 |

---

## Feature Porting Plan (Phases 1320)

### Phase 13: Binary Readers (YES2/Bintex/Snappy)  ~14 issues
**Goal:** Port all binary file format readers to pure Kotlin in commonMain  
**Source:** androidbible Java modules (BintexReader/, AlkitabYes2/, Snappy/)

| Source (Java) | Target (Kotlin) |
|---|---|
| `BintexReader.java` | `data/binary/bintex/BintexReader.kt` |
| `BintexWriter.java` | `data/binary/bintex/BintexWriter.kt` |
| `SnappyImplJava.java` | `data/binary/snappy/SnappyCodec.kt` |
| `SnappyInputStream.java` | `data/binary/snappy/SnappyInputStream.kt` |
| `Yes2Reader.java` | `data/binary/yes2/Yes2Reader.kt` |
| `VersionInfoSection.java` | `data/binary/yes2/section/VersionInfoSection.kt` |
| `BooksInfoSection.java` | `data/binary/yes2/section/BooksInfoSection.kt` |
| `TextSection.java` | `data/binary/yes2/section/TextSection.kt` |
| `FootnotesSection.java` | `data/binary/yes2/section/FootnotesSection.kt` |
| `XrefsSection.java` | `data/binary/yes2/section/XrefsSection.kt` |
| `PericopesSection.java` | `data/binary/yes2/section/PericopesSection.kt` |
| `Yes1Reader.java` | `data/binary/yes1/Yes1Reader.kt` |
| expect/actual | `RandomAccessSource` (platform I/O) |
| integration | Tests with real YES2 files |

**Critical constraints:**
- NO `java.io` in commonMain  use `expect/actual RandomAccessSource`
- Port pure Java Snappy (NOT JNI native)
- BintexReader: replace DataInputStream with ByteArray + offset
- Always UTF-8 decode YES2 strings

### Phase 14: Data Models & SQLDelight Database  ~13 issues
**Goal:** Port all data models and complete the SQLDelight schema

```sql
Marker(gid, ari, kind, caption, verseCount, createTime, modifyTime, deleted, syncRevision)
Label(gid, title, ordering, backgroundColor, deleted, syncRevision)
MarkerLabel(marker_gid, label_gid)
ProgressMark(preset_id, caption, ari, modifyTime, syncRevision)
InstalledVersion(locale, shortName, longName, filename, ordering, active)
SyncState(syncSetName, revisionId, lastSyncAt)
ReadingPlan(version, name, title, description, duration, startTime)
ReadingPlanProgress(readingPlan_id, reading_code, checkTime)
ReadingHistory(ari, timestamp)
```

Models to port:
`Ari.kt`, `Marker.kt`, `Label.kt`, `Book.kt`, `VersionInfo.kt`,
`PericopeData.kt`, `FootnoteEntry.kt`, `XrefEntry.kt`, `ReadingPlan.kt`, `SyncPayload.kt`

### Phase 15: Core Bible Reader UI  ~20 issues
**Goal:** Full Bible reading experience  
**androidbible source:** `IsiActivity.kt`, `VersesControllerImpl.kt`, `FormattedVerseText.kt`

Composables to build:
```kotlin
BibleReaderScreen    // main screen
VerseList            // LazyColumn
VerseItem            // single verse with formatting
PericopeHeader       // section headers
ChapterPager         // HorizontalPager swipe
DrawerContent        // navigation drawer
VersionsScreen       // download + manage versions
TextAppearancePanel  // font size/face/colors settings
```

Features: verse numbers, red-letter text, pericopes, night mode, footnotes display, cross-ref display, version selector, paragraph formatting

### Phase 16: Navigation & Search  ~12 issues
**Goal:** Complete verse navigation and full-text search  
**androidbible source:** `GotoActivity.kt`, `SearchActivity.java`

- Go-to dialog: Book grid  Chapter grid  Verse grid
- Dialer mode (numeric input `GotoDialerFragment`)
- Direct mode ("John 3:16" text reference)
- Full-text search with highlighting
- Search filters (OT/NT, by book, regex)
- Split-screen parallel reading (two versions)
- Reading history (back/forward)

### Phase 17: Markers & Labels System  ~14 issues
**Goal:** Complete marker system  
**androidbible source:** `Marker.java`, `MarkersActivity.kt`, `Label.java`

- Create/edit/delete bookmarks, highlights (color picker), notes (editor)
- Multi-verse markers (verseCount)
- Labels (create/edit/delete/reorder/color)
- Assign labels to markers (many-to-many)
- Markers list screen (filter by kind/label, sort by date/book)
- Inline marker indicators in verse text
- Context menu on verse long-press

### Phase 18: Sync & Backend Integration  ~15 issues
**Goal:** Full goldenBowl sync protocol client-side

- `SyncEngine.kt`  delta sync logic
- `SyncApi.kt` (Ktor)  `POST /api/sync/`, `GET /api/sync/full`, etc.
- `AuthRepository.kt`  login, register, Google/Apple Sign-In
- `ReverbClient.kt`  WebSocket (Pusher protocol) channel subscription
- Private channel `private-user.{userId}` with event handlers
- Echo prevention (device_id filtering)
- `ConflictResolver.kt`  last-write-wins
- Background sync (WorkManager/BGTasks)
- Offline queue (retry on reconnect)
- Sync status UI indicator

### Phase 19: Reading Plans, Devotions & Songs  ~12 issues
**androidbible source:** `KpriModel`, `SongDb`, reading plan activities

- Reading plan browser (`ReadingPlansScreen.kt`)
- Daily reading assignments
- Progress tracking + calendar view
- Multiple simultaneous plans
- Devotional content display (`DevotionScreen.kt`)
- Song database (SQLDelight: `SongDatabase.sq`)
- Song browsing and search (`SongsScreen.kt`, `SongSearchScreen.kt`)
- Song lyrics display (`SongDetailScreen.kt`)

### Phase 20: Platform Polish & Release  ~15 issues
- Share verse text (platform share sheet)
- Copy to clipboard
- Export/import markers (JSON)
- Android home screen widget (Glance)
- TTS (Text-to-Speech) for verse reading
- Crash reporting (Sentry/Bugsnag KMP)
- Accessibility (TalkBack, VoiceOver)
- App icon + splash screen
- Google Play submission
- Apple App Store submission
- Desktop distribution (GitHub Releases / DMG / MSI)

---

## Feature Parity Matrix Summary

| Phase | Features | Count |
|-------|----------|-------|
| 13 | Binary Readers | 7 |
| 14 | Data Models & Database | 10 |
| 15 | Core Reader + Appearance + Versions | 23 |
| 16 | Navigation + Search + Split | 12 |
| 17 | Markers + Labels | 14 |
| 18 | Backend + Sync | 10 |
| 19 | Plans + Devotions + Songs | 9 |
| 20 | Polish + Platform-Specific | 8 |
| **Total** | | **88 features** |

---

## Running Locally

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run

# iOS (macOS only)
open iosApp/iosApp.xcodeproj

# Tests (all platforms)
./gradlew :shared:testDebugUnitTest
./gradlew :shared:desktopTest
```

---

## Related Repositories

- **API backend:** https://github.com/maxymurm/androidbible-api
- **Original Android app:** https://github.com/maxymurm/androidbible
- **Reference documentation:** C:\Users\maxmm\Downloads\inspiration\inspiration\
