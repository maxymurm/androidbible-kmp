package org.androidbible.ui.screens.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.androidbible.domain.model.Song
import org.androidbible.domain.model.SongBook
import org.androidbible.domain.repository.SongRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SongsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { SongsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.currentBookTitle ?: "Song Books") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) { CircularProgressIndicator() }
            } else if (state.currentBookId != null) {
                // Show songs in selected book
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.songs) { song ->
                        ListItem(
                            headlineContent = { Text("${song.number}. ${song.title}") },
                            supportingContent = { song.author?.let { Text(it) } },
                            modifier = Modifier.clickable {
                                // TODO: Navigate to song detail
                            },
                        )
                    }
                }
            } else {
                // Show song books
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.songBooks) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                screenModel.selectBook(book)
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                book.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SongsState(
    val songBooks: List<SongBook> = emptyList(),
    val songs: List<Song> = emptyList(),
    val currentBookId: Long? = null,
    val currentBookTitle: String? = null,
    val isLoading: Boolean = false,
)

class SongsScreenModel : ScreenModel, KoinComponent {

    private val songRepo: SongRepository by inject()

    private val _state = MutableStateFlow(SongsState())
    val state: StateFlow<SongsState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(isLoading = true)
        screenModelScope.launch {
            songRepo.getSongBooks().collect { books ->
                _state.value = _state.value.copy(songBooks = books, isLoading = false)
            }
        }
    }

    fun selectBook(book: SongBook) {
        _state.value = _state.value.copy(
            currentBookId = book.id,
            currentBookTitle = book.title,
            isLoading = true,
        )
        screenModelScope.launch {
            songRepo.getSongs(book.id).collect { songs ->
                _state.value = _state.value.copy(songs = songs, isLoading = false)
            }
        }
    }
}
