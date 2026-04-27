package com.example.cardwords.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.AssignedCollection
import com.example.cardwords.data.model.AssignmentStatus
import com.example.cardwords.data.model.Collection
import com.example.cardwords.data.model.TeacherSummary
import com.example.cardwords.di.AppModule
import com.example.cardwords.data.remote.AssignedCollectionDto
import com.example.cardwords.data.remote.TeacherSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MyTeachersPhase { LOADING, CONTENT, ERROR }

data class MyTeachersUiState(
    val phase: MyTeachersPhase = MyTeachersPhase.LOADING,
    val teachers: List<TeacherSummary> = emptyList(),
    val collections: List<AssignedCollection> = emptyList(),
    val error: String? = null,
    val acceptingIds: Set<String> = emptySet(),
)

class MyTeachersViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager
    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(MyTeachersUiState())
    val uiState: StateFlow<MyTeachersUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val token = authManager.getToken() ?: return
        _uiState.update { it.copy(phase = MyTeachersPhase.LOADING, error = null) }
        viewModelScope.launch {
            val (teachersR, collectionsR) = withContext(Dispatchers.Default) {
                apiClient.listMyTeachers(token) to apiClient.listAssignedCollections(token)
            }
            val teachers = teachersR.getOrNull()?.map { it.toDomain() } ?: emptyList()
            val rawCollections = collectionsR.getOrNull() ?: emptyList()

            val mapped = rawCollections.map { dto ->
                val storedKey = "accepted_coll_${dto.id}"
                val lastKnown = repository.getSetting(storedKey)?.toIntOrNull() ?: 0
                val status = AssignmentStatus.fromWire(dto.status)
                AssignedCollection(
                    collection = Collection(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        cardsCount = dto.cardsCount,
                    ),
                    teacherName = dto.teacherName,
                    status = status,
                    hasNewCards = status == AssignmentStatus.ADDED && dto.cardsCount > lastKnown,
                )
            }
            val isError = teachersR.isFailure || collectionsR.isFailure
            _uiState.update {
                it.copy(
                    phase = if (isError) MyTeachersPhase.ERROR else MyTeachersPhase.CONTENT,
                    teachers = teachers,
                    collections = mapped,
                    error = if (isError) "Не удалось загрузить" else null,
                )
            }
        }
    }

    fun acceptCollection(collectionId: String) {
        if (collectionId in _uiState.value.acceptingIds) return
        val token = authManager.getToken() ?: return
        val previous = _uiState.value
        // optimistic: set status to ADDED + clear hasNewCards
        _uiState.update { state ->
            state.copy(
                acceptingIds = state.acceptingIds + collectionId,
                collections = state.collections.map { ac ->
                    if (ac.collection.id == collectionId) {
                        ac.copy(status = AssignmentStatus.ADDED, hasNewCards = false)
                    } else ac
                },
            )
        }
        AppModule.syncScope.launch {
            val result = apiClient.acceptCollection(token, collectionId)
            result.onSuccess {
                // Persist new "last known" cards count from CURRENT state (may have been updated by a concurrent refresh)
                val target = _uiState.value.collections.firstOrNull { it.collection.id == collectionId }
                if (target != null) {
                    repository.setSetting("accepted_coll_$collectionId", target.collection.cardsCount.toString())
                }
                // Pull new cards into the personal dictionary
                AppModule.syncScope.launch { syncDictionary() }
            }
            result.onFailure {
                // rollback
                _uiState.update { state ->
                    state.copy(
                        acceptingIds = state.acceptingIds - collectionId,
                        collections = previous.collections,
                        error = "Не удалось принять коллекцию",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(acceptingIds = it.acceptingIds - collectionId) }
        }
    }

    private suspend fun syncDictionary() {
        val token = authManager.getToken() ?: return
        val cardsResult = apiClient.getCards(token)
        cardsResult.onSuccess { cards ->
            withContext(Dispatchers.Default) {
                val existingByOriginal = repository.getDictionaryWords()
                    .associateBy { it.original.lowercase() }
                val toInsert = mutableListOf<com.example.cardwords.data.model.Word>()
                cards.forEach { card ->
                    val key = card.wordOriginal.lowercase()
                    if (existingByOriginal[key] == null) {
                        toInsert.add(
                            com.example.cardwords.data.model.Word(
                                id = 0L,
                                original = card.wordOriginal,
                                translation = card.wordTranslation,
                                transcription = "",
                                category = "",
                                isInDictionary = true,
                                addedAt = repository.currentTimeMillis(),
                                source = "server",
                            )
                        )
                    } else {
                        val w = existingByOriginal[key]!!
                        if (!w.isInDictionary) repository.addToDictionary(w.id)
                    }
                }
                if (toInsert.isNotEmpty()) repository.insertWordsInTransaction(toInsert)
            }
        }
    }

    private fun TeacherSummaryDto.toDomain() = TeacherSummary(id = id, email = email, name = name)
}
