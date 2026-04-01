package com.example.cardwords.ui.packs

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.PackWord
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordPack
import com.example.cardwords.data.packs.WordPackRegistry
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PackWordItem(
    val packWord: PackWord,
    val isAdded: Boolean,
)

data class PackDetailUiState(
    val pack: WordPack? = null,
    val words: List<PackWordItem> = emptyList(),
    val addedCount: Int = 0,
    val isInstalling: Boolean = false,
    val installComplete: Boolean = false,
) {
    val totalCount: Int get() = pack?.wordCount ?: 0
    val allAdded: Boolean get() = addedCount >= totalCount && totalCount > 0
    val progress: Float get() = if (totalCount > 0) addedCount.toFloat() / totalCount else 0f
}

class PackDetailViewModel(packId: String) : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(PackDetailUiState())
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    init {
        val pack = WordPackRegistry.findById(packId)
        if (pack != null) {
            _uiState.value = PackDetailUiState(pack = pack)
            loadWords(pack)
        }
    }

    fun refresh() {
        val pack = _uiState.value.pack ?: return
        loadWords(pack)
    }

    fun installAll() {
        val pack = _uiState.value.pack ?: return
        _uiState.update { it.copy(isInstalling = true) }

        // Insert words that don't exist yet (as not-in-dictionary)
        val existingBySource = repository.getWordsBySource(pack.sourceTag)
            .associateBy { it.original.lowercase() }

        val toInsert = pack.words.filter { pw ->
            val key = pw.original.lowercase()
            key !in existingBySource && repository.findByOriginal(pw.original) == null
        }.map { pw ->
            Word(
                id = 0,
                original = pw.original,
                translation = pw.translation,
                transcription = pw.transcription,
                category = pw.category,
                isInDictionary = false,
                source = pack.sourceTag,
            )
        }

        if (toInsert.isNotEmpty()) {
            repository.insertWordsInTransaction(toInsert)
        }

        // Now add all pack words to dictionary (addToDictionary uses the repository's clock)
        val allPackWords = repository.getWordsBySource(pack.sourceTag)
        allPackWords.filter { !it.isInDictionary }.forEach { word ->
            repository.addToDictionary(word.id)
        }

        // Also handle words that exist from other sources
        pack.words.forEach { pw ->
            val existing = repository.findByOriginal(pw.original)
            if (existing != null && !existing.isInDictionary) {
                repository.addToDictionary(existing.id)
            }
        }

        _uiState.update { it.copy(isInstalling = false, installComplete = true) }
        loadWords(pack)
    }

    fun addSingleWord(packWord: PackWord) {
        val pack = _uiState.value.pack ?: return

        val existing = repository.findByOriginal(packWord.original)
        if (existing != null) {
            if (!existing.isInDictionary) {
                repository.addToDictionary(existing.id)
            }
        } else {
            val word = Word(
                id = 0,
                original = packWord.original,
                translation = packWord.translation,
                transcription = packWord.transcription,
                category = packWord.category,
                isInDictionary = false,
                source = pack.sourceTag,
            )
            repository.insertWordsInTransaction(listOf(word))
            // Find inserted word and add to dictionary
            val inserted = repository.findByOriginal(packWord.original)
            if (inserted != null) {
                repository.addToDictionary(inserted.id)
            }
        }

        loadWords(pack)
    }

    private fun loadWords(pack: WordPack) {
        val allDictionaryOriginals = repository.getDictionaryWords()
            .map { it.original.lowercase() }
            .toSet()

        val items = pack.words.map { pw ->
            PackWordItem(
                packWord = pw,
                isAdded = pw.original.lowercase() in allDictionaryOriginals,
            )
        }

        _uiState.update {
            it.copy(
                words = items,
                addedCount = items.count { item -> item.isAdded },
            )
        }
    }
}
