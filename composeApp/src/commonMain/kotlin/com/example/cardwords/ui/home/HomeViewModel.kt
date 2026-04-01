package com.example.cardwords.ui.home

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.DailyGoalManager
import com.example.cardwords.data.model.DailyGoalProgress
import com.example.cardwords.data.model.MasteryBreakdown
import com.example.cardwords.data.model.SmartSessionSelector
import com.example.cardwords.data.model.WeeklySummary
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StreakManager
import com.example.cardwords.data.model.XpManager
import com.example.cardwords.di.AppModule
import com.example.cardwords.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WeekDay(
    val label: String,     // "Пн", "Вт", etc.
    val isActive: Boolean,
    val isToday: Boolean,
)

data class HomeUiState(
    val wordCount: Long = 0,
    val currentStreak: Int = 0,
    val wordsMastered: Int = 0,
    val totalTrackedWords: Int = 0,
    val hasWordsToReview: Boolean = false,
    val reviewWordCount: Int = 0,
    val isLoaded: Boolean = false,
    // New dashboard data
    val weekDays: List<WeekDay> = emptyList(),
    val masteryBreakdown: MasteryBreakdown = MasteryBreakdown(),
    val weeklyWordCounts: List<Int> = emptyList(), // last 7 days word counts for mini chart
    val todayWordsStudied: Int = 0,
    val totalReviews: Int = 0,
    val greetingTime: String = "day", // "morning", "day", "evening", "night"
    // Engagement features
    val dailyGoalProgress: DailyGoalProgress = DailyGoalProgress(10, 0, 0f, false),
    val totalXp: Int = 0,
    val currentLevel: Int = 0,
    val xpProgress: Pair<Int, Int> = Pair(0, 100), // (current progress, needed for next level)
    val streakFreezes: Int = 0,
    // Weekly summary
    val weeklySummary: WeeklySummary = WeeklySummary(0, 0, null),
    val weeklyAccuracy: Int = 0,
    val weeklyTimeMinutes: Int = 0,
    val dungeonHighestFloor: Int = 0,
)

class HomeViewModel : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val wordCount = repository.getDictionaryWordCount()
        val now = repository.currentTimeMillis()

        // Apply streak freeze if needed (auto-protect streak)
        StreakManager.applyFreezeIfNeeded(repository, now)

        // Streak (with freeze support)
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()
        val freezeUsedDates = StreakManager.getFreezeUsedDates(repository, now)
        val currentStreak = StreakManager.computeStreakWithFreeze(activeDates, freezeUsedDates, now)

        // Award streak freeze for milestones
        StreakManager.awardFreezeForMilestone(repository, currentStreak)

        // Mastery overview
        val minMasteryPerWord = repository.getMinMasteryPerWord()
        val totalTrackedWords = minMasteryPerWord.size
        val wordsMastered = minMasteryPerWord.values.count { it >= MasteryLevels.MASTERED }

        // Mastery breakdown
        val masteryBreakdown = computeMasteryBreakdown(minMasteryPerWord, wordCount.toInt())

        // Weekly tracker (7 days)
        val weekDays = computeWeekDays(now, activeDates)

        // Weekly mini-chart (words studied per day, last 7 days)
        val activityByDate = allActivity.associateBy { it.date }
        val weeklyWordCounts = (6 downTo 0).map { daysAgo ->
            val dateStr = DateUtil.daysAgoFromMillis(now, daysAgo)
            activityByDate[dateStr]?.wordsStudied ?: 0
        }

        // Today's stats
        val todayStr = DateUtil.epochMillisToDateString(now)
        val todayActivity = activityByDate[todayStr]
        val todayWordsStudied = todayActivity?.wordsStudied ?: 0
        val totalReviews = allActivity.sumOf { it.wordsStudied }

        // Greeting based on hour (approximate from millis)
        val greetingTime = computeGreetingTime(now)

        // Smart session: compute word selection
        val smartResult = SmartSessionSelector.selectWords(repository)
        val smartIds = smartResult.wordIds

        // Engagement data
        val dailyGoalProgress = DailyGoalManager.getTodayProgress(repository)
        val totalXp = XpManager.getTotalXp(repository)
        val currentLevel = XpManager.levelForXp(totalXp)
        val xpProgress = XpManager.xpProgress(totalXp)
        val streakFreezes = StreakManager.getAvailableFreezes(repository)
        val weeklySummary = DailyGoalManager.computeWeeklySummary(repository)

        // Weekly accuracy
        val weeklyActivities = (0..6).mapNotNull { daysAgo ->
            val dateStr = DateUtil.daysAgoFromMillis(now, daysAgo)
            activityByDate[dateStr]
        }
        val weeklyCorrect = weeklyActivities.sumOf { it.correctCount }
        val weeklyTotal = weeklyActivities.sumOf { it.totalCount }
        val weeklyAccuracy = if (weeklyTotal > 0) weeklyCorrect * 100 / weeklyTotal else 0

        // Weekly time (approximate: 5 seconds per attempt)
        val weeklyTimeMinutes = (weeklyTotal * 5) / 60

        // Dungeon highest floor
        val dungeonHighestFloor = repository.getSetting("dungeon_highest_floor")?.toIntOrNull() ?: 0

        _uiState.value = HomeUiState(
            wordCount = wordCount,
            currentStreak = currentStreak,
            wordsMastered = wordsMastered,
            totalTrackedWords = totalTrackedWords,
            hasWordsToReview = smartIds.isNotEmpty() && wordCount > 0,
            reviewWordCount = smartIds.size,
            isLoaded = true,
            weekDays = weekDays,
            masteryBreakdown = masteryBreakdown,
            weeklyWordCounts = weeklyWordCounts,
            todayWordsStudied = todayWordsStudied,
            totalReviews = totalReviews,
            greetingTime = greetingTime,
            dailyGoalProgress = dailyGoalProgress,
            totalXp = totalXp,
            currentLevel = currentLevel,
            xpProgress = xpProgress,
            streakFreezes = streakFreezes,
            weeklySummary = weeklySummary,
            weeklyAccuracy = weeklyAccuracy,
            weeklyTimeMinutes = weeklyTimeMinutes,
            dungeonHighestFloor = dungeonHighestFloor,
        )
    }

    private fun computeWeekDays(now: Long, activeDates: Set<String>): List<WeekDay> {
        val todayStr = DateUtil.epochMillisToDateString(now)
        val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        // Day-of-week using local timezone
        val instant = Instant.fromEpochMilliseconds(now)
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayDow = localDate.dayOfWeek.ordinal // MONDAY=0..SUNDAY=6
        // Build 7-day list starting from Monday of this week
        return (0..6).map { dow ->
            val daysAgo = todayDow - dow
            val dateStr = DateUtil.daysAgoFromMillis(now, daysAgo)
            WeekDay(
                label = dayLabels[dow],
                isActive = dateStr in activeDates,
                isToday = dateStr == todayStr,
            )
        }
    }

    private fun computeMasteryBreakdown(
        minMasteryPerWord: Map<Long, Int>,
        totalDictionaryWords: Int,
    ): MasteryBreakdown = MasteryLevels.computeBreakdown(
        minMasteryPerWord = minMasteryPerWord,
        totalDictionaryWords = totalDictionaryWords,
    )

    private fun computeGreetingTime(now: Long): String {
        val instant = Instant.fromEpochMilliseconds(now)
        val hourOfDay = instant.toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return when {
            hourOfDay < 6 -> "night"
            hourOfDay < 12 -> "morning"
            hourOfDay < 18 -> "day"
            else -> "evening"
        }
    }
}
