package org.androidbible.ui.screens.markers

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
import org.androidbible.domain.model.Marker
import org.androidbible.domain.repository.MarkerRepository
import org.androidbible.util.Ari
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BookmarksScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { BookmarksScreenModel() }
        val state by screenModel.state.collectAsState()

        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bookmarks & Notes") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Tabs for Bookmarks, Notes, Highlights
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            screenModel.filterByKind(Marker.KIND_BOOKMARK)
                        },
                        text = { Text("Bookmarks") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            screenModel.filterByKind(Marker.KIND_NOTE)
                        },
                        text = { Text("Notes") },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            screenModel.filterByKind(Marker.KIND_HIGHLIGHT)
                        },
                        text = { Text("Highlights") },
                    )
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.markers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text(
                            "No items yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.markers, key = { it.id }) { marker ->
                            MarkerCard(marker = marker, onDelete = { screenModel.deleteMarker(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkerCard(
    marker: Marker,
    onDelete: (Long) -> Unit = {},
) {
    val (book, chapter, verse) = Ari.decode(marker.ari)
    val kindLabel = when (marker.kind) {
        Marker.KIND_BOOKMARK -> "Bookmark"
        Marker.KIND_NOTE -> "Note"
        Marker.KIND_HIGHLIGHT -> "Highlight"
        else -> "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Book $book, $chapter:$verse",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = kindLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (marker.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = marker.caption,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

data class BookmarksState(
    val markers: List<Marker> = emptyList(),
    val isLoading: Boolean = false,
    val currentKind: Int? = null,
)

class BookmarksScreenModel : ScreenModel, KoinComponent {

    private val markerRepo: MarkerRepository by inject()

    private val _state = MutableStateFlow(BookmarksState())
    val state: StateFlow<BookmarksState> = _state.asStateFlow()

    init {
        loadMarkers(null)
    }

    fun filterByKind(kind: Int) {
        _state.value = _state.value.copy(currentKind = kind)
        loadMarkers(kind)
    }

    private fun loadMarkers(kind: Int?) {
        _state.value = _state.value.copy(isLoading = true)
        screenModelScope.launch {
            markerRepo.getMarkers(kind).collect { markers ->
                _state.value = _state.value.copy(markers = markers, isLoading = false)
            }
        }
    }

    fun deleteMarker(id: Long) {
        screenModelScope.launch {
            markerRepo.deleteMarker(id)
        }
    }
}
