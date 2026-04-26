package com.example.cardwords.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RolePickPhase { IDLE, SUBMITTING, ERROR }

data class RoleSelectionUiState(
    val phase: RolePickPhase = RolePickPhase.IDLE,
    val error: String? = null,
)

class RoleSelectionViewModel : ViewModel() {
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    fun pickRole(role: UserRole, onSuccess: () -> Unit) {
        if (_uiState.value.phase == RolePickPhase.SUBMITTING) return
        _uiState.value = RoleSelectionUiState(phase = RolePickPhase.SUBMITTING)
        viewModelScope.launch {
            val result = authManager.setRole(role)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { e ->
                    _uiState.value = RoleSelectionUiState(
                        phase = RolePickPhase.ERROR,
                        error = mapError(e.message),
                    )
                },
            )
        }
    }

    private fun mapError(raw: String?): String = when {
        raw == null -> "Не удалось установить роль"
        raw.contains("401") -> "Сессия истекла, войдите заново"
        raw.contains("ConnectException", ignoreCase = true) ||
            raw.contains("UnresolvedAddress", ignoreCase = true) -> "Нет подключения к интернету"
        raw.contains("timeout", ignoreCase = true) -> "Превышено время ожидания"
        else -> "Не удалось установить роль"
    }
}
