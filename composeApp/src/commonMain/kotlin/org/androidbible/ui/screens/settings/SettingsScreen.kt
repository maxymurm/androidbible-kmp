package org.androidbible.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.androidbible.domain.repository.AuthRepository
import org.androidbible.ui.screens.auth.LoginScreen
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SettingsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Account section
                item {
                    Text(
                        "Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                item {
                    if (state.isLoggedIn) {
                        ListItem(
                            headlineContent = { Text(state.userName) },
                            supportingContent = { Text(state.userEmail) },
                            trailingContent = {
                                TextButton(onClick = { screenModel.logout() }) {
                                    Text("Logout")
                                }
                            },
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Sign In") },
                            supportingContent = { Text("Sync your data across devices") },
                            modifier = Modifier.clickable {
                                navigator.push(LoginScreen())
                            },
                        )
                    }
                }

                item { HorizontalDivider() }

                // Display section
                item {
                    Text(
                        "Display",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Font Size") },
                        supportingContent = { Text("${state.fontSize}sp") },
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Dark Mode") },
                        trailingContent = {
                            Switch(
                                checked = state.isDarkMode,
                                onCheckedChange = { screenModel.setDarkMode(it) },
                            )
                        },
                    )
                }

                item { HorizontalDivider() }

                // Sync section
                item {
                    Text(
                        "Sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Sync Now") },
                        supportingContent = { Text("${state.pendingSyncItems} pending items") },
                        modifier = Modifier.clickable { screenModel.syncNow() },
                    )
                }

                item { HorizontalDivider() }

                // About section
                item {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Version") },
                        supportingContent = { Text("1.0.0") },
                    )
                }
            }
        }
    }
}

data class SettingsState(
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val fontSize: Int = 16,
    val isDarkMode: Boolean = false,
    val pendingSyncItems: Int = 0,
)

class SettingsScreenModel : ScreenModel, KoinComponent {

    private val authRepo: AuthRepository by inject()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            authRepo.isLoggedIn().collect { isLoggedIn ->
                _state.value = _state.value.copy(isLoggedIn = isLoggedIn)
            }
        }
        screenModelScope.launch {
            authRepo.getCurrentUser().collect { user ->
                if (user != null) {
                    _state.value = _state.value.copy(
                        userName = user.name,
                        userEmail = user.email,
                    )
                }
            }
        }
    }

    fun logout() {
        screenModelScope.launch {
            authRepo.logout()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _state.value = _state.value.copy(isDarkMode = enabled)
    }

    fun syncNow() {
        // Trigger sync via SyncManager
    }
}
