package org.androidbible.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.androidbible.ui.screens.bible.BibleScreen
import org.androidbible.ui.screens.markers.BookmarksScreen
import org.androidbible.ui.screens.search.SearchScreen
import org.androidbible.ui.screens.settings.SettingsScreen

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Android Bible") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("📖") },
                        label = { Text("Bible") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Text("🔖") },
                        label = { Text("Bookmarks") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Text("🔍") },
                        label = { Text("Search") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙") },
                        label = { Text("Settings") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> BibleTabContent(
                        onNavigateToBible = { navigator.push(BibleScreen()) }
                    )
                    1 -> BookmarksTabContent(
                        onNavigateToBookmarks = { navigator.push(BookmarksScreen()) }
                    )
                    2 -> SearchTabContent(
                        onNavigateToSearch = { navigator.push(SearchScreen()) }
                    )
                    3 -> SettingsTabContent(
                        onNavigateToSettings = { navigator.push(SettingsScreen()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BibleTabContent(onNavigateToBible: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Welcome to Android Bible",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Select a Bible version to start reading",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToBible) {
            Text("Open Bible Reader")
        }
    }
}

@Composable
private fun BookmarksTabContent(onNavigateToBookmarks: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Bookmarks & Notes",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToBookmarks) {
            Text("View Bookmarks")
        }
    }
}

@Composable
private fun SearchTabContent(onNavigateToSearch: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Search the Bible",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToSearch) {
            Text("Start Searching")
        }
    }
}

@Composable
private fun SettingsTabContent(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToSettings) {
            Text("Open Settings")
        }
    }
}
