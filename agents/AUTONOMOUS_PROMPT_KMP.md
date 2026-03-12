# AUTONOMOUS EXECUTION PROMPT  androidbible-kmp (BibleCMP Compose Multiplatform)

> **FOR AI AGENTS:** This document instructs you on how to autonomously develop the androidbible-kmp Compose Multiplatform Bible app. Read this COMPLETELY before starting any task.

---

## Project Identity

- **Repository:** https://github.com/maxymurm/androidbible-kmp
- **App Name:** BibleCMP
- **Type:** Kotlin Multiplatform + Compose Multiplatform (Android + iOS + Desktop)
- **Source reference:** androidbible Java/Android app (this workspace folder)
- **Backend:** androidbible-api (goldenBowl, writings.gadsda.com)
- **Bible format:** YES2/YES1 binary files (NOT SWORD!)

---

## Mission

Port the native `androidbible` Java/Android app to **Compose Multiplatform** (BibleCMP). This is a feature-by-feature conversion:
- Port binary readers (BintexReader, Snappy, Yes2Reader) to pure Kotlin commonMain
- Build UI from scratch in Compose Multiplatform (Material3)
- Implement MVVM architecture with SQLDelight + Ktor
- Integrate with androidbible-api sync protocol (goldenBowl)
- Ship to Android, iOS, and Desktop

**Phases 112 are complete.** Continue from Phase 13 as specified below.

---

## Operating Rules

1. **Read memory.instruction.md FIRST**  `.github/instructions/memory.instruction.md`
2. **One issue at a time.** Pick ONE open GitHub issue, complete it, commit, push.
3. **100% Kotlin.** No Java in commonMain. Swift only in iosApp entry point.
4. **NO `java.io` in commonMain.** Use `expect/actual` for all platform I/O.
5. **No business logic in Composables.** Use ViewModels + StateFlow.
6. **ARI encoding:** `(bookId shl 16) or (chapter shl 8) or verse`
7. **YES2 strings:** Always UTF-8 decode after reading bytes from BintexReader.
8. **Sync is transactional.** Wrap SQLDelight mutations in transactions.
9. **Commit format:** `feat(scope): description [Closes #N]`
10. **Tests:** Write `kotlin.test` / JUnit5 tests for all business logic.
11. **Update memory file** after every significant decision.

---

## Architecture Reference

### Module Structure
```
androidbible-kmp/
 shared/                  # 100% Kotlin KMP
    src/commonMain/kotlin/
        data/binary/     # YES2, Bintex, Snappy readers
        data/model/      # Domain models
        data/db/         # SQLDelight
        data/repository/ # Repository implementations
        domain/usecase/  # Business logic
        domain/sync/     # Sync engine
        network/         # Ktor HttpClient
        ui/              # Compose screens & ViewModels
        util/            # Ari.kt, FormattedVerseText.kt
 androidApp/              # Android-specific entry point + WorkManager
 iosApp/                  # Swift entry point (thin shell)
 desktopApp/              # Desktop entry point
```

### MVVM Pattern
```kotlin
// CORRECT: UI  ViewModel  UseCase  Repository
@Composable
fun BibleReaderScreen(viewModel: BibleReaderViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // render uiState only, no business logic
}

class BibleReaderViewModel(
    private val getChapterUseCase: GetChapterUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<BibleReaderUiState>(BibleReaderUiState.Loading)
    val uiState: StateFlow<BibleReaderUiState> = _uiState.asStateFlow()
    
    fun loadChapter(ari: Int) {
        viewModelScope.launch {
            _uiState.value = BibleReaderUiState.Success(getChapterUseCase(ari))
        }
    }
}
```

