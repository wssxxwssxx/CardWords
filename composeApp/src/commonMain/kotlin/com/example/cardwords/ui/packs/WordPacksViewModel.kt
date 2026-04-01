package com.example.cardwords.ui.packs

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.PackLevel
import com.example.cardwords.data.model.WordPack
import com.example.cardwords.data.packs.WordPackRegistry
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PackOverview(
    val pack: WordPack,
    val installedCount: Long,
    val inDictionaryCount: Long,
) {
    val isInstalled: Boolean get() = installedCount > 0
    val progress: Float
        get() = if (pack.wordCount > 0) inDictionaryCount.toFloat() / pack.wordCount else 0f
}

data class WordPacksUiState(
    val byLevel: List<PackOverview> = emptyList(),
    val byTopic: List<PackOverview> = emptyList(),
    val special: List<PackOverview> = emptyList(),
    val isLoading: Boolean = true,
)

class WordPacksViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(WordPacksUiState())
    val uiState: StateFlow<WordPacksUiState> = _uiState.asStateFlow()

    init {
        loadPacks()
    }

    fun refresh() {
        loadPacks()
    }

    private fun loadPacks() {
        val overviews = WordPackRegistry.allPacks.map { pack ->
            PackOverview(
                pack = pack,
                installedCount = repository.getWordCountBySource(pack.sourceTag),
                inDictionaryCount = repository.getDictionaryWordCountBySource(pack.sourceTag),
            )
        }
        _uiState.update {
            it.copy(
                byLevel = overviews.filter { o -> o.pack.level == PackLevel.BY_LEVEL },
                byTopic = overviews.filter { o -> o.pack.level == PackLevel.BY_TOPIC },
                special = overviews.filter { o -> o.pack.level == PackLevel.SPECIAL },
                isLoading = false,
            )
        }
    }
}
