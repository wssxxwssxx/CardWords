package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.util.DateUtil

object StreakManager {

    private const val SETTING_STREAK_FREEZES = "streak_freezes"
    private const val SETTING_FREEZE_USED_PREFIX = "freeze_used_"
    private const val FREEZE_MILESTONE_INTERVAL = 7

    fun getAvailableFreezes(repository: DatabaseRepository): Int {
        return repository.getSettingOrDefault(SETTING_STREAK_FREEZES, "0").toIntOrNull() ?: 0
    }

    fun setAvailableFreezes(repository: DatabaseRepository, count: Int) {
        repository.setSetting(SETTING_STREAK_FREEZES, count.coerceAtLeast(0).toString())
    }

    fun isFreezeUsedOnDate(repository: DatabaseRepository, dateStr: String): Boolean {
        return repository.getSetting(SETTING_FREEZE_USED_PREFIX + dateStr) != null
    }

    fun getFreezeUsedDates(repository: DatabaseRepository, now: Long, lookbackDays: Int = 60): Set<String> {
        val result = mutableSetOf<String>()
        for (i in 0..lookbackDays) {
            val dateStr = DateUtil.daysAgoFromMillis(now, i)
            if (isFreezeUsedOnDate(repository, dateStr)) {
                result.add(dateStr)
            }
        }
        return result
    }

    fun computeStreakWithFreeze(
        activeDates: Set<String>,
        freezeUsedDates: Set<String>,
        now: Long,
    ): Int {
        if (activeDates.isEmpty() && freezeUsedDates.isEmpty()) return 0

        val todayStr = DateUtil.epochMillisToDateString(now)
        val todayActive = todayStr in activeDates || todayStr in freezeUsedDates
        var streak = 0
        val startDay = if (todayActive) 0 else 1

        for (i in startDay..365) {
            val dateStr = DateUtil.daysAgoFromMillis(now, i)
            when {
                dateStr in activeDates -> streak++
                dateStr in freezeUsedDates -> streak++
                else -> break
            }
        }
        return streak
    }

    fun shouldUseFreeze(
        activeDates: Set<String>,
        now: Long,
    ): Boolean {
        val yesterdayStr = DateUtil.daysAgoFromMillis(now, 1)
        val dayBeforeStr = DateUtil.daysAgoFromMillis(now, 2)
        val yesterdayMissed = yesterdayStr !in activeDates
        val hadStreakBefore = dayBeforeStr in activeDates
        return yesterdayMissed && hadStreakBefore
    }

    fun applyFreezeIfNeeded(repository: DatabaseRepository, now: Long): Boolean {
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()
        val yesterdayStr = DateUtil.daysAgoFromMillis(now, 1)

        if (isFreezeUsedOnDate(repository, yesterdayStr)) return false
        if (!shouldUseFreeze(activeDates, now)) return false

        val available = getAvailableFreezes(repository)
        if (available <= 0) return false

        repository.setSetting(SETTING_FREEZE_USED_PREFIX + yesterdayStr, "1")
        setAvailableFreezes(repository, available - 1)
        return true
    }

    fun awardFreezeForMilestone(repository: DatabaseRepository, streak: Int): Boolean {
        if (streak <= 0 || streak % FREEZE_MILESTONE_INTERVAL != 0) return false

        val milestoneKey = "freeze_milestone_$streak"
        if (repository.getSetting(milestoneKey) != null) return false

        repository.setSetting(milestoneKey, "1")
        val current = getAvailableFreezes(repository)
        setAvailableFreezes(repository, current + 1)
        return true
    }
}
