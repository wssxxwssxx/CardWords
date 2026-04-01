package com.example.cardwords.ui.achievements

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AchievementItem(
    val type: AchievementType,
    val isUnlocked: Boolean,
    val unlockedAt: Long = 0L,
)

data class AchievementsUiState(
    val achievements: List<AchievementItem> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = AchievementType.entries.size,
    val isLoaded: Boolean = false,
)

class AchievementsViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        val allUnlocked = repository.getAllAchievements()
        val unlockedMap = allUnlocked.associate { it.type to it.unlockedAt }

        val items = AchievementType.entries.map { type ->
            val unlockedAt = unlockedMap[type]
            AchievementItem(
                type = type,
                isUnlocked = unlockedAt != null,
                unlockedAt = unlockedAt ?: 0L,
            )
        }

        // Unlocked first, then locked
        val sorted = items.sortedByDescending { it.isUnlocked }

        _uiState.value = AchievementsUiState(
            achievements = sorted,
            unlockedCount = unlockedMap.size,
            totalCount = AchievementType.entries.size,
            isLoaded = true,
        )
    }
}