### Binary Reader Pattern
```kotlin
// YES2 reading pipeline:
// 1. Load file via platform-specific RandomAccessSource (expect/actual)
// 2. Create BintexReader(source)
// 3. Create Yes2Reader(bintexReader)
// 4. Parse sections (section index  seek  parse)
// 5. UTF-8 decode all strings

// Port from Java WITHOUT java.io:
// DataInputStream  ByteArray + BinaryReader offset tracking
// RandomAccessFile  expect class RandomAccessSource
// String(bytes, "UTF-8")  bytes.decodeToString()
```

### RandomAccessSource (expect/actual)
```kotlin
// commonMain
expect class RandomAccessSource(path: String) {
    fun read(offset: Long, length: Int): ByteArray
    fun close()
    val length: Long
}

// androidMain
actual class RandomAccessSource actual constructor(path: String) {
    private val raf = java.io.RandomAccessFile(path, "r")
    actual fun read(offset: Long, length: Int): ByteArray { ... }
    actual fun close() = raf.close()
    actual val length get() = raf.length()
}

// iosMain / desktopMain  similar implementations
```

### SQLDelight Pattern
```kotlin
// Define schema in BibleDatabase.sq
// Use generated queries:
val db: BibleDatabase = // injected via Koin
db.markerQueries.selectByAri(ari).executeAsList()
db.markerQueries.insert(gid, ari, kind, caption, verseCount, now, now)

// Transactions
db.transaction {
    serverMarkers.forEach { db.markerQueries.upsert(...) }
    db.syncStateQueries.updateRevision("all", newRevision)
}
```

### Koin DI
```kotlin
// shared/src/commonMain/kotlin/di/AppModule.kt
val appModule = module {
    // Repositories
    single<MarkerRepository> { MarkerRepositoryImpl(get()) }
    single<LabelRepository> { LabelRepositoryImpl(get()) }
    single<VersionRepository> { VersionRepositoryImpl(get(), get()) }
    
    // Use Cases
    factory { GetChapterUseCase(get()) }
    factory { SearchVersesUseCase(get()) }
    factory { SyncUseCase(get(), get()) }
    
    // ViewModels
    viewModel { BibleReaderViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { MarkersViewModel(get()) }
    
    // Network
    single { createHttpClient() }
    single<SyncApi> { SyncApiImpl(get()) }
    single<AuthApi> { AuthApiImpl(get()) }
}
```

### Sync Protocol (client-side)
```kotlin
class SyncEngine(
    private val syncApi: SyncApi,
    private val db: BibleDatabase,
    private val deviceId: String
) {
    suspend fun sync() {
        val revision = db.syncStateQueries.getRevision("all").executeAsOne().revisionId
        
        // Collect local changes
        val localMarkers = db.markerQueries.changesSince(revision).executeAsList()
        val localLabels = db.labelQueries.changesSince(revision).executeAsList()
        
        // POST to /api/sync/
        val response = syncApi.sync(SyncRequest(
            revision = revision,
            deviceId = deviceId,
            markers = localMarkers.map { it.toDto() },
            labels = localLabels.map { it.toDto() }
        ))
        
        // Apply server changes in SQLDelight transaction
        db.transaction {
            response.markers.forEach { applyServerMarker(it) }
            response.labels.forEach { applyServerLabel(it) }
            db.syncStateQueries.updateRevision("all", response.serverRevision)
        }
    }
}
```

### WebSocket (Reverb/Pusher protocol)
```kotlin
class ReverbClient(
    private val client: HttpClient,
    private val authApi: SyncApi,
    private val userId: Long
) {
    suspend fun connect(onEvent: (String, String) -> Unit) {
        client.webSocket("wss://writings.gadsda.com/app/REVERB_APP_KEY") {
            // Subscribe to private channel
            val authResponse = authApi.authenticateChannel(socketId, "private-user.$userId")
            sendSubscribe("private-user.$userId", authResponse.auth)
            
            // Listen for events
            for (frame in incoming) {
                val event = parseEvent(frame)
                if (event.channel == "private-user.$userId") {
                    if (event.eventName != "marker.created" || event.deviceId != localDeviceId) {
                        onEvent(event.eventName, event.data)
                    }
                }
            }
        }
    }
}
```

