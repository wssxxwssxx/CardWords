package com.example.cardwords.ui.dictionary

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.study.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DictionaryUiState(
    val words: List<Word> = emptyList(),
    val filteredWords: List<Word> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = true,
    val wordProgressMap: Map<Long, Map<StudyMode, WordProgress>> = emptyMap(),
    val now: Long = 0L,
)

class DictionaryViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    fun loadWords() {
        val words = repository.getDictionaryWords()
        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) words else filterWords(words, query)

        // Load word progress grouped by wordId → (mode → progress)
        val allProgress = repository.getAllWordProgress()
        val progressMap = allProgress.groupBy { it.wordId }
            .mapValues { (_, list) -> list.associateBy { it.mode } }

        _uiState.update {
            it.copy(
                words = words,
                filteredWords = filtered,
                isEmpty = words.isEmpty(),
                wordProgressMap = progressMap,
                now = repository.currentTimeMillis(),
            )
        }
    }

    fun removeWord(wordId: Long) {
        repository.removeFromDictionary(wordId)
        loadWords()
    }

    fun onSearchQueryChange(query: String) {
        val words = _uiState.value.words
        val filtered = if (query.isBlank()) words else filterWords(words, query)
        _uiState.update {
            it.copy(searchQuery = query, filteredWords = filtered)
        }
    }

    private fun filterWords(words: List<Word>, query: String): List<Word> {
        val q = query.lowercase()
        return words.filter {
            it.original.lowercase().contains(q) || it.translation.lowercase().contains(q)
        }
    }
}
