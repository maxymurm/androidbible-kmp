---
applyTo: '**'
lastUpdated: '2026-03-12 00:00'
chatSession: 'session-001'
projectName: 'Android Bible KMP'
---

# Project Memory - Android Bible KMP

> **AGENT INSTRUCTIONS:** Always read this file FIRST before starting any new conversation. Update after completing tasks, making decisions, or when user says "remember this".

---

## 🎯 Current Focus

**Active Phase:** Phase 13 — Feature Parity & Enhancement (inspired by PocketSword)
**Active Issue:** None (scoping just completed)
**Current Branch:** main
**Last Activity:** 2026-03-12 — Full project initialization, agent setup, new issues created

**What Was Accomplished (Phases 1-12):**
- ✅ Phase 1: KMP project setup (Gradle multiplatform, shared/androidApp/iosApp modules)
- ✅ Phase 2: Core data layer (SQLDelight schema, repositories, models)
- ✅ Phase 3: DI with Koin, Ktor HTTP client, network layer
- ✅ Phase 4: Authentication flow UI (login, register, token management)
- ✅ Phase 5: Bible reader core (navigation, content rendering)
- ✅ Phase 6: Markers/Annotations (bookmarks, notes, highlights)
- ✅ Phase 7-8: Reading plans, devotionals, song books
- ✅ Phase 9: Modern Compose UI (Material3 dark/light themes, fonts)
- ✅ Phase 10: Testing (unit + integration)
- ✅ Phase 11: CI/CD (GitHub Actions, fastlane, signed builds)
- ✅ Phase 12: Documentation (README, KDoc, architecture diagram)

**New Issues Created (Phase 13+):**
- See GitHub issues for androidbible-kmp repo

**Next Steps:**
1. Implement pure-Kotlin SWORD engine (binary file readers)
2. Module browser + installer screen
3. Pins screen + PinsRepository
4. Bookmark folders UI
5. Commentary/Dictionary screens
6. Strong's word study panel
7. Onboarding flow
8. Audio player (expect/actual per platform)

---

## 👤 User Preferences

### Project-Specific
- **Tech Stack:** Kotlin Multiplatform 2.x, Compose Multiplatform, SQLDelight, Ktor, Koin
- **Platforms:** Android API 21+, iOS 14+
- **Architecture:** Clean architecture, MVVM in presentation layer, Repository pattern
- **Navigation:** Compose Navigation (shared)
- **DI:** Koin 3.x
- **Networking:** Ktor with kotlinx.serialization

### Coding Style
- **Language:** Kotlin idiomatic (data classes, sealed interfaces, coroutines, flows)
- **State:** StateFlow + ViewModel pattern
- **Commits:** Conventional commits with "Closes #N"
- **expect/actual:** Used for platform-specific: audio, file I/O, notifications

### Git Workflow
- **Branch:** main
- **Commits:** Conventional commits
- **Auto-push:** After every commit
- **CI:** GitHub Actions (build + test matrix Android/iOS)

---

## 📁 Project File Map

### Key Directories
```
androidbible-kmp/
├── .github/
│   ├── instructions/memory.instruction.md  ← THIS FILE
│   ├── ISSUE_TEMPLATE/
│   └── workflows/ci.yml
├── agents/                  ← AI automation templates
├── docs/
│   └── PROJECT_DOCUMENTATION.md
├── shared/
│   ├── src/commonMain/
│   │   ├── data/            ← Repositories, models, DAOs
│   │   ├── domain/          ← Use cases, domain models
│   │   ├── presentation/    ← ViewModels, UI state classes
│   │   └── di/              ← Koin modules
│   ├── src/androidMain/     ← Android-specific implementations
│   └── src/iosMain/         ← iOS-specific implementations
├── composeApp/
│   └── src/commonMain/
│       └── ui/
│           ├── screens/     ← All Compose screens
│           ├── components/  ← Reusable composables
│           └── theme/       ← Material3 theme, typography, colors
├── androidApp/
│   └── src/main/            ← Android entry point
└── iosApp/
    └── iosApp/              ← iOS entry point (Swift wrapper)
```