---

## Data Models Reference

### Marker
```kotlin
data class Marker(
    val id: Long = 0,
    val gid: String,        // UUID v4
    val ari: Int,           // ARI encoded verse reference
    val kind: Kind,
    val caption: String = "",
    val verseCount: Int = 1,
    val createTime: Long,   // epoch millis
    val modifyTime: Long,
    val deleted: Boolean = false
) {
    enum class Kind(val value: Int) {
        BOOKMARK(1), NOTE(2), HIGHLIGHT(3)
    }
}
```

### ARI Utility
```kotlin
object Ari {
    fun encode(book: Int, chapter: Int, verse: Int): Int =
        (book shl 16) or (chapter shl 8) or verse
    fun bookNumber(ari: Int): Int = (ari shr 16) and 0xFF
    fun chapter(ari: Int): Int = (ari shr 8) and 0xFF
    fun verse(ari: Int): Int = ari and 0xFF
    fun toChapterStart(book: Int, chapter: Int): Int = encode(book, chapter, 0)
    fun toChapterEnd(book: Int, chapter: Int): Int = encode(book, chapter, 0xFF)
}
```

---

## Phase Execution Plan

### CURRENT: Phase 13  Binary Readers (~14 issues)
**Milestone:** "Phase 13: Binary Readers"

1. `[KMP-BR-01]` `BintexReader.kt`  port BintexReader.java to pure Kotlin commonMain
2. `[KMP-BR-02]` `BintexWriter.kt`  port BintexWriter.java
3. `[KMP-BR-03]` `SnappyCodec.kt`  port SnappyImplJava (pure Java, NOT JNI)
4. `[KMP-BR-04]` `SnappyInputStream.kt`  port compressed stream reader
5. `[KMP-BR-05]` `expect/actual RandomAccessSource`  platform file I/O abstraction
6. `[KMP-BR-06]` `Yes2Reader.kt` skeleton  open file, read section index
7. `[KMP-BR-07]` `VersionInfoSection.kt`  parse version metadata
8. `[KMP-BR-08]` `BooksInfoSection.kt`  parse book structure
9. `[KMP-BR-09]` `TextSection.kt`  parse verse text (UTF-8 decode required)
10. `[KMP-BR-10]` `FootnotesSection.kt`  parse footnotes
11. `[KMP-BR-11]` `XrefsSection.kt`  parse cross-references
12. `[KMP-BR-12]` `PericopesSection.kt`  parse pericope (section) headings
13. `[KMP-BR-13]` `Yes1Reader.kt`  legacy YES1 format reader
14. `[KMP-BR-14]` Integration tests with real YES2 files (all platforms: Android JVM, Desktop, iOS Simulator)

### Phase 14  Data Models & SQLDelight (~13 issues)
**Milestone:** "Phase 14: Data Models"

1. `[KMP-DM-01]` `Ari.kt` utility (encode/decode/chapter range)
2. `[KMP-DM-02]` `Marker.kt` data class (Kind enum: BOOKMARK=1, NOTE=2, HIGHLIGHT=3)
3. `[KMP-DM-03]` `Label.kt` data class
4. `[KMP-DM-04]` `Book.kt` and `VersionInfo.kt`
5. `[KMP-DM-05]` `SyncPayload.kt` DTOs (kotlinx.serialization)
6. `[KMP-DM-06]` SQLDelight schema: `BibleDatabase.sq` + complete table definitions
7. `[KMP-DM-07]` `MarkerQueries.sq` (selectByAri, selectByChapter, selectByLabel, insert, update, softDelete, changesSince)
8. `[KMP-DM-08]` `LabelQueries.sq` + `MarkerLabel` junction queries
9. `[KMP-DM-09]` `VersionQueries.sq` (selectActive, insert, updateOrdering, toggleActive)
10. `[KMP-DM-10]` `MarkerRepository` implementation
11. `[KMP-DM-11]` `LabelRepository` implementation
12. `[KMP-DM-12]` Database driver expect/actual (Android/iOS/Desktop)
13. `[KMP-DM-13]` Unit tests for all models and repository queries

