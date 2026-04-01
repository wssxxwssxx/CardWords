package com.example.cardwords

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.StreakManager
import com.example.cardwords.util.DateUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakManagerTest {

    // Helper: day in millis (epoch day 0 = 1970-01-01)
    private val DAY_MS = 86_400_000L
    private val BASE_TIME = 1_700_000_000_000L // approx 2023-11-14

    @Test
    fun computeStreakWithFreeze_noActivity() {
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = emptySet(),
            freezeUsedDates = emptySet(),
            now = BASE_TIME,
        )
        assertEquals(0, streak)
    }

    @Test
    fun computeStreakWithFreeze_todayOnly() {
        val todayStr = DateUtil.epochMillisToDateString(BASE_TIME)
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = setOf(todayStr),
            freezeUsedDates = emptySet(),
            now = BASE_TIME,
        )
        assertEquals(1, streak)
    }

    @Test
    fun computeStreakWithFreeze_threeDaysInARow() {
        val dates = (0..2).map { DateUtil.daysAgoFromMillis(BASE_TIME, it) }.toSet()
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = dates,
            freezeUsedDates = emptySet(),
            now = BASE_TIME,
        )
        assertEquals(3, streak)
    }

    @Test
    fun computeStreakWithFreeze_gapBreaksStreak() {
        val today = DateUtil.epochMillisToDateString(BASE_TIME)
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = setOf(today, twoDaysAgo),
            freezeUsedDates = emptySet(),
            now = BASE_TIME,
        )
        assertEquals(1, streak) // gap at day 1
    }

    @Test
    fun computeStreakWithFreeze_freezeCoversGap() {
        val today = DateUtil.epochMillisToDateString(BASE_TIME)
        val yesterday = DateUtil.daysAgoFromMillis(BASE_TIME, 1)
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = setOf(today, twoDaysAgo),
            freezeUsedDates = setOf(yesterday), // freeze covers the gap
            now = BASE_TIME,
        )
        assertEquals(3, streak) // today + freeze + twoDaysAgo
    }

    @Test
    fun computeStreakWithFreeze_yesterdayActiveNotToday() {
        val yesterday = DateUtil.daysAgoFromMillis(BASE_TIME, 1)
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        val streak = StreakManager.computeStreakWithFreeze(
            activeDates = setOf(yesterday, twoDaysAgo),
            freezeUsedDates = emptySet(),
            now = BASE_TIME,
        )
        assertEquals(2, streak) // starts from yesterday
    }

    @Test
    fun shouldUseFreeze_yesterdayMissedWithPriorActivity() {
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        assertTrue(
            StreakManager.shouldUseFreeze(
                activeDates = setOf(twoDaysAgo),
                now = BASE_TIME,
            ),
        )
    }

    @Test
    fun shouldUseFreeze_yesterdayActive() {
        val yesterday = DateUtil.daysAgoFromMillis(BASE_TIME, 1)
        assertFalse(
            StreakManager.shouldUseFreeze(
                activeDates = setOf(yesterday),
                now = BASE_TIME,
            ),
        )
    }

    @Test
    fun shouldUseFreeze_noPriorActivity() {
        assertFalse(
            StreakManager.shouldUseFreeze(
                activeDates = emptySet(),
                now = BASE_TIME,
            ),
        )
    }

    @Test
    fun applyFreezeIfNeeded_usesFreeze() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        repo.setSetting("streak_freezes", "2")
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        repo.upsertDailyActivity(twoDaysAgo, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)

        val used = StreakManager.applyFreezeIfNeeded(repo, BASE_TIME)

        assertTrue(used)
        assertEquals(1, StreakManager.getAvailableFreezes(repo))
        assertTrue(StreakManager.isFreezeUsedOnDate(repo, DateUtil.daysAgoFromMillis(BASE_TIME, 1)))
    }

    @Test
    fun applyFreezeIfNeeded_noFreezeAvailable() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        repo.setSetting("streak_freezes", "0")
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        repo.upsertDailyActivity(twoDaysAgo, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)

        val used = StreakManager.applyFreezeIfNeeded(repo, BASE_TIME)

        assertFalse(used)
    }

    @Test
    fun applyFreezeIfNeeded_doesNotDoubleApply() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        repo.setSetting("streak_freezes", "2")
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        repo.upsertDailyActivity(twoDaysAgo, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)

        StreakManager.applyFreezeIfNeeded(repo, BASE_TIME)
        val usedAgain = StreakManager.applyFreezeIfNeeded(repo, BASE_TIME)

        assertFalse(usedAgain) // should not use another freeze for same day
        assertEquals(1, StreakManager.getAvailableFreezes(repo))
    }

    @Test
    fun awardFreezeForMilestone_awardsAt7() {
        val repo = InMemoryDatabaseRepository()
        val awarded = StreakManager.awardFreezeForMilestone(repo, streak = 7)
        assertTrue(awarded)
        assertEquals(1, StreakManager.getAvailableFreezes(repo))
    }

    @Test
    fun awardFreezeForMilestone_awardsAt14() {
        val repo = InMemoryDatabaseRepository()
        StreakManager.awardFreezeForMilestone(repo, streak = 7) // first
        val awarded = StreakManager.awardFreezeForMilestone(repo, streak = 14)
        assertTrue(awarded)
        assertEquals(2, StreakManager.getAvailableFreezes(repo))
    }

    @Test
    fun awardFreezeForMilestone_doesNotAwardNonMilestone() {
        val repo = InMemoryDatabaseRepository()
        val awarded = StreakManager.awardFreezeForMilestone(repo, streak = 5)
        assertFalse(awarded)
    }

    @Test
    fun awardFreezeForMilestone_doesNotDoubleAward() {
        val repo = InMemoryDatabaseRepository()
        StreakManager.awardFreezeForMilestone(repo, streak = 7)
        val second = StreakManager.awardFreezeForMilestone(repo, streak = 7)
        assertFalse(second)
        assertEquals(1, StreakManager.getAvailableFreezes(repo))
    }
}