### Feature → Screen Mapping
- **Auth:** `LoginScreen.kt`, `RegisterScreen.kt`
- **Bible Reader:** `BibleReaderScreen.kt`, `BibleReaderViewModel.kt`
- **Book/Chapter Selector:** `BookSelectorScreen.kt`, `ChapterSelectorScreen.kt`
- **Bookmarks:** `BookmarksScreen.kt`, `BookmarksViewModel.kt`
- **Notes:** `NotesScreen.kt`
- **Highlights:** handled inline in BibleReader
- **Labels:** `LabelsScreen.kt`
- **Reading Plans:** `ReadingPlansScreen.kt`
- **Songs:** `SongsScreen.kt`
- **Search:** `SearchScreen.kt`
- **Settings:** `SettingsScreen.kt`

---

## 💭 Recent Decisions & Context

### 2026-03-12

#### SWORD Engine Decision
**Decision:** Implement pure-Kotlin SWORD engine (modeled after pocketsword KotlinSword module)
**Rationale:** Enables offline Bible reading from `.zip`/`.sword` module files without API dependency
**Key module types to support:**
- `zText` (compressed Bible text — most modules)
- `RawCom` (commentaries)
- `RawLD4` / `zLD` (lexicon/dictionary)
- `RawGenBook` (general books)
**Versification systems:** KJV (primary), NRSV, NRSVA, Catholic, Catholic2, LXX, Eastern, Synodal

#### Feature Gap Analysis vs PocketSword
**Missing from KMP vs PocketSword inspiration:**
1. Pure-Kotlin SWORD engine (binary file readers)
2. SwordVersification.kt (8 versification systems)
3. Module browser/catalog screen
4. Module download manager
5. Pins screen (PinsRepository + PinsViewModel + PinsScreen)
6. Bookmark folders (hierarchical with color pickers)
7. Commentary screen and panel
8. Dictionary screen
9. Strong's word study panel
10. Onboarding flow (3-step wizard)
11. Audio player (expect/actual per platform)
12. Verse image generation / sharing
13. History screen with navigation
14. Profile screen
15. Reading statistics / streaks
16. Data export (DOCX/PDF)
17. Tag/collection system
18. Advanced search (phrase, Strong's, morphology filters)
19. Study pad (rich text note editor)
20. Parallel translation view

---

## 🧩 Patterns & Architecture

### ARI Encoding
```kotlin
val ari = (bookId shl 16) or (chapter shl 8) or verse
```

### SQLDelight Pattern
- Schema defined in `.sq` files in `shared/src/commonMain/sqldelight/`
- Queries generated as type-safe functions
- `DatabaseDriverFactory` expect/actual for Android/iOS

### ViewModel Pattern
```kotlin
class SomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SomeUiState())
    val uiState: StateFlow<SomeUiState> = _uiState.asStateFlow()
}
```

### Navigation
- `NavController` shared via `LocalNavController.current`
- Route constants in `Screen.kt` sealed class

### SWORD Module System (PocketSword model)
```
SwordEngine.kt          ← Entry point
modules/
  SwordModule.kt        ← Base module class
  BibleModule.kt        ← zText/RawText reader
  CommentaryModule.kt   ← RawCom reader
  DictionaryModule.kt   ← RawLD4/zLD reader
drivers/
  ZTextDriver.kt        ← Compressed text decoder (LZW/zlib)
  RawTextDriver.kt      ← Simple raw text reader
  RawLD4Driver.kt       ← Lexicon/dictionary reader
versification/
  SwordVersification.kt ← Verse count tables
  VersificationSystem.kt← Enum for all systems
```

---

## 🔧 Things to Remember

- **GitHub Repo:** https://github.com/maxymurm/androidbible-kmp
- **Project Board:** #6 — Android Bible - Compose Multiplatform (https://github.com/users/maxymurm/projects/6)
- **Build:** `./gradlew build` (cross-platform)
- **Android Run:** `./gradlew :androidApp:installDebug`
- **Tests:** `./gradlew :shared:allTests`
- **CI:** `.github/workflows/ci.yml` (pre-existing)
- **KMP Compose Version:** Check `gradle/libs.versions.toml`

---

## 📊 Project Statistics

- **Total Issues Created:** See GitHub
- **Phases Completed:** 12 of 12 original + new enhancement phases
- **Screens:** 15+ Compose screens
- **Shared modules:** shared, composeApp
- **Platform targets:** Android + iOS