### Phase 15  Core Bible Reader UI (~20 issues)
**Milestone:** "Phase 15: Core Reader UI"

1. `[KMP-UI-01]` `GetChapterUseCase`  retrieve verse list for a chapter via Yes2Reader
2. `[KMP-UI-02]` `BibleReaderViewModel`  StateFlow for current ari, verseList, loading
3. `[KMP-UI-03]` `VerseItem.kt` composable  verse number + text + marker indicators
4. `[KMP-UI-04]` `PericopeHeader.kt` composable  section heading display
5. `[KMP-UI-05]` `VerseList.kt`  LazyColumn of VerseItems + PericopeHeaders
6. `[KMP-UI-06]` `ChapterPager.kt`  HorizontalPager with swipe + ViewModel integration
7. `[KMP-UI-07]` `FormattedVerseText.kt`  red-letter, bold, paragraph formatting
8. `[KMP-UI-08]` `DrawerContent.kt`  navigation drawer (book list, settings)
9. `[KMP-UI-09]` `BibleReaderScreen.kt`  full main screen with drawer + pager
10. `[KMP-UI-10]` Night mode (Material3 dark theme toggle)
11. `[KMP-UI-11]` `TextAppearancePanel.kt`  font size, face, line spacing, colors
12. `[KMP-UI-12]` `VersionsScreen.kt`  list + download + activate/deactivate versions
13. `[KMP-UI-13]` `VersionRepository` + download manager (Ktor)
14. `[KMP-UI-14]` Footnote display in reader (bottom sheet or inline)
15. `[KMP-UI-15]` Cross-reference display (navigable links)
16. `[KMP-UI-16]` Reading history (back/forward navigation stack in ViewModel)
17. `[KMP-UI-17]` `VersionSelectorBottomSheet`  quick switch between active versions
18. `[KMP-UI-18]` Bundled internal version (asset-based YES2 file for offline use)
19. `[KMP-UI-19]` Per-version font size settings
20. `[KMP-UI-20]` UI tests for reader composables

### Phase 16  Navigation & Search (~12 issues)
**Milestone:** "Phase 16: Navigation & Search"

1. `[KMP-NAV-01]` `GotoScreen.kt`  3-mode dialog (Book/Chapter/Verse grid)
2. `[KMP-NAV-02]` `BookGrid.kt`  visual OT/NT book selection
3. `[KMP-NAV-03]` `ChapterGrid.kt`  chapter number picker
4. `[KMP-NAV-04]` `DialerMode.kt`  numeric dialer for reference input
5. `[KMP-NAV-05]` `DirectMode.kt`  text input ("John 3:16" parsing)
6. `[KMP-NAV-06]` `SearchScreen.kt`  search input + results list
7. `[KMP-NAV-07]` `SearchUseCase`  query across active versions via Yes2Reader
8. `[KMP-NAV-08]` `SearchResultItem.kt`  highlighted snippet display
9. `[KMP-NAV-09]` `SearchFilters.kt`  OT/NT/book filter + regex toggle
10. `[KMP-NAV-10]` `SplitReader.kt`  split-screen with two version panes
11. `[KMP-NAV-11]` `SplitReaderViewModel`  two independent reader ViewModels
12. `[KMP-NAV-12]` Deep link parsing for verse references (Android Intent / iOS URL scheme)

### Phase 17  Markers & Labels System (~14 issues)
**Milestone:** "Phase 17: Markers System"

