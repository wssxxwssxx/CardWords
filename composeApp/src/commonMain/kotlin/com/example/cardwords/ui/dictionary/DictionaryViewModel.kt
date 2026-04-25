package com.example.cardwords.ui.dictionary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.study.StudyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class DictionaryUiState(
    val words: List<Word> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = true,
    val wordProgressMap: Map<Long, Map<StudyMode, WordProgress>> = emptyMap(),
    val now: Long = 0L,
)

class DictionaryViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    fun refresh(onComplete: () -> Unit = {}) {
        val token = authManager.getToken()
        if (token == null) {
            loadWords(onComplete)
            return
        }
        viewModelScope.launch {
            val result = apiClient.getCards(token)
            result.onSuccess { cards ->
                withContext(Dispatchers.Default) {
                    // Server is the source of truth for the dictionary.
                    // Any local dictionary word NOT on the server gets removed —
                    // regardless of its local `source` tag (pack_*, server, api, etc).
                    val serverOriginals = cards.map { it.wordOriginal.lowercase().trim() }.toSet()

                    repository.getDictionaryWords().forEach { local ->
                        val key = local.original.lowercase().trim()
                        if (key !in serverOriginals) {
                            repository.removeFromDictionary(local.id)
                        }
                    }

                    // Insert missing server words / add existing ones back to dictionary.
                    val toInsert = mutableListOf<Word>()
                    cards.forEach { card ->
                        val existing = repository.findByOriginal(card.wordOriginal)
                        if (existing == null) {
                            toInsert.add(
                                Word(
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
                        } else if (!existing.isInDictionary) {
                            repository.addToDictionary(existing.id)
                        }
                    }
                    if (toInsert.isNotEmpty()) {
                        repository.insertWordsInTransaction(toInsert)
                    }
                }
            }
            loadWords(onComplete)
        }
    }

    fun loadWords(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.Default) {
                val words = repository.getDictionaryWords()
                val allProgress = repository.getAllWordProgress()
                val progressMap = allProgress.groupBy { it.wordId }
                    .mapValues { (_, list) -> list.associateBy { it.mode } }
                val now = repository.currentTimeMillis()
                Triple(words, progressMap, now)
            }
            _uiState.update {
                it.copy(
                    words = snapshot.first,
                    isEmpty = snapshot.first.isEmpty(),
                    wordProgressMap = snapshot.second,
                    now = snapshot.third,
                )
            }
            onComplete()
        }
    }

    fun removeWord(wordId: Long) {
        // Optimistic UI update — remove immediately from state
        val currentState = _uiState.value
        val word = currentState.words.firstOrNull { it.id == wordId }
        _uiState.update { state ->
            state.copy(
                words = state.words.filter { it.id != wordId },
                isEmpty = state.words.size <= 1,
                wordProgressMap = state.wordProgressMap - wordId,
            )
        }

        // Local DB removal — tied to viewModelScope is fine, runs immediately
        viewModelScope.launch(Dispatchers.Default) {
            repository.removeFromDictionary(wordId)
        }

        // Server delete — run on APP-level scope so it survives screen navigation.
        // Without this, closing the screen before the HTTP round-trip finishes
        // would cancel the delete and leave the card on the server.
        val token = authManager.getToken()
        if (token != null && word != null) {
            AppModule.syncScope.launch {
                deleteWordFromServer(token, word.original)
            }
        }
    }

    /** Find card on the server by original word (case-insensitive) and delete it. */
    private suspend fun deleteWordFromServer(token: String, original: String) {
        // 1. Try the dedicated check endpoint first
        apiClient.checkWord(token, original).onSuccess { response ->
            if (response.exists && response.cardId != null) {
                apiClient.deleteCard(token, response.cardId)
                return
            }
        }
        // 2. Fallback: pull the full list and match case-insensitively
        apiClient.getCards(token).onSuccess { cards ->
            val target = original.lowercase().trim()
            val match = cards.firstOrNull { it.wordOriginal.lowercase().trim() == target }
            if (match != null) {
                apiClient.deleteCard(token, match.id)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        // Only update the query — filtering happens lazily in the UI (memoized)
        _uiState.update { it.copy(searchQuery = query) }
    }
}
