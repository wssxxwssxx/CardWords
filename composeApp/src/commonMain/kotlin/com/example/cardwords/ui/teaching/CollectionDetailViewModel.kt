package com.example.cardwords.ui.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.CollectionCard
import com.example.cardwords.data.model.CollectionDetail
import com.example.cardwords.data.model.Collection
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailPhase { LOADING, CONTENT, ERROR }

data class CollectionDetailUiState(
    val phase: DetailPhase = DetailPhase.LOADING,
    val detail: CollectionDetail? = null,
    val error: String? = null,
    val savingMeta: Boolean = false,
    val addingCard: Boolean = false,
    val deletingCardIds: Set<String> = emptySet(),
    val assigning: Boolean = false,
)

class CollectionDetailViewModel(private val collectionId: String) : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = DetailPhase.LOADING, error = null) }
        viewModelScope.launch {
            val result = apiClient.getCollection(token, collectionId)
            result.fold(
                onSuccess = { dto ->
                    val detail = CollectionDetail(
                        collection = Collection(dto.id, dto.name, dto.description, dto.cardsCount),
                        cards = dto.cards.map { CollectionCard(it.id, it.wordOriginal, it.wordTranslation) },
                    )
                    _uiState.update { it.copy(phase = DetailPhase.CONTENT, detail = detail) }
                },
                onFailure = {
                    _uiState.update { it.copy(phase = DetailPhase.ERROR, error = "Не удалось загрузить") }
                },
            )
        }
    }

    fun saveMeta(name: String, description: String, onComplete: () -> Unit) {
        if (_uiState.value.savingMeta) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(savingMeta = true) }
        AppModule.syncScope.launch {
            val result = apiClient.updateCollection(token, collectionId, name.trim(), description.trim())
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        val detail = state.detail
                        state.copy(
                            savingMeta = false,
                            detail = detail?.copy(collection = Collection(dto.id, dto.name, dto.description, dto.cardsCount)),
                        )
                    }
                    onComplete()
                },
                onFailure = {
                    _uiState.update { it.copy(savingMeta = false, error = "Не удалось сохранить") }
                },
            )
        }
    }

    fun addCard(original: String, translation: String?) {
        val trimmed = original.trim()
        if (trimmed.isEmpty()) return
        val current = _uiState.value.detail ?: return
        // Local dedupe — case-insensitive compare on original
        val key = trimmed.lowercase()
        val alreadyExists = current.cards.any { it.wordOriginal.lowercase() == key }
        if (alreadyExists || _uiState.value.addingCard) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(addingCard = true) }
        AppModule.syncScope.launch {
            val result = apiClient.addCardToCollection(token, collectionId, trimmed, translation?.trim()?.takeIf { it.isNotEmpty() })
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { state ->
                        val detail = state.detail ?: return@update state
                        state.copy(
                            addingCard = false,
                            detail = detail.copy(
                                cards = detail.cards + CollectionCard(dto.id, dto.wordOriginal, dto.wordTranslation),
                                collection = detail.collection.copy(cardsCount = detail.collection.cardsCount + 1),
                            ),
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(addingCard = false, error = "Не удалось добавить карточку") }
                },
            )
        }
    }

    fun deleteCard(cardId: String) {
        if (cardId in _uiState.value.deletingCardIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value.detail ?: return
        _uiState.update { state ->
            state.copy(
                deletingCardIds = state.deletingCardIds + cardId,
                detail = previous.copy(
                    cards = previous.cards.filter { it.id != cardId },
                    collection = previous.collection.copy(cardsCount = (previous.collection.cardsCount - 1).coerceAtLeast(0)),
                ),
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.removeCardFromCollection(token, collectionId, cardId)
            if (result.isFailure) {
                _uiState.update { it.copy(detail = previous, error = "Не удалось удалить") }
            }
            _uiState.update { it.copy(deletingCardIds = it.deletingCardIds - cardId) }
        }
    }

    fun assignTo(studentId: String, onComplete: () -> Unit) {
        if (_uiState.value.assigning) return
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(assigning = true) }
        AppModule.syncScope.launch {
            val result = apiClient.assignCollection(token, collectionId, studentId)
            _uiState.update { it.copy(assigning = false) }
            if (result.isSuccess) onComplete()
        }
    }
}
