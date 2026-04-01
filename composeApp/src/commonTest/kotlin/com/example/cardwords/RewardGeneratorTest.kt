package com.example.cardwords

import com.example.cardwords.data.model.BonusType
import com.example.cardwords.data.model.RewardGenerator
import com.example.cardwords.data.model.XpReward
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardGeneratorTest {

    private fun makeXpReward(
        baseXp: Int = 75,
        streakBonus: Int = 0,
        perfectBonus: Int = 0,
        leveledUp: Boolean = false,
        oldLevel: Int = 1,
        newLevel: Int = 1,
    ): XpReward {
        val total = baseXp + streakBonus + perfectBonus
        return XpReward(
            baseXp = baseXp,
            streakBonus = streakBonus,
            perfectBonus = perfectBonus,
            totalXp = total,
            newTotalXp = 500 + total,
            oldLevel = oldLevel,
            newLevel = if (leveledUp) oldLevel + 1 else newLevel,
            leveledUp = leveledUp,
        )
    }

    @Test
    fun generateSessionReward_levelUp() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(leveledUp = true),
            accuracy = 0.8f,
            streak = 5,
        )
        assertEquals(BonusType.LEVEL_UP, reward.bonusType)
        assertEquals("\uD83C\uDF1F", reward.emoji)
        assertTrue(reward.motivationalMessage.isNotBlank())
    }

    @Test
    fun generateSessionReward_perfectScore() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(perfectBonus = 50),
            accuracy = 1f,
            streak = 0,
        )
        assertEquals(BonusType.PERFECT_SCORE, reward.bonusType)
        assertEquals("\uD83D\uDCAF", reward.emoji)
    }

    @Test
    fun generateSessionReward_streakBonus() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(streakBonus = 25),
            accuracy = 0.6f,
            streak = 5,
        )
        assertEquals(BonusType.STREAK_BONUS, reward.bonusType)
        assertEquals("\uD83D\uDD25", reward.emoji)
    }

    @Test
    fun generateSessionReward_none() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(),
            accuracy = 0.5f,
            streak = 0,
        )
        assertEquals(BonusType.NONE, reward.bonusType)
    }

    @Test
    fun generateSessionReward_levelUpTakesPriorityOverPerfect() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(perfectBonus = 50, leveledUp = true),
            accuracy = 1f,
            streak = 10,
        )
        assertEquals(BonusType.LEVEL_UP, reward.bonusType) // level up > perfect
    }

    @Test
    fun generateSessionReward_perfectTakesPriorityOverStreak() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(perfectBonus = 50, streakBonus = 25),
            accuracy = 1f,
            streak = 5,
        )
        assertEquals(BonusType.PERFECT_SCORE, reward.bonusType) // perfect > streak
    }

    @Test
    fun generateSessionReward_highAccuracyGoodEmoji() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(),
            accuracy = 0.8f,
            streak = 0,
        )
        assertEquals("\uD83D\uDC4D", reward.emoji) // thumbs up for good accuracy
    }

    @Test
    fun generateSessionReward_lowAccuracyFlexEmoji() {
        val reward = RewardGenerator.generateSessionReward(
            xpReward = makeXpReward(),
            accuracy = 0.3f,
            streak = 0,
        )
        assertEquals("\uD83D\uDCAA", reward.emoji) // flexed biceps for encouragement
    }
}
