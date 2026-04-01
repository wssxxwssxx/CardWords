package com.example.cardwords

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.AchievementChecker
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.util.DateUtil
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementCheckerTest {

    private val BASE_TIME = 1_700_000_000_000L
    private val DAY_MS = 86_400_000L

    private fun createRepo(now: Long = BASE_TIME): InMemoryDatabaseRepository {
        return InMemoryDatabaseRepository(clock = { now })
    }

    private fun addSession(repo: InMemoryDatabaseRepository, correct: Int = 5, total: Int = 10) {
        repo.insertStudySession(
            StudySession(
                id = 0, startedAt = BASE_TIME - 1000, finishedAt = BASE_TIME,
                correctCount = correct, totalCount = total, modesUsed = "MULTIPLE_CHOICE", wordSource = "",
            ),
        )
    }

    @Test
    fun firstSteps_unlockedAfterFirstSession() {
        val repo = createRepo()
        addSession(repo)

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.FIRST_STEPS)
    }

    @Test
    fun firstSteps_notUnlockedWhenNoSession() {
        val repo = createRepo()
        val unlocked = AchievementChecker.checkAll(repo, 0, 0)

        assertFalse(AchievementType.FIRST_STEPS in unlocked)
    }

    @Test
    fun perfectionist_unlockedWith100Percent() {
        val repo = createRepo()
        addSession(repo)

        val unlocked = AchievementChecker.checkAll(repo, 5, 5)

        assertContains(unlocked, AchievementType.PERFECTIONIST)
    }

    @Test
    fun perfectionist_notUnlockedWithLessThan5() {
        val repo = createRepo()
        addSession(repo)

        val unlocked = AchievementChecker.checkAll(repo, 4, 4)

        assertFalse(AchievementType.PERFECTIONIST in unlocked)
    }

    @Test
    fun beginner_unlockedAfter10Sessions() {
        val repo = createRepo()
        repeat(10) { addSession(repo) }

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.BEGINNER)
    }

    @Test
    fun collector_unlockedWith100Words() {
        val repo = createRepo()
        addSession(repo) // need at least one session for FIRST_STEPS
        for (i in 1L..100L) {
            val wordId = repo.insertWord(
                Word(id = 0, original = "word$i", translation = "слово$i", isInDictionary = true, addedAt = BASE_TIME),
            )
            repo.addToDictionary(wordId)
        }

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.COLLECTOR)
    }

    @Test
    fun expert_unlockedWith10MasteredWordModes() {
        val repo = createRepo()
        addSession(repo)

        for (i in 1L..10L) {
            repo.insertWord(
                Word(id = 0, original = "w$i", translation = "t$i", isInDictionary = true, addedAt = BASE_TIME),
            )
            repo.upsertWordProgress(
                WordProgress(
                    wordId = i, mode = StudyMode.MULTIPLE_CHOICE,
                    correctCount = 9, totalCount = 10,
                    masteryLevel = MasteryLevels.MASTERED,
                    lastStudiedAt = BASE_TIME, nextReviewAt = BASE_TIME + DAY_MS,
                ),
            )
        }

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.EXPERT)
    }

    @Test
    fun onFire_unlockedWith7DayStreak() {
        val repo = createRepo()
        addSession(repo)

        for (i in 0..6) {
            val dateStr = DateUtil.daysAgoFromMillis(BASE_TIME, i)
            repo.upsertDailyActivity(dateStr, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)
        }

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.ON_FIRE)
    }

    @Test
    fun xpHunter_unlockedWith1000Xp() {
        val repo = createRepo()
        addSession(repo)
        repo.setSetting("total_xp", "1000")

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.XP_HUNTER)
    }

    @Test
    fun level5_unlockedAtLevel5() {
        val repo = createRepo()
        addSession(repo)
        repo.setSetting("total_xp", "2500") // level 5

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.LEVEL_5)
    }

    @Test
    fun comeback_unlockedAfter3DayGap() {
        val repo = createRepo()
        addSession(repo)

        val todayStr = DateUtil.epochMillisToDateString(BASE_TIME)
        val fiveDaysAgo = DateUtil.daysAgoFromMillis(BASE_TIME, 5)
        repo.upsertDailyActivity(todayStr, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)
        repo.upsertDailyActivity(fiveDaysAgo, wordsStudied = 5, sessionsCount = 1, correctCount = 5, totalCount = 5)

        val unlocked = AchievementChecker.checkAll(repo, 5, 10)

        assertContains(unlocked, AchievementType.COMEBACK)
    }

    @Test
    fun achievements_notDoubleUnlocked() {
        val repo = createRepo()
        addSession(repo)

        val first = AchievementChecker.checkAll(repo, 5, 10)
        assertContains(first, AchievementType.FIRST_STEPS)

        val second = AchievementChecker.checkAll(repo, 5, 10)
        assertFalse(AchievementType.FIRST_STEPS in second) // already unlocked
    }
}
