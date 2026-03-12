# AUTONOMOUS EXECUTION PROMPT — Android Bible KMP
## Document Type: Opus-Level Autonomous Execution Prompt
## Version: 1.0
## Date: 2026-03-12
## Track: Mobile (Kotlin Multiplatform + Compose Multiplatform)

---

## ⚡ YOLO MODE — FULL AUTONOMOUS EXECUTION

You are an expert autonomous AI agent executing work on the **Android Bible KMP** — a Kotlin Multiplatform (KMP) + Compose Multiplatform mobile app for Android and iOS.

**DO NOT** ask for clarification. **DO NOT** stop for approval. Work through open issues one by one, committing after each, until ALL issues are resolved or you run out of context.

---

## 🔐 Repository Access

```
Repo:    https://github.com/maxymurm/androidbible-kmp.git
Branch:  main
Remote:  origin
```

### Before You Start
1. `cd` into the project root
2. Read `.github/instructions/memory.instruction.md` — project state, architecture, patterns
3. Read `docs/PROJECT_DOCUMENTATION.md` — full architecture reference
4. Run `./gradlew :shared:allTests` to confirm all tests pass before starting

---

## ✅ What Has Been Completed (Phases 1–12)

All original Android Bible KMP phases are complete:

- **KMP project setup**: Gradle Kotlin DSL, version catalog (`libs.versions.toml`), shared/composeApp/androidApp/iosApp modules
- **SQLDelight**: Core schema (BibleVersion, Marker, MarkerLabel, ReadingPlan, SyncQueueItem, ReadingHistoryItem)
- **Ktor HTTP client**: kotlinx.serialization DTOs, ApiClient.kt
- **Koin DI**: AppModule.kt with all repositories and ViewModels
- **Auth flow**: LoginScreen, RegisterScreen, DataStore token storage
- **Bible reader core**: BibleReaderScreen, BookSelectorScreen, ChapterSelectorScreen, VersionSelectorScreen
- **Markers system**: BookmarksScreen, NotesScreen, Marker bottom sheet, LabelsScreen
- **Reading Plans + Devotionals + Songs**: full UI implemented
- **Material 3 theme**: dark/light, custom typography (Gentium for verse text)
- **Sync queue**: SyncQueueItem table, mutations enqueued on local writes
- **CI/CD**: `.github/workflows/ci.yml`, fastlane

---

## 📋 Open Issues to Implement

Issues: https://github.com/maxymurm/androidbible-kmp/issues

### Phase 13: SWORD Engine & Modules (Milestone #1)

| # | Title | Priority |
|---|-------|----------|
| #1 | Epic: Pure-Kotlin SWORD Engine | Epic |
| #2 | SwordVersification.kt: verse count tables for 8 systems | P0 |
| #3 | SwordModuleConfig.kt: parser for SWORD .conf files | P0 |
| #4 | ZTextDriver.kt: compressed Bible text reader (zText) | P0 |
| #5 | RawTextDriver.kt: uncompressed Bible text reader | P0 |
| #6 | RawComDriver.kt: commentary module reader | P0 |
| #7 | RawLD4Driver.kt and ZLDDriver.kt: dictionary readers | P0 |
| #8 | SwordEngine.kt: facade combining all drivers | P0 |
| #9 | expect/actual: SwordModuleInitializer | P0 |
| #10 | Module browser screen | P1 |
| #11 | Module download manager | P1 |
| #12 | Pins feature: PinsRepository, PinsViewModel, PinsScreen | P1 |
| #29 | Onboarding flow: 3-step setup wizard | P1 |

### Phase 14: Study Tools (Milestone #2)

| # | Title | Priority |
|---|-------|----------|
| #30 | Commentary screen and panel in Bible reader | P1 |
| #31 | Dictionary screen for lexicon/dictionary modules | P1 |
| #32 | Strong's word study panel: tap word to see lexicon entry | P1 |
| #33 | Bookmark folders: hierarchical folder UI | P2 |
| #34 | Enhanced highlight palette: 6-color selection UI | P2 |
| #35 | Rich-text note editor with Markdown support | P2 |

