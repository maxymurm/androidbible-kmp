package org.androidbible.ui.screens.bible

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.androidbible.domain.model.BibleVersion
import org.androidbible.domain.model.Book
import org.androidbible.domain.model.Verse
import org.androidbible.domain.repository.BibleRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class BibleState(
    val versions: List<BibleVersion> = emptyList(),
    val books: List<Book> = emptyList(),
    val verses: List<Verse> = emptyList(),
    val currentVersionId: Long = 0,
    val currentBookId: Int = 0,
    val currentBookName: String = "",
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class BibleScreenModel : ScreenModel, KoinComponent {

    private val bibleRepo: BibleRepository by inject()

    private val _state = MutableStateFlow(BibleState())
    val state: StateFlow<BibleState> = _state.asStateFlow()

    init {
        loadVersions()
    }

    private fun loadVersions() {
        screenModelScope.launch {
            bibleRepo.getVersions().collect { versions ->
                _state.value = _state.value.copy(versions = versions)
                if (versions.isNotEmpty() && _state.value.currentVersionId == 0L) {
                    selectVersion(versions.first().id)
                }
            }
        }
    }

    fun selectVersion(versionId: Long) {
        _state.value = _state.value.copy(currentVersionId = versionId, isLoading = true)
        screenModelScope.launch {
            bibleRepo.getBooks(versionId).collect { books ->
                _state.value = _state.value.copy(books = books, isLoading = false)
                if (books.isNotEmpty() && _state.value.currentBookId == 0) {
                    selectBook(books.first().bookId)
                }
            }
        }
    }

    fun selectBook(bookId: Int) {
        val book = _state.value.books.find { it.bookId == bookId }
        _state.value = _state.value.copy(
            currentBookId = bookId,
            currentBookName = book?.shortName ?: "",
            totalChapters = book?.chapterCount ?: 0,
            currentChapter = 1,
        )
        loadChapter(1)
    }

    fun loadChapter(chapter: Int) {
        _state.value = _state.value.copy(currentChapter = chapter, isLoading = true)
        screenModelScope.launch {
            bibleRepo.getChapter(
                _state.value.currentVersionId,
                _state.value.currentBookId,
                chapter,
            ).collect { chapterData ->
                _state.value = _state.value.copy(
                    verses = chapterData.verses,
                    isLoading = false,
                )
            }
        }
    }

    fun nextChapter() {
        val current = _state.value.currentChapter
        if (current < _state.value.totalChapters) {
            loadChapter(current + 1)
        }
    }

    fun previousChapter() {
        val current = _state.value.currentChapter
        if (current > 1) {
            loadChapter(current - 1)
        }
    }
}
