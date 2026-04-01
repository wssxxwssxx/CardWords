package com.example.cardwords

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.XpManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XpManagerTest {

    @Test
    fun xpForSession_basicCorrectAnswers() {
        val (base, streak, perfect) = XpManager.xpForSession(
            correctCount = 5, totalCount = 10, streakDays = 0,
        )
        assertEquals(5 * 10 + 25, base) // 5 correct * 10 + 25 session bonus
        assertEquals(0, streak)
        assertEquals(0, perfect) // not perfect
    }

    @Test
    fun xpForSession_perfectScoreBonus() {
        val (base, streak, perfect) = XpManager.xpForSession(
            correctCount = 5, totalCount = 5, streakDays = 0,
        )
        assertEquals(5 * 10 + 25, base)
        assertEquals(0, streak)
        assertEquals(50, perfect) // perfect bonus
    }

    @Test
    fun xpForSession_noPerfectBonusWhenLessThan5Questions() {
        val (_, _, perfect) = XpManager.xpForSession(
            correctCount = 4, totalCount = 4, streakDays = 0,
        )
        assertEquals(0, perfect) // needs at least 5 questions
    }

    @Test
    fun xpForSession_streakBonus() {
        val (_, streak, _) = XpManager.xpForSession(
            correctCount = 3, totalCount = 5, streakDays = 10,
        )
        assertEquals(10 * 5, streak) // 10 days * 5 XP per day
    }

    @Test
    fun xpForSession_zeroCorrect() {
        val (base, _, _) = XpManager.xpForSession(
            correctCount = 0, totalCount = 5, streakDays = 0,
        )
        assertEquals(25, base) // only session bonus
    }

    @Test
    fun levelForXp_level0() {
        assertEquals(0, XpManager.levelForXp(0))
        assertEquals(0, XpManager.levelForXp(99))
    }

    @Test
    fun levelForXp_level1() {
        assertEquals(1, XpManager.levelForXp(100))
        assertEquals(1, XpManager.levelForXp(399))
    }

    @Test
    fun levelForXp_level2() {
        assertEquals(2, XpManager.levelForXp(400))
        assertEquals(2, XpManager.levelForXp(899))
    }

    @Test
    fun levelForXp_level3() {
        assertEquals(3, XpManager.levelForXp(900))
    }

    @Test
    fun levelForXp_level5() {
        assertEquals(5, XpManager.levelForXp(2500))
    }

    @Test
    fun levelForXp_negative() {
        assertEquals(0, XpManager.levelForXp(-100))
    }

    @Test
    fun xpForLevel_boundaries() {
        assertEquals(0, XpManager.xpForLevel(0))
        assertEquals(100, XpManager.xpForLevel(1))
        assertEquals(400, XpManager.xpForLevel(2))
        assertEquals(900, XpManager.xpForLevel(3))
        assertEquals(2500, XpManager.xpForLevel(5))
    }

    @Test
    fun xpProgress_midLevel() {
        val (progress, needed) = XpManager.xpProgress(250)
        // Level 1 = 100 XP, Level 2 = 400 XP
        assertEquals(150, progress) // 250 - 100
        assertEquals(300, needed)   // 400 - 100
    }

    @Test
    fun xpProgress_atLevelBoundary() {
        val (progress, needed) = XpManager.xpProgress(400)
        // Level 2 = 400 XP, Level 3 = 900 XP
        assertEquals(0, progress) // 400 - 400
        assertEquals(500, needed) // 900 - 400
    }

    @Test
    fun awardSessionXp_persistsToRepository() {
        val repo = InMemoryDatabaseRepository()
        val reward = XpManager.awardSessionXp(repo, correctCount = 5, totalCount = 10, streakDays = 0)

        assertEquals(75, reward.baseXp) // 5*10 + 25
        assertEquals(0, reward.streakBonus)
        assertEquals(0, reward.perfectBonus)
        assertEquals(75, reward.totalXp)
        assertEquals(75, reward.newTotalXp)
        assertEquals(0, reward.oldLevel)
        assertEquals(0, reward.newLevel)
        assertFalse(reward.leveledUp)

        // Verify persisted
        assertEquals(75, XpManager.getTotalXp(repo))
    }

    @Test
    fun awardSessionXp_levelUp() {
        val repo = InMemoryDatabaseRepository()
        repo.setSetting("total_xp", "90")

        // Award 75 XP → total 165 → level 1
        val reward = XpManager.awardSessionXp(repo, correctCount = 5, totalCount = 10, streakDays = 0)

        assertEquals(0, reward.oldLevel)
        assertEquals(1, reward.newLevel)
        assertTrue(reward.leveledUp)
    }

    @Test
    fun awardSessionXp_accumulatesXp() {
        val repo = InMemoryDatabaseRepository()

        XpManager.awardSessionXp(repo, correctCount = 5, totalCount = 10, streakDays = 0)
        XpManager.awardSessionXp(repo, correctCount = 3, totalCount = 5, streakDays = 1)

        // First: 5*10 + 25 = 75
        // Second: 3*10 + 25 + 1*5 = 60
        assertEquals(135, XpManager.getTotalXp(repo))
    }
}