1. `[KMP-MK-01]` Verse long-press context menu (bookmark/highlight/note options)
2. `[KMP-MK-02]` `HighlightColorPicker.kt`  6-color palette
3. `[KMP-MK-03]` `NoteEditor.kt`  multi-line note editor with save/cancel
4. `[KMP-MK-04]` `CreateMarkerUseCase`  create/update/delete with sync revision
5. `[KMP-MK-05]` `MarkersScreen.kt`  tabbed list (Bookmarks / Notes / Highlights)
6. `[KMP-MK-06]` `MarkersViewModel`  filter by kind/label, sort by date/book
7. `[KMP-MK-07]` Inline marker indicators in `VerseItem` (underline/circle/color)
8. `[KMP-MK-08]` `LabelsScreen.kt`  list labels with reorder + color + delete
9. `[KMP-MK-09]` `LabelEditor.kt`  create/edit label with color picker
10. `[KMP-MK-10]` `LabelManagementViewModel`
11. `[KMP-MK-11]` Assign labels to markers (many-to-many, MarkerLabel junction)
12. `[KMP-MK-12]` Filter markers by label in `MarkersScreen`
13. `[KMP-MK-13]` Multi-verse marker creation (verseCount > 1)
14. `[KMP-MK-14]` Unit + UI tests for markers system

### Phase 18  Sync & Backend Integration (~15 issues)
**Milestone:** "Phase 18: Sync & Realtime"

1. `[KMP-SY-01]` `AuthApi.kt` (Ktor)  login, register, oauth, logout, account delete
2. `[KMP-SY-02]` `AuthRepository`  token storage (expect/actual per platform: Keystore/Keychain)
3. `[KMP-SY-03]` Google Sign-In integration (Android: Google SDK; iOS: Google SDK)
4. `[KMP-SY-04]` Apple Sign-In integration (iOS only: ASAuthorization)
5. `[KMP-SY-05]` `SyncApi.kt` (Ktor)  POST /api/sync/, GET /full, /delta, /status
6. `[KMP-SY-06]` `SyncEngine.kt`  delta sync logic (collect changes, push, apply response)
7. `[KMP-SY-07]` `ConflictResolver.kt`  last-write-wins based on modifyTime
8. `[KMP-SY-08]` `ReverbClient.kt`  WebSocket Pusher protocol (subscribe, receive events)
9. `[KMP-SY-09]` Echo prevention  filter events by device_id
10. `[KMP-SY-10]` Background sync (Android: WorkManager; iOS: BGTasks  expect/actual)
11. `[KMP-SY-11]` Offline mutation queue  store pending changes, retry on reconnect
12. `[KMP-SY-12]` `SyncStatusIndicator` composable  show syncing/synced/error state
13. `[KMP-SY-13]` Device registration on first launch
14. `[KMP-SY-14]` Token refresh handling on 401
15. `[KMP-SY-15]` Integration tests for sync round-trip

### Phase 19  Reading Plans, Devotions & Songs (~12 issues)
**Milestone:** "Phase 19: Content Features"

1. `[KMP-CO-01]` `ReadingPlansScreen.kt`  browse plans (from server or bundled)
2. `[KMP-CO-02]` `ReadingPlanUseCase`  get daily assignments, mark complete
3. `[KMP-CO-03]` `PlanProgressView.kt`  calendar streak visualization
4. `[KMP-CO-04]` Multiple simultaneous reading plans support
5. `[KMP-CO-05]` `DevotionScreen.kt`  daily devotional display
6. `[KMP-CO-06]` `DevotionRepository`  fetch from API or local DB
7. `[KMP-CO-07]` `SongDatabase.sq`  SQLDelight schema for KpriModel songs
8. `[KMP-CO-08]` `SongsScreen.kt`  browse hymns by number/title
9. `[KMP-CO-09]` `SongSearchScreen.kt`  search songs
10. `[KMP-CO-10]` `SongDetailScreen.kt`  display lyrics
11. `[KMP-CO-11]` Seed songs from bundled source
12. `[KMP-CO-12]` Feature tests for content screens

### Phase 20  Platform Polish & Release (~15 issues)
**Milestone:** "Phase 20: Release"

