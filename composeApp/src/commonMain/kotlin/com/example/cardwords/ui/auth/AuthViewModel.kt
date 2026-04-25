package com.example.cardwords.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.Word
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthPhase { IDLE, LOADING, SUCCESS, ERROR }
enum class AuthTab { LOGIN, REGISTER }

data class AuthUiState(
    val phase: AuthPhase = AuthPhase.IDLE,
    val tab: AuthTab = AuthTab.LOGIN,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val error: String? = null,
    val serverAvailable: Boolean? = null, // null = checking
)

class AuthViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager
    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkServer()
    }

    private fun checkServer() {
        viewModelScope.launch {
            val result = apiClient.healthCheck()
            _uiState.update {
                it.copy(serverAvailable = result.isSuccess && result.getOrNull()?.status == "ok")
            }
        }
    }

    fun switchTab(tab: AuthTab) {
        _uiState.update { it.copy(tab = tab, error = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    private suspend fun syncCardsFromServer(token: String) {
        val cardsResult = apiClient.getCards(token)
        cardsResult.onSuccess { cards ->
            val words = cards.map { card ->
                Word(
                    id = 0L,
                    original = card.wordOriginal,
                    translation = card.wordTranslation,
                    transcription = "",
                    category = "",
                    isInDictionary = true,
                    addedAt = System.currentTimeMillis(),
                    source = "server",
                )
            }
            if (words.isNotEmpty()) {
                repository.insertWordsInTransaction(words)
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        _uiState.update { it.copy(phase = AuthPhase.LOADING, error = null) }

        viewModelScope.launch {
            val result = apiClient.login(state.email, state.password)
            result.fold(
                onSuccess = { response ->
                    authManager.saveSession(
                        token = response.accessToken,
                        email = response.user.email,
                        name = response.user.name,
                        subscription = response.user.subscriptionStatus,
                    )
                    syncCardsFromServer(response.accessToken)
                    _uiState.update { it.copy(phase = AuthPhase.SUCCESS) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            phase = AuthPhase.ERROR,
                            error = "Неверный email или пароль",
                        )
                    }
                },
            )
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.name.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Пароль минимум 6 символов") }
            return
        }
        _uiState.update { it.copy(phase = AuthPhase.LOADING, error = null) }

        viewModelScope.launch {
            val registerResult = apiClient.register(state.email, state.name, state.password)
            registerResult.fold(
                onSuccess = {
                    // Auto-login after registration
                    val loginResult = apiClient.login(state.email, state.password)
                    loginResult.fold(
                        onSuccess = { response ->
                            authManager.saveSession(
                                token = response.accessToken,
                                email = response.user.email,
                                name = response.user.name,
                                subscription = response.user.subscriptionStatus,
                            )
                            syncCardsFromServer(response.accessToken)
                            _uiState.update { it.copy(phase = AuthPhase.SUCCESS) }
                        },
                        onFailure = {
                            _uiState.update {
                                it.copy(phase = AuthPhase.ERROR, error = "Регистрация успешна, но не удалось войти")
                            }
                        },
                    )
                },
                onFailure = { e ->
                    val msg = if (e.message?.contains("409") == true) {
                        "Email уже занят"
                    } else {
                        "Ошибка регистрации"
                    }
                    _uiState.update { it.copy(phase = AuthPhase.ERROR, error = msg) }
                },
            )
        }
    }
}
