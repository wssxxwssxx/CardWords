package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository
import kotlin.math.sqrt

data class XpReward(
    val baseXp: Int,
    val streakBonus: Int,
    val perfectBonus: Int,
    val totalXp: Int,
    val newTotalXp: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
)

object XpManager {
    const val XP_PER_CORRECT = 10
    const val XP_SESSION_BONUS = 25
    const val XP_PERFECT_BONUS = 50
    const val XP_STREAK_MULTIPLIER = 5

    private const val SETTING_TOTAL_XP = "total_xp"

    fun xpForSession(correctCount: Int, totalCount: Int, streakDays: Int): Triple<Int, Int, Int> {
        val baseXp = correctCount * XP_PER_CORRECT + XP_SESSION_BONUS
        val perfectBonus = if (totalCount >= 5 && correctCount == totalCount) XP_PERFECT_BONUS else 0
        val streakBonus = if (streakDays > 0) streakDays * XP_STREAK_MULTIPLIER else 0
        return Triple(baseXp, streakBonus, perfectBonus)
    }

    fun levelForXp(totalXp: Int): Int {
        if (totalXp <= 0) return 0
        return sqrt(totalXp.toDouble() / 100.0).toInt()
    }

    fun xpForLevel(level: Int): Int = level * level * 100

    fun xpProgress(totalXp: Int): Pair<Int, Int> {
        val currentLevel = levelForXp(totalXp)
        val currentLevelXp = xpForLevel(currentLevel)
        val nextLevelXp = xpForLevel(currentLevel + 1)
        val progress = totalXp - currentLevelXp
        val needed = nextLevelXp - currentLevelXp
        return Pair(progress, needed)
    }

    fun getTotalXp(repository: DatabaseRepository): Int {
        return repository.getSettingOrDefault(SETTING_TOTAL_XP, "0").toIntOrNull() ?: 0
    }

    fun awardSessionXp(
        repository: DatabaseRepository,
        correctCount: Int,
        totalCount: Int,
        streakDays: Int,
    ): XpReward {
        val (baseXp, streakBonus, perfectBonus) = xpForSession(correctCount, totalCount, streakDays)
        val sessionTotalXp = baseXp + streakBonus + perfectBonus

        val oldTotalXp = getTotalXp(repository)
        val newTotalXp = oldTotalXp + sessionTotalXp
        val oldLevel = levelForXp(oldTotalXp)
        val newLevel = levelForXp(newTotalXp)

        repository.setSetting(SETTING_TOTAL_XP, newTotalXp.toString())

        return XpReward(
            baseXp = baseXp,
            streakBonus = streakBonus,
            perfectBonus = perfectBonus,
            totalXp = sessionTotalXp,
            newTotalXp = newTotalXp,
            oldLevel = oldLevel,
            newLevel = newLevel,
            leveledUp = newLevel > oldLevel,
        )
    }
}
