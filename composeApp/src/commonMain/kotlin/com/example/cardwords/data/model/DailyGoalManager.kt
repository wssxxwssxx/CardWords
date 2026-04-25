package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.util.DateUtil

data class DailyGoalProgress(
    val goal: Int,
    val wordsStudied: Int,
    val progressFraction: Float,
    val isCompleted: Boolean,
)

data class WeeklySummary(
    val thisWeekWords: Int,
    val lastWeekWords: Int,
    val changePercent: Int?,
)

object DailyGoalManager {
    const val DEFAULT_DAILY_GOAL = 10
    private const val SETTING_DAILY_GOAL = "daily_goal"

    fun getDailyGoal(repository: DatabaseRepository): Int =
        repository.getSettingOrDefault(SETTING_DAILY_GOAL, DEFAULT_DAILY_GOAL.toString())
            .toIntOrNull() ?: DEFAULT_DAILY_GOAL

    fun setDailyGoal(repository: DatabaseRepository, goal: Int) {
        repository.setSetting(SETTING_DAILY_GOAL, goal.coerceIn(1, 100).toString())
    }

    fun getTodayProgress(repository: DatabaseRepository): DailyGoalProgress {
        val goal = getDailyGoal(repository)
        val now = repository.currentTimeMillis()
        val todayStr = DateUtil.epochMillisToDateString(now)
        val todayActivity = repository.getDailyActivityRange(todayStr, todayStr).firstOrNull()
        val wordsStudied = todayActivity?.wordsStudied ?: 0
        val fraction = if (goal > 0) (wordsStudied.toFloat() / goal).coerceAtMost(1f) else 1f

        return DailyGoalProgress(
            goal = goal,
            wordsStudied = wordsStudied,
            progressFraction = fraction,
            isCompleted = wordsStudied >= goal,
        )
    }

    fun computeWeeklySummary(repository: DatabaseRepository): WeeklySummary {
        val now = repository.currentTimeMillis()
        val twoWeeksAgoStr = DateUtil.daysAgoFromMillis(now, 13)
        val todayStr = DateUtil.epochMillisToDateString(now)
        val activityByDate = repository.getDailyActivityRange(twoWeeksAgoStr, todayStr)
            .associateBy { it.date }

        val thisWeekWords = (0..6).sumOf { daysAgo ->
            activityByDate[DateUtil.daysAgoFromMillis(now, daysAgo)]?.wordsStudied ?: 0
        }
        val lastWeekWords = (7..13).sumOf { daysAgo ->
            activityByDate[DateUtil.daysAgoFromMillis(now, daysAgo)]?.wordsStudied ?: 0
        }
        val changePercent = if (lastWeekWords > 0) {
            ((thisWeekWords - lastWeekWords) * 100) / lastWeekWords
        } else null

        return WeeklySummary(thisWeekWords, lastWeekWords, changePercent)
    }

    fun countConsecutiveGoalDays(repository: DatabaseRepository): Int {
        val goal = getDailyGoal(repository)
        val now = repository.currentTimeMillis()
        val lookbackStr = DateUtil.daysAgoFromMillis(now, 365)
        val todayStr = DateUtil.epochMillisToDateString(now)
        val activityByDate = repository.getDailyActivityRange(lookbackStr, todayStr)
            .associateBy { it.date }

        val todayMet = (activityByDate[todayStr]?.wordsStudied ?: 0) >= goal
        val startDay = if (todayMet) 0 else 1
        var count = 0

        for (i in startDay..365) {
            val dateStr = DateUtil.daysAgoFromMillis(now, i)
            val activity = activityByDate[dateStr]
            if (activity != null && activity.wordsStudied >= goal) count++ else break
        }
        return count
    }
}
