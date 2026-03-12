---
applyTo: '**'
lastUpdated: '2026-03-12'
chatSession: 'session-002'
projectName: 'androidbible-kmp'
---

# Project Memory  androidbible-kmp (BibleCMP Compose Multiplatform App)

> **AGENT INSTRUCTIONS:** Always read this file FIRST before starting any new conversation. Update after completing tasks, making decisions, or when user says "remember this".

---

##  Current Focus

**Active Phase:** Phase 13  Binary Readers (YES2/Bintex/Snappy)  
**Active Issue:** None (scoping re-done with correct inspiration)  
**Current Branch:** main  
**Last Activity:** 2026-03-12  Re-scoped against actual inspiration: androidbible native Java  BibleCMP (Compose Multiplatform) porting guide.

**Phases 112 Complete:**
-  Project scaffolding (KMP shared/androidApp/iosApp/desktopApp modules)
-  Gradle version catalog (libs.versions.toml)
-  Koin, SQLDelight, Ktor, Compose Multiplatform configured
-  Basic DB schema, auth, markers (initial implementation)
-  Sync engine (initial), reading plans, devotionals, songs
-  All 85 original issues closed; committed and pushed

**What's Next (Phase 13+)  androidbible Java  CMP Porting:**
1. Port binary readers (BintexReader, SnappyCodec, Yes2Reader, Yes1Reader)
2. Port data models (Marker, Label, Book, Ari, VersionInfo)
3. Expand SQLDelight schema to match goldenBowl schema
4. Build full Bible reader UI (verse list, chapter paging, pericopes, red letter)
5. Navigation (Go-to dialog: book/chapter/verse, dialer, direct input)
6. Full-text search with filters
7. Complete markers system (bookmark/highlight/note + labels)
8. Backend sync integration (goldenBowl API, delta sync)
9. Reading plans, songs, devotions
10. Platform polish (widgets, sharing, App Store submission)

---

##  User Preferences

### Tech Stack
- **Platform:** Compose Multiplatform (Android + iOS + Desktop)
- **Language:** 100% Kotlin (commonMain + platform-specific)
- **DI:** Koin
- **Database:** SQLDelight (multiplatform SQLite)
- **Networking:** Ktor Client
- **Serialization:** kotlinx.serialization
- **UI:** Compose Multiplatform + Material3
- **Real-time:** Pusher protocol via Ktor WebSocket (Laravel Reverb)
- **Backend:** androidbible-api (goldenBowl reference, writes.gadsda.com)

### Architecture
- **Pattern:** MVVM  ViewModels + StateFlow in commonMain, no business logic in Composables
- **Layers:** ui/  domain/usecase/  data/repository/  data/binary/ or data/db/
- **expect/actual for:** file I/O (RandomAccessSource), platform DB driver, crypto, platform share

### Coding Style
- 100% Kotlin, no Java in shared code
- Conventional commits with "Closes #N"
- MVVM  thin Composables, fat ViewModels
- ARI encoding: `(bookId shl 16) or (chapter shl 8) or verse`

---

##  Project File Map

```
androidbible-kmp/
 .github/instructions/memory.instruction.md   THIS FILE
 agents/AUTONOMOUS_PROMPT_KMP.md
 shared/src/commonMain/kotlin/
    di/                         Koin modules
    data/
       model/                  Marker, Label, Book, VersionInfo, Ari, etc.
       db/                     SQLDelight queries
       repository/             MarkerRepository, LabelRepository, etc.
       binary/
           bintex/             BintexReader.kt, BintexWriter.kt
           snappy/             SnappyCodec.kt, SnappyInputStream.kt
           yes2/               Yes2Reader.kt + section/*.kt
           yes1/               Yes1Reader.kt
    domain/
       usecase/                SearchUseCase, ReadingPlanUseCase, etc.
       sync/                   SyncEngine.kt, ConflictResolver.kt
    network/
       api/                    Ktor client (SyncApi.kt, AuthApi.kt)
       auth/                   AuthService.kt, TokenStorage.kt
       websocket/              ReverbClient.kt (Pusher protocol)
    ui/
       reader/                 BibleReaderScreen, VerseList, VerseItem
       navigation/             GotoScreen, DialerMode, DirectMode, BookGrid
       search/                 SearchScreen, SearchResultItem, SearchFilters
       markers/                MarkersScreen, NoteEditor, HighlightColorPicker
       labels/                 LabelsScreen, LabelEditor
       versions/               VersionsScreen, download manager
       plans/                  ReadingPlansScreen, PlanProgressView
       songs/                  SongsScreen, SongDetailScreen
       devotions/              DevotionScreen
       settings/               TextAppearancePanel, ColorPickerDialog
       auth/                   LoginScreen, RegisterScreen
       theme/                  Material3 theme, colors, typography
    util/
        Ari.kt                  ARI encode/decode utility
        FormattedVerseText.kt   Red-letter, bold, formatting
 shared/src/commonMain/sqldelight/     BibleDatabase.sq, queries
 androidApp/
 iosApp/
 desktopApp/
```