### Phase 15: User Profile & History (Milestone #3)

| # | Title | Priority |
|---|-------|----------|
| #36 | History screen: reading history with calendar heatmap | P2 |
| #37 | Profile screen: user info, stats, and settings | P2 |
| #38 | Reading statistics screen: streaks and charts | P2 |
| #39 | Audio player: ExoPlayer (Android) and AVAudioPlayer (iOS) | P2 |

### Phase 16: Advanced Features (Milestone #4)

| # | Title | Priority |
|---|-------|----------|
| #40 | Advanced search screen: phrase, Strong's, morphology | P3 |
| #41 | Parallel translation view | P3 |
| #42 | Verse image generation and sharing screen | P3 |
| #43 | Data export: DOCX and PDF | P3 |
| #44 | Tags / collection system | P3 |

---

## 🏗️ Architecture Reference

### Directory Layout
```
shared/src/commonMain/kotlin/
├── data/
│   ├── local/          ← SQLDelight generated code, DAO wrappers
│   ├── remote/         ← Ktor API client, DTOs
│   └── repository/     ← Repository implementations
├── domain/
│   ├── model/          ← Domain models (Verse, Marker, etc.)
│   └── usecase/        ← Use cases
├── presentation/
│   └── viewmodel/      ← ViewModels + UiState sealed classes
└── di/
    └── AppModule.kt    ← Koin modules

composeApp/src/commonMain/kotlin/ui/
├── screens/            ← All Compose screens
├── components/         ← Reusable composables
└── theme/              ← Material3 theme
```

### Key Patterns

**ViewModel + StateFlow:**
```kotlin
class ExampleViewModel(private val repo: ExampleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            repo.getAll().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }
}

data class ExampleUiState(
    val items: List<ExampleItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

**SQLDelight repository:**
```kotlin
class ExampleRepositoryImpl(private val db: AppDatabase) : ExampleRepository {
    override fun getAll(): Flow<List<ExampleItem>> =
        db.exampleQueries.selectAll().asFlow().mapToList(Dispatchers.IO)
}
```

**Compose Screen:**
```kotlin
@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = koinViewModel(),
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ... UI
}
```

**expect/actual for platform-specific code:**
```kotlin
// commonMain
expect class FileReader() {
    fun readBytes(path: String, offset: Long, length: Int): ByteArray
}

// androidMain
actual class FileReader {
    actual fun readBytes(path: String, offset: Long, length: Int): ByteArray {
        // Android RandomAccessFile implementation
    }
}

// iosMain
actual class FileReader {
    actual fun readBytes(path: String, offset: Long, length: Int): ByteArray {
        // iOS NSFileHandle implementation
    }
}
```

**Koin registration:**
```kotlin
// In AppModule.kt
val appModule = module {
    single { ExampleRepositoryImpl(get()) as ExampleRepository }
    viewModel { ExampleViewModel(get()) }
}
```

**ARI encoding:**
```kotlin
val ari = (bookId shl 16) or (chapter shl 8) or verse
val bookId = (ari shr 16) and 0xFF
val chapter = (ari shr 8) and 0xFF
val verse = ari and 0xFF
```

---

## 🗡️ SWORD Engine Implementation Guide

This is the most critical Phase 13 work. Model after PocketSword's KotlinSword module.

### Architecture
```
shared/src/commonMain/kotlin/sword/
├── SwordEngine.kt              ← Main facade
├── SwordModuleConfig.kt        ← .conf file parser
├── SwordVersification.kt       ← Verse count tables per system
├── drivers/
│   ├── ZTextDriver.kt          ← zText compressed Bible
│   ├── RawTextDriver.kt        ← RawText uncompressed
│   ├── RawComDriver.kt         ← commentary
│   ├── RawLD4Driver.kt         ← dictionary (uncompressed)
│   └── ZLDDriver.kt            ← dictionary (compressed)
└── platform/
    └── SwordModuleInitializer.kt  ← expect class for file I/O
