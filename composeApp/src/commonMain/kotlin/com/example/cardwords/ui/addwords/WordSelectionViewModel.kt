package com.example.cardwords.ui.addwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.remote.FetchedWordResult
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ApiSearchResult {
    data class Found(val word: FetchedWordResult) : ApiSearchResult
    data object NotFound : ApiSearchResult
}

data class WordSelectionUiState(
    val words: List<Word> = emptyList(),
    val selectedWordIds: Set<Long> = emptySet(),
    val alreadyAddedIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val filteredWords: List<Word> = emptyList(),
    val isApiSearching: Boolean = false,
    val apiSearchResult: ApiSearchResult? = null,
    val apiError: String? = null,
    val customOriginal: String = "",
    val customTranslation: String = "",
    val customTranscription: String = "",
    val showCustomForm: Boolean = false,
    val customWordAdded: Boolean = false,
) {
    val hasSelection: Boolean get() = selectedWordIds.isNotEmpty()
    val selectedCount: Int get() = selectedWordIds.size
    val canAddCustomWord: Boolean get() = customOriginal.isNotBlank() && customTranslation.isNotBlank()
}

class WordSelectionViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository
    private val apiClient = AppModule.wordApiClient

    private val _uiState = MutableStateFlow(WordSelectionUiState())
    val uiState: StateFlow<WordSelectionUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    private fun loadWords() {
        val allWords = repository.getAllWords()
        val alreadyAdded = allWords.filter { it.isInDictionary }.map { it.id }.toSet()
        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) allWords else filterWords(allWords, query)
        _uiState.update {
            it.copy(words = allWords, alreadyAddedIds = alreadyAdded, filteredWords = filtered)
        }
    }

    fun onSearchQueryChange(query: String) {
        val allWords = _uiState.value.words
        val filtered = if (query.isBlank()) allWords else filterWords(allWords, query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredWords = filtered,
                apiSearchResult = null,
                apiError = null,
            )
        }
    }

    fun searchApi() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isApiSearching = true, apiSearchResult = null, apiError = null) }

        viewModelScope.launch {
            val result = apiClient.fetchWordWithTranslation(query)
            result.fold(
                onSuccess = { fetched ->
                    _uiState.update {
                        it.copy(
                            isApiSearching = false,
                            apiSearchResult = ApiSearchResult.Found(fetched),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isApiSearching = false,
                            apiSearchResult = ApiSearchResult.NotFound,
                            apiError = error.message,
                        )
                    }
                },
            )
        }
    }

    fun addApiWord(fetched: FetchedWordResult) {
        val existing = repository.findByOriginal(fetched.original)
        val wordId = if (existing != null) {
            existing.id
        } else {
            repository.insertWord(
                Word(
                    id = 0,
                    original = fetched.original,
                    translation = fetched.translation,
                    transcription = fetched.transcription,
                    source = "api",
                )
            )
        }
        repository.addToDictionary(wordId)
        _uiState.update { it.copy(apiSearchResult = null, searchQuery = "") }
        loadWords()
    }

    fun toggleWordSelection(wordId: Long) {
        _uiState.update { state ->
            val newSelection = if (wordId in state.selectedWordIds) {
                state.selectedWordIds - wordId
            } else {
                state.selectedWordIds + wordId
            }
            state.copy(selectedWordIds = newSelection)
        }
    }

    fun addSelectedWords() {
        val selected = _uiState.value.selectedWordIds
        if (selected.isNotEmpty()) {
            selected.forEach { id ->
                repository.addToDictionary(id)
            }
        }
    }

    fun toggleCustomForm() {
        _uiState.update { it.copy(showCustomForm = !it.showCustomForm, customWordAdded = false) }
    }

    fun onCustomOriginalChange(value: String) {
        _uiState.update { it.copy(customOriginal = value, customWordAdded = false) }
    }

    fun onCustomTranslationChange(value: String) {
        _uiState.update { it.copy(customTranslation = value, customWordAdded = false) }
    }

    fun onCustomTranscriptionChange(value: String) {
        _uiState.update { it.copy(customTranscription = value) }
    }

    fun addCustomWord() {
        val state = _uiState.value
        if (!state.canAddCustomWord) return

        val word = Word(
            id = 0,
            original = state.customOriginal.trim(),
            translation = state.customTranslation.trim(),
            transcription = state.customTranscription.trim(),
            source = "custom",
        )
        val existing = repository.findByOriginal(word.original)
        val wordId = if (existing != null) {
            existing.id
        } else {
            repository.insertWord(word)
        }
        repository.addToDictionary(wordId)
        _uiState.update {
            it.copy(
                customOriginal = "",
                customTranslation = "",
                customTranscription = "",
                customWordAdded = true,
            )
        }
        loadWords()
    }

    private fun filterWords(words: List<Word>, query: String): List<Word> {
        val q = query.lowercase()
        return words.filter {
            it.original.lowercase().contains(q) || it.translation.lowercase().contains(q)
        }
    }
}