### Feature  File Mapping (from androidbible Java)
| androidbible Source | CMP Target |
|---------------------|------------|
| `Yes2Reader.java` | `data/binary/yes2/Yes2Reader.kt` |
| `BintexReader.java` | `data/binary/bintex/BintexReader.kt` |
| `SnappyImplJava.java` | `data/binary/snappy/SnappyCodec.kt` |
| `Marker.java` | `data/model/Marker.kt` |
| `Label.java` | `data/model/Label.kt` |
| `Ari.java` | `util/Ari.kt` |
| `IsiActivity.kt` | `ui/reader/BibleReaderScreen.kt` |
| `GotoActivity.kt` | `ui/navigation/GotoScreen.kt` |
| `SearchActivity.java` | `ui/search/SearchScreen.kt` |
| `MarkersActivity.kt` | `ui/markers/MarkersScreen.kt` |

---

##  Patterns & Architecture

### Binary Reader Pipeline
```
YES2 file  SnappyInputStream  BintexReader  Section parsers
 VersionInfoSection (metadata)
 BooksInfoSection (book structure)
 TextSection (verse text, UTF-8)
 FootnotesSection
 XrefsSection
 PericopesSection
```

### Critical Binary Rules
- **NO `java.io` in commonMain**  use expect/actual RandomAccessSource
- **Snappy:** Port pure Java impl (`SnappyImplJava`), NOT the JNI native one
- **BintexReader:** Replace DataInputStream with ByteArray + offset tracking
- **UTF-8 decoding:** `String(bytes, Charsets.UTF_8)` or `byteArray.decodeToString()`
- **YES2 strings:** Always UTF-8 decode after BintexReader reads bytes

### ARI Encoding
```kotlin
object Ari {
    fun encode(book: Int, chapter: Int, verse: Int): Int =
        (book shl 16) or (chapter shl 8) or verse
    fun book(ari: Int) = (ari shr 16) and 0xFF
    fun chapter(ari: Int) = (ari shr 8) and 0xFF
    fun verse(ari: Int) = ari and 0xFF
}
```

### SQLDelight Schema (goldenBowl alignment)
```sql
Marker(gid, ari, kind, caption, verseCount, createTime, modifyTime, deleted, syncRevision)
Label(gid, title, ordering, backgroundColor, deleted, syncRevision)
MarkerLabel(marker_gid, label_gid)  -- junction table
ProgressMark(preset_id, caption, ari, modifyTime, syncRevision)
InstalledVersion(locale, shortName, longName, filename, ordering, active)
SyncState(syncSetName, revisionId, lastSyncAt)
```
Marker kinds: 1=bookmark, 2=note, 3=highlight

### Sync Protocol
```
Client changes  SyncEngine  POST /api/sync/  server processes
 server returns delta since client revision
 apply server changes in SQLDelight transaction
 update local syncRevision
 other devices notified via WebSocket (Reverb)
```

### MVVM Pattern
```
Composable  ViewModel (StateFlow)  UseCase  Repository  DB or Network
                                   (no business logic in Composable)
```

---

##  Things to Remember

- **Repo:** https://github.com/maxymurm/androidbible-kmp
- **Reference app:** androidbible Java source (BintexReader.java, Yes2Reader.java, etc.)
- **Reference backend:** goldenBowl (writings.gadsda.com) / androidbible-api
- **Marker kinds:** 1=bookmark, 2=note, 3=highlight (match goldenBowl)
- **GIDs:** UUID v4, generated client-side
- **Sync is transactional:** SQLDelight transaction for applying server changes
- **Echo prevention:** device_id in sync payloads, filter WebSocket events
- **iOS:** Swift only in iosApp entry point  all logic in commonMain Kotlin
- **expect/actual needed for:** RandomAccessSource (file I/O), DB driver, crypto, permissions
- **Platform modules:** androidApp for WorkManager (background sync), iosApp for BGTasks

---

##  Project Statistics

**Phases Completed:** 12 of 12 original  
**In Progress:** Phase 13+ (androidbible native feature porting)  
**88 Features to Port:** per FEATURE_PARITY_MATRIX.md  
**~140 Issues Total:** across 8 new phases (1320)  
**Estimated Effort:** ~340 hours remaining
