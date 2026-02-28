package org.androidbible.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import org.androidbible.domain.model.LoginRequest
import org.androidbible.domain.model.RegisterRequest
import org.androidbible.domain.repository.AuthRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { LoginScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (state.isRegisterMode) "Create Account" else "Sign In") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isRegisterMode) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { screenModel.updateName(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { screenModel.updateEmail(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { screenModel.updatePassword(it) },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )

                if (state.isRegisterMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.passwordConfirmation,
                        onValueChange = { screenModel.updatePasswordConfirmation(it) },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        screenModel.submit {
                            navigator.pop()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(if (state.isRegisterMode) "Create Account" else "Sign In")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { screenModel.toggleMode() },
                ) {
                    Text(
                        if (state.isRegisterMode) "Already have an account? Sign In"
                        else "Don't have an account? Create one"
                    )
                }
            }
        }
    }
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val passwordConfirmation: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoginScreenModel : ScreenModel, KoinComponent {

    private val authRepo: AuthRepository by inject()

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun updatePasswordConfirmation(confirmation: String) {
        _state.value = _state.value.copy(passwordConfirmation = confirmation, error = null)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(
            isRegisterMode = !_state.value.isRegisterMode,
            error = null,
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        _state.value = s.copy(isLoading = true, error = null)

        screenModelScope.launch {
            try {
                if (s.isRegisterMode) {
                    authRepo.register(
                        RegisterRequest(
                            name = s.name,
                            email = s.email,
                            password = s.password,
                            passwordConfirmation = s.passwordConfirmation,
                        )
                    )
                } else {
                    authRepo.login(
                        LoginRequest(
                            email = s.email,
                            password = s.password,
                        )
                    )
                }
                _state.value = _state.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Authentication failed",
                )
            }
        }
    }
}