1. `[KMP-PL-01]` Share verse text (platform share sheet  expect/actual)
2. `[KMP-PL-02]` Copy verse to clipboard (platform clipboard)
3. `[KMP-PL-03]` Export markers to JSON file
4. `[KMP-PL-04]` Import markers from JSON file
5. `[KMP-PL-05]` Android home screen widget (Glance API)
6. `[KMP-PL-06]` TTS  Text-to-Speech for verse reading
7. `[KMP-PL-07]` Sentry/Bugsnag crash reporting (KMP SDK)
8. `[KMP-PL-08]` Accessibility audit (TalkBack, VoiceOver)
9. `[KMP-PL-09]` Localization / i18n setup
10. `[KMP-PL-10]` App icon + splash screen (Android + iOS + Desktop)
11. `[KMP-PL-11]` Performance optimization (LazyColumn, image caching)
12. `[KMP-PL-12]` Google Play store listing + submission
13. `[KMP-PL-13]` Apple App Store listing + submission
14. `[KMP-PL-14]` Desktop distribution (GitHub Releases: DMG + MSI + DEB)
15. `[KMP-PL-15]` Production backend URL config (writings.gadsda.com)

---

## Step-by-Step Autonomous Workflow

```
LOOP:
  1. Read .github/instructions/memory.instruction.md
  2. Run: gh issue list --repo maxymurm/androidbible-kmp --state open --limit 20
  3. Pick the LOWEST numbered open issue (follows phase order)
  4. Read the issue body fully
  5. Identify the corresponding Java source in androidbible workspace
  6. Check out feature branch: git checkout -b feat/issue-N-short-description
  7. Port/implement following patterns above
  8. Write kotlin.test / JUnit5 test
  9. Run: ./gradlew :shared:testDebugUnitTest (must pass)
  10. Commit: git commit -m "feat(scope): description [Closes #N]"
  11. Push: git push origin feat/issue-N-short-description
  12. Merge to main (or create PR)
  13. Update memory.instruction.md
  14. GOTO 1
```

---

## Source Reference Files in androidbible Workspace

When porting, always check these Java/Kotlin files first:

| Task | androidbible Source Path |
|------|--------------------------|
| BintexReader | `BintexReader/src/main/java/.../BintexReader.java` |
| BintexWriter | `BintexWriter/src/main/java/.../BintexWriter.java` |
| Snappy decompression | `Snappy/src/main/java/.../SnappyImplJava.java` |
| YES2 reading | `AlkitabYes2/src/main/java/.../yes2/Yes2Reader.java` |
| YES2 sections | `AlkitabYes2/src/main/java/.../yes2/section/*.java` |
| Marker model | `AlkitabModel/src/main/java/.../model/Marker.java` |
| Label model | `AlkitabModel/src/main/java/.../model/Label.java` |
| ARI utility | `AlkitabModel/src/main/java/.../model/Ari.java` |
| S singleton | `Alkitab/src/main/java/.../S.kt` |
| Bible reader | `Alkitab/src/main/java/.../IsiActivity.kt` |
| Goto dialog | `Alkitab/src/main/java/.../GotoActivity.kt` |
| Search | `Alkitab/src/main/java/.../SearchActivity.java` |
| Song model | `KpriModel/src/main/java/.../KpriModel.java` |

---

## Anti-Patterns (Never Do)

-  Don't use SWORD/CrossWire  this app uses YES2/YES1 binary format
-  Don't use `java.io` classes in commonMain  use expect/actual
-  Don't put business logic in Composables  use ViewModels
-  Don't use Room  use SQLDelight (multiplatform)
-  Don't use Retrofit  use Ktor Client
-  Don't use Hilt  use Koin
-  Don't use LiveData  use StateFlow/SharedFlow
-  Don't skip UTF-8 decode on YES2 bytes
-  Don't sync outside a SQLDelight transaction
-  Don't ignore device_id echo prevention in WebSocket events