```

### zText Binary Format
```
.bzv file — verse index:
  - 8 bytes per verse: [4-byte block number][4-byte verse start offset within block]

.bzz file — block data:
  Block header: [compressed_size: 4 bytes][uncompressed_size: 4 bytes]
  Block data: zlib-compressed text (all verses in block concatenated with \n)

Algorithm:
1. Calculate verse index position: (ari_offset_from_start_of_book) * 8
2. Read 8 bytes → blockNum + verseOffset
3. Seek to block in .bzz, read compressed_size + uncompressed_size
4. Read compressed_size bytes, inflate with zlib
5. Extract verse starting at verseOffset
```

### RawCom Binary Format
```
.idx file — 6 bytes per verse: [4-byte offset][2-byte length]
.dat file — raw text

Algorithm:
1. Calculate verse index position similarly to zText
2. Read 6 bytes → offset + length
3. Seek to offset in .dat, read length bytes
```

### Versification
```kotlin
object SwordVersification {
    // KJV verse counts per book
    val KJV = mapOf(
        1 to intArrayOf(31,25,24,26,32,22,24,22,29,32,32,20,18,24,21,16,27,33,38,18,34,24,20,67,34,35,46,22,35,43,55,32,20,31,29,43,36,30,23,23,57,38,34,34,28,34,31,22,33,26),
        // ... all 66 books
    )
    
    fun getVerseCount(book: Int, chapter: Int, versification: String = "KJV"): Int
    fun getChapterCount(book: Int, versification: String = "KJV"): Int
    fun getAriOffset(book: Int, chapter: Int, verse: Int, versification: String = "KJV"): Int
}
```

---

## 🧰 Build & Test Commands

```bash
# Build all
./gradlew build

# Run shared tests
./gradlew :shared:allTests

# Run Android test
./gradlew :androidApp:connectedAndroidTest

# Install debug on connected device
./gradlew :androidApp:installDebug

# Clean
./gradlew clean

# Generate SQLDelight code
./gradlew generateCommonMainDatabaseInterface
```

---

## 📐 Execution Rules

1. **Read each issue fully** before implementing
2. **Write unit tests** for all SWORD engine classes (`*DriverTest.kt`, `*EngineTest.kt`)
3. **One commit per issue** with message `feat: {description} (Closes #N)`
4. **Run `./gradlew :shared:allTests`** before every commit
5. **Update `.github/instructions/memory.instruction.md`** after completing each issue
6. **Always use expect/actual** for platform-specific file I/O and audio
7. **SWORD engine work first** — it's the foundation for commentary, dictionary, Strong's screens
8. **YOLO** — make architectural decisions. Commit and move on.

---

## 🚦 Implementation Order

**Phase 13 (foundational SWORD work first):**
#2 → #3 → #4 → #5 → #6 → #7 → #8 → #9 → #10 → #11 → #12 → #29 → close #1 epic

**Phase 14 (study tools building on SWORD):**
#30 → #31 → #32 → #33 → #34 → #35

**Phase 15 (user features):**
#36 → #37 → #38 → #39

**Phase 16 (advanced):**
#40 → #41 → #42 → #43 → #44

---

## 📚 Reference Projects

- **PocketSword KotlinSword** — reference Kotlin SWORD implementation:
  `pocketsword/mobile/shared/src/commonMain/kotlin/sword/`
- **PocketSword Android** — reference Compose screens:
  `pocketsword/mobile/composeApp/`

When in doubt about how to implement a SWORD driver or screen, look at PocketSword first.

---

## 📝 Final Note

YOLO mode. You have full authority to:
- Create, modify, delete any files in this repo
- Add SQLDelight tables and migrations
- Commit and push at any point
- Close GitHub issues
- Make architectural decisions within established KMP/Compose patterns

**Critical**: Always implement expect/actual properly for platform code. Never use Android-specific APIs in `commonMain`.

Go. Start with issue #2 (SwordVersification.kt).
