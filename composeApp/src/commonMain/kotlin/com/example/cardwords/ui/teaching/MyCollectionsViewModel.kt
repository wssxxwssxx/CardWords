package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.Collection
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CollectionsPhase { LOADING, CONTENT, ERROR }

data class MyCollectionsUiState(
    val phase: CollectionsPhase = CollectionsPhase.LOADING,
    val collections: List<Collection> = emptyList(),
    val error: String? = null,
    val createInFlight: Boolean = false,
    val createError: String? = null,
    val deletingIds: Set<String> = emptySet(),
)

class MyCollectionsViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(MyCollectionsUiState())
    val uiState: StateFlow<MyCollectionsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = CollectionsPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.listMyCollections(token)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(
                        phase = CollectionsPhase.CONTENT,
                        collections = list.map { dto ->
                            Collection(dto.id, dto.name, dto.description, dto.cardsCount)
                        },
                    ) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = CollectionsPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun create(name: String, description: String, onComplete: () -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(createError = "Имя не может быть пустым") }
            return
        }
        if (_uiState.value.createInFlight) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(createInFlight = true, createError = null) }
        AppModule.syncScope.launch {
            val result = apiClient.createCollection(token, trimmedName, description.trim())
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        state.copy(
                            createInFlight = false,
                            collections = state.collections + Collection(dto.id, dto.name, dto.description, dto.cardsCount),
                        )
                    }
                    onComplete()
                },
                onFailure = {
                    _uiState.update { it.copy(createInFlight = false, createError = "Не удалось создать") }
                },
            )
        }
    }

    fun delete(id: String) {
        if (id in _uiState.value.deletingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.collections
        _uiState.update {
            it.copy(deletingIds = it.deletingIds + id, collections = it.collections.filter { c -> c.id != id })
        }
        AppModule.syncScope.launch {
            val result = apiClient.deleteCollection(token, id)
            if (result.isFailure) {
                _uiState.update { it.copy(collections = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(deletingIds = it.deletingIds - id) }
        }
    }
}
