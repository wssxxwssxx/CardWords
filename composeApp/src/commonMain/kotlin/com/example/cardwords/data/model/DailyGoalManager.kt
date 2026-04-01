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
    val changePercent: Int?,  // null if no last week data
)

object DailyGoalManager {
    const val DEFAULT_DAILY_GOAL = 10
    private const val SETTING_DAILY_GOAL = "daily_goal"

    fun getDailyGoal(repository: DatabaseRepository): Int {
        return repository.getSettingOrDefault(SETTING_DAILY_GOAL, DEFAULT_DAILY_GOAL.toString())
            .toIntOrNull() ?: DEFAULT_DAILY_GOAL
    }

    fun setDailyGoal(repository: DatabaseRepository, goal: Int) {
        repository.setSetting(SETTING_DAILY_GOAL, goal.coerceIn(1, 100).toString())
    }

    fun getTodayProgress(repository: DatabaseRepository): DailyGoalProgress {
        val goal = getDailyGoal(repository)
        val now = repository.currentTimeMillis()
        val todayStr = DateUtil.epochMillisToDateString(now)
        val allActivity = repository.getAllDailyActivity()
        val todayActivity = allActivity.find { it.date == todayStr }
        val wordsStudied = todayActivity?.wordsStudied ?: 0

        val fraction = if (goal > 0) {
            (wordsStudied.toFloat() / goal).coerceAtMost(1f)
        } else 1f

        return DailyGoalProgress(
            goal = goal,
            wordsStudied = wordsStudied,
            progressFraction = fraction,
            isCompleted = wordsStudied >= goal,
        )
    }

    fun computeWeeklySummary(repository: DatabaseRepository): WeeklySummary {
        val now = repository.currentTimeMillis()
        val allActivity = repository.getAllDailyActivity()
        val activityByDate = allActivity.associateBy { it.date }

        val thisWeekWords = (0..6).sumOf { daysAgo ->
            val dateStr = DateUtil.daysAgoFromMillis(now, daysAgo)
            activityByDate[dateStr]?.wordsStudied ?: 0
        }
        val lastWeekWords = (7..13).sumOf { daysAgo ->
            val dateStr = DateUtil.daysAgoFromMillis(now, daysAgo)
            activityByDate[dateStr]?.wordsStudied ?: 0
        }
        val changePercent = if (lastWeekWords > 0) {
            ((thisWeekWords - lastWeekWords) * 100) / lastWeekWords
        } else null

        return WeeklySummary(
            thisWeekWords = thisWeekWords,
            lastWeekWords = lastWeekWords,
            changePercent = changePercent,
        )
    }

    fun countConsecutiveGoalDays(repository: DatabaseRepository): Int {
        val goal = getDailyGoal(repository)
        val now = repository.currentTimeMillis()
        val allActivity = repository.getAllDailyActivity()
        val activityByDate = allActivity.associateBy { it.date }

        val todayStr = DateUtil.epochMillisToDateString(now)
        val todayMet = (activityByDate[todayStr]?.wordsStudied ?: 0) >= goal

        var count = 0
        val startDay = if (todayMet) 0 else 1

        for (i in startDay..365) {
            val dateStr = DateUtil.daysAgoFromMillis(now, i)
            val dayActivity = activityByDate[dateStr]
            if (dayActivity != null && dayActivity.wordsStudied >= goal) {
                count++
            } else {
                break
            }
        }
        return count
    }
}
