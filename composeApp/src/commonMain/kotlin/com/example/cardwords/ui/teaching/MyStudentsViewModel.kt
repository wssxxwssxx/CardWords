package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.StudentSummary
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudentsPhase { LOADING, CONTENT, ERROR }

private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class MyStudentsUiState(
    val phase: StudentsPhase = StudentsPhase.LOADING,
    val students: List<StudentSummary> = emptyList(),
    val error: String? = null,
    val inviteInFlight: Boolean = false,
    val inviteError: String? = null,
    val removingIds: Set<String> = emptySet(),
)

class MyStudentsViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(MyStudentsUiState())
    val uiState: StateFlow<MyStudentsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = StudentsPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.listMyStudents(token)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(
                        phase = StudentsPhase.CONTENT,
                        students = list.map { dto -> StudentSummary(dto.id, dto.email, dto.name, dto.cardsCount) },
                    ) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = StudentsPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun invite(rawEmail: String, onComplete: () -> Unit) {
        val email = rawEmail.trim()
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(inviteError = "Некорректный email") }
            return
        }
        if (_uiState.value.inviteInFlight) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(inviteInFlight = true, inviteError = null) }
        AppModule.syncScope.launch {
            val result = apiClient.inviteStudent(token, email)
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        state.copy(
                            inviteInFlight = false,
                            inviteError = null,
                            students = state.students + StudentSummary(dto.id, dto.email, dto.name, dto.cardsCount),
                        )
                    }
                    onComplete()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(inviteInFlight = false, inviteError = mapInviteError(e.message))
                    }
                },
            )
        }
    }

    fun removeStudent(id: String) {
        if (id in _uiState.value.removingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.students
        _uiState.update { state ->
            state.copy(
                removingIds = state.removingIds + id,
                students = state.students.filter { it.id != id },
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.removeStudent(token, id)
            if (result.isFailure) {
                // rollback
                _uiState.update { it.copy(students = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(removingIds = it.removingIds - id) }
        }
    }

    private fun mapInviteError(raw: String?): String = when {
        raw == null -> "Не удалось пригласить"
        raw.contains("404") -> "Пользователь с таким email не найден"
        raw.contains("400") -> "Этот пользователь не является учеником"
        raw.contains("409") -> "Уже в списке учеников"
        else -> "Не удалось пригласить"
    }
}
