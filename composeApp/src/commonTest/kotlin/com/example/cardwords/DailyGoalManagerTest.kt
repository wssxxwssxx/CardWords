package com.example.cardwords

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.DailyGoalManager
import com.example.cardwords.util.DateUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyGoalManagerTest {

    private val DAY_MS = 86_400_000L
    private val BASE_TIME = 1_700_000_000_000L

    @Test
    fun getDailyGoal_default() {
        val repo = InMemoryDatabaseRepository()
        assertEquals(10, DailyGoalManager.getDailyGoal(repo))
    }

    @Test
    fun setAndGetDailyGoal() {
        val repo = InMemoryDatabaseRepository()
        DailyGoalManager.setDailyGoal(repo, 20)
        assertEquals(20, DailyGoalManager.getDailyGoal(repo))
    }

    @Test
    fun setDailyGoal_clampsToBounds() {
        val repo = InMemoryDatabaseRepository()
        DailyGoalManager.setDailyGoal(repo, 0)
        assertEquals(1, DailyGoalManager.getDailyGoal(repo))

        DailyGoalManager.setDailyGoal(repo, 200)
        assertEquals(100, DailyGoalManager.getDailyGoal(repo))
    }

    @Test
    fun getTodayProgress_noActivity() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        val progress = DailyGoalManager.getTodayProgress(repo)

        assertEquals(10, progress.goal)
        assertEquals(0, progress.wordsStudied)
        assertEquals(0f, progress.progressFraction)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun getTodayProgress_partialProgress() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        val todayStr = DateUtil.epochMillisToDateString(BASE_TIME)
        repo.upsertDailyActivity(todayStr, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)

        val progress = DailyGoalManager.getTodayProgress(repo)

        assertEquals(5, progress.wordsStudied)
        assertEquals(0.5f, progress.progressFraction)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun getTodayProgress_goalCompleted() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        val todayStr = DateUtil.epochMillisToDateString(BASE_TIME)
        repo.upsertDailyActivity(todayStr, wordsStudied = 12, sessionsCount = 2, correctCount = 10, totalCount = 12)

        val progress = DailyGoalManager.getTodayProgress(repo)

        assertEquals(12, progress.wordsStudied)
        assertEquals(1f, progress.progressFraction) // capped at 1
        assertTrue(progress.isCompleted)
    }

    @Test
    fun countConsecutiveGoalDays_noDays() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        assertEquals(0, DailyGoalManager.countConsecutiveGoalDays(repo))
    }

    @Test
    fun countConsecutiveGoalDays_threeDaysInRow() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        for (i in 0..2) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)
        }

        assertEquals(3, DailyGoalManager.countConsecutiveGoalDays(repo))
    }

    @Test
    fun countConsecutiveGoalDays_gapBreaksStreak() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        // Today and 2 days ago have activity, but yesterday doesn't
        val today = DateUtil.epochMillisToDateString(BASE_TIME)
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)
        repo.upsertDailyActivity(today, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)
        repo.upsertDailyActivity(twoDaysAgo, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)

        assertEquals(1, DailyGoalManager.countConsecutiveGoalDays(repo)) // only today counts
    }

    @Test
    fun countConsecutiveGoalDays_belowGoalBreaksStreak() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        val today = DateUtil.epochMillisToDateString(BASE_TIME)
        val yesterday = DateUtil.daysAgoFromMillis(BASE_TIME, 1)
        val twoDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 2)

        repo.upsertDailyActivity(today, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)
        repo.upsertDailyActivity(yesterday, wordsStudied = 3, sessionsCount = 1, correctCount = 3, totalCount = 3) // below goal
        repo.upsertDailyActivity(twoDaysAgo, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)

        assertEquals(1, DailyGoalManager.countConsecutiveGoalDays(repo)) // yesterday below goal breaks it
    }

    // ── Weekly Summary Tests ──

    @Test
    fun weeklySummary_noActivity() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        val summary = DailyGoalManager.computeWeeklySummary(repo)

        assertEquals(0, summary.thisWeekWords)
        assertEquals(0, summary.lastWeekWords)
        assertEquals(null, summary.changePercent)
    }

    @Test
    fun weeklySummary_thisWeekOnly() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        for (i in 0..2) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)
        }

        val summary = DailyGoalManager.computeWeeklySummary(repo)

        assertEquals(30, summary.thisWeekWords)
        assertEquals(0, summary.lastWeekWords)
        assertEquals(null, summary.changePercent) // no last week data
    }

    @Test
    fun weeklySummary_bothWeeks_improvement() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        // This week: 5 words/day for 7 days = 35
        for (i in 0..6) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)
        }
        // Last week: 3 words/day for 7 days = 21
        for (i in 7..13) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 3, sessionsCount = 1, correctCount = 3, totalCount = 3)
        }

        val summary = DailyGoalManager.computeWeeklySummary(repo)

        assertEquals(35, summary.thisWeekWords)
        assertEquals(21, summary.lastWeekWords)
        assertEquals(66, summary.changePercent) // (35-21)*100/21 = 66%
    }

    @Test
    fun weeklySummary_bothWeeks_decline() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        // This week: 2 words/day for 7 days = 14
        for (i in 0..6) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 2, sessionsCount = 1, correctCount = 2, totalCount = 2)
        }
        // Last week: 5 words/day for 7 days = 35
        for (i in 7..13) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)
        }

        val summary = DailyGoalManager.computeWeeklySummary(repo)

        assertEquals(14, summary.thisWeekWords)
        assertEquals(35, summary.lastWeekWords)
        assertEquals(-60, summary.changePercent) // (14-35)*100/35 = -60%
    }

    @Test
    fun weeklySummary_sameAsBefore() {
        val repo = InMemoryDatabaseRepository(clock = { BASE_TIME })
        for (i in 0..13) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 10, sessionsCount = 1, correctCount = 10, totalCount = 10)
        }

        val summary = DailyGoalManager.computeWeeklySummary(repo)

        assertEquals(70, summary.thisWeekWords)
        assertEquals(70, summary.lastWeekWords)
        assertEquals(0, summary.changePercent) // no change
    }
}
