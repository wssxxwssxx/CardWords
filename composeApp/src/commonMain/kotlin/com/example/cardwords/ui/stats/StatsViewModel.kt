package com.example.cardwords.ui.stats

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.DailyGoalManager
import com.example.cardwords.data.model.MasteryBreakdown
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.WeeklySummary
import com.example.cardwords.di.AppModule
import com.example.cardwords.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatsUiState(
    val currentStreak: Int = 0,
    val totalSessions: Long = 0,
    val overallAccuracy: Float = 0f,
    val heatmapData: List<DailyActivity> = emptyList(),
    val heatmapStartDate: String = "",
    val heatmapEndDate: String = "",
    val recentSessions: List<StudySession> = emptyList(),
    val masteryBreakdown: MasteryBreakdown = MasteryBreakdown(),
    val wordsMastered: Int = 0,
    val totalTrackedWords: Int = 0,
    val weeklySummary: WeeklySummary = WeeklySummary(0, 0, null),
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = AchievementType.entries.size,
    val isLoaded: Boolean = false,
)

class StatsViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        val now = repository.currentTimeMillis()
        val todayStr = DateUtil.epochMillisToDateString(now)

        // Heatmap: last 91 days (13 weeks)
        val startDateStr = DateUtil.daysAgoFromMillis(now, 90)
        val allActivity = repository.getDailyActivityRange(startDateStr, todayStr)

        // Streak calculation
        val currentStreak = computeStreak(now)

        // Session stats
        val totalSessions = repository.getSessionCount()
        val recentSessions = repository.getRecentSessions(20)

        // Overall accuracy from daily activity
        val allDailyActivity = repository.getAllDailyActivity()
        val totalCorrect = allDailyActivity.sumOf { it.correctCount }
        val totalCount = allDailyActivity.sumOf { it.totalCount }
        val overallAccuracy = if (totalCount > 0) totalCorrect.toFloat() / totalCount else 0f

        // Mastery — same algorithm as Home
        val wordCount = repository.getDictionaryWordCount()
        val minMasteryPerWord = repository.getMinMasteryPerWord()
        val masteryBreakdown = MasteryLevels.computeBreakdown(minMasteryPerWord, wordCount.toInt())

        // Weekly summary
        val weeklySummary = DailyGoalManager.computeWeeklySummary(repository)

        // Achievements
        val unlockedAchievementsCount = repository.getAllAchievements().size

        _uiState.value = StatsUiState(
            currentStreak = currentStreak,
            totalSessions = totalSessions,
            overallAccuracy = overallAccuracy,
            heatmapData = allActivity,
            heatmapStartDate = startDateStr,
            heatmapEndDate = todayStr,
            recentSessions = recentSessions,
            masteryBreakdown = masteryBreakdown,
            wordsMastered = masteryBreakdown.masteredCount,
            totalTrackedWords = minMasteryPerWord.size,
            weeklySummary = weeklySummary,
            unlockedAchievementsCount = unlockedAchievementsCount,
            isLoaded = true,
        )
    }

    private fun computeStreak(now: Long): Int {
        val todayStr = DateUtil.epochMillisToDateString(now)
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()

        if (activeDates.isEmpty()) return 0

        // Check if today has activity; if not, start from yesterday
        var streak = 0
        val startDay = if (todayStr in activeDates) 0 else 1

        for (i in startDay..365) {
            val dateStr = DateUtil.daysAgoFromMillis(now, i)
            if (dateStr in activeDates) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    fun refresh() {
        loadStats()
    }
}
