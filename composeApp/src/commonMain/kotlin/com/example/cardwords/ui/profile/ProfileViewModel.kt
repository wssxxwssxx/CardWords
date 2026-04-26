package com.example.cardwords.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StreakManager
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.data.model.XpManager
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val totalWords: Long = 0,
    val wordsMastered: Int = 0,
    val currentStreak: Int = 0,
    val streakFreezes: Int = 0,
    val totalXp: Int = 0,
    val currentLevel: Int = 0,
    val xpProgress: Pair<Int, Int> = Pair(0, 100),
    val totalReviews: Int = 0,
    val isLoaded: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val selectedAvatarId: Int = -1,   // -1 = no avatar chosen (gray placeholder)
    val currentRole: UserRole? = null,
    val roleSwitchInFlight: Boolean = false,
)

class ProfileViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val wordCount = repository.getDictionaryWordCount()
        val allActivity = repository.getAllDailyActivity()
        val minMastery = repository.getMinMasteryPerWord()
        val now = repository.currentTimeMillis()

        val activeDates = allActivity.map { it.date }.toSet()
        val freezeUsedDates = StreakManager.getFreezeUsedDates(repository, now)
        val streak = StreakManager.computeStreakWithFreeze(activeDates, freezeUsedDates, now)
        val freezes = StreakManager.getAvailableFreezes(repository)
        val totalXp = XpManager.getTotalXp(repository)
        val level = XpManager.levelForXp(totalXp)
        val xpProgress = XpManager.xpProgress(totalXp)
        val totalReviews = allActivity.sumOf { it.wordsStudied }
        val masteredCount = minMastery.values.count { it >= MasteryLevels.MASTERED }

        val authManager = AppModule.authManager
        val avatarId = repository.getSetting("avatar_id")?.toIntOrNull() ?: -1
        _uiState.value = ProfileUiState(
            totalWords = wordCount,
            wordsMastered = masteredCount,
            currentStreak = streak,
            streakFreezes = freezes,
            totalXp = totalXp,
            currentLevel = level,
            xpProgress = xpProgress,
            totalReviews = totalReviews,
            isLoaded = true,
            userName = authManager.getUserName(),
            userEmail = authManager.getUserEmail(),
            selectedAvatarId = avatarId,
            currentRole = authManager.getRole(),
            roleSwitchInFlight = _uiState.value.roleSwitchInFlight,
        )
    }

    fun switchRole(newRole: UserRole) {
        if (_uiState.value.roleSwitchInFlight) return
        _uiState.update { it.copy(roleSwitchInFlight = true) }
        viewModelScope.launch {
            AppModule.authManager.setRole(newRole)
            _uiState.update {
                it.copy(
                    roleSwitchInFlight = false,
                    currentRole = AppModule.authManager.getRole(),
                )
            }
        }
    }

    fun setAvatar(id: Int) {
        repository.setSetting("avatar_id", id.toString())
        _uiState.value = _uiState.value.copy(selectedAvatarId = id)
    }

    fun logout() {
        AppModule.authManager.logout()
    }
}
