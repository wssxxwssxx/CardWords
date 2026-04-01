package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.util.DateUtil

object AchievementChecker {

    /**
     * Check all achievement conditions and unlock any that are newly met.
     * Returns the list of achievement types that were NEWLY unlocked in this call.
     */
    fun checkAll(
        repository: DatabaseRepository,
        sessionCorrectCount: Int = 0,
        sessionTotalCount: Int = 0,
    ): List<AchievementType> {
        val now = repository.currentTimeMillis()
        val newlyUnlocked = mutableListOf<AchievementType>()

        fun tryUnlock(type: AchievementType) {
            if (repository.insertAchievement(type.name, now)) {
                newlyUnlocked.add(type)
            }
        }

        // 1. FIRST_STEPS: complete first session
        if (repository.getSessionCount() >= 1) {
            tryUnlock(AchievementType.FIRST_STEPS)
        }

        // Load progress data once
        val allProgress = repository.getAllWordProgress()
        val allModes = StudyMode.entries.toSet()
        val progressByWord = allProgress.groupBy { it.wordId }

        // 2. EXPERT: 10 word-mode pairs at mastery level 5
        val masteredCount = allProgress.count { it.masteryLevel >= MasteryLevels.MASTERED }
        if (masteredCount >= 10) {
            tryUnlock(AchievementType.EXPERT)
        }

        // 3. POLYGLOT: 50 words mastered in ALL 4 modes
        val fullyMasteredCount = progressByWord.count { (_, entries) ->
            val masteredModes = entries
                .filter { it.masteryLevel >= MasteryLevels.MASTERED }
                .map { it.mode }
                .toSet()
            masteredModes.containsAll(allModes)
        }
        if (fullyMasteredCount >= 50) {
            tryUnlock(AchievementType.POLYGLOT)
        }

        // 4. ON_FIRE: 7-day streak
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()
        val streakFreezeDates = StreakManager.getFreezeUsedDates(repository, now)
        val streak = StreakManager.computeStreakWithFreeze(activeDates, streakFreezeDates, now)
        if (streak >= 7) {
            tryUnlock(AchievementType.ON_FIRE)
        }

        // 5. PERFECTIONIST: 100% accuracy in current session with min 5 questions
        if (sessionTotalCount >= 5 && sessionCorrectCount == sessionTotalCount) {
            tryUnlock(AchievementType.PERFECTIONIST)
        }

        // 6. MARATHON: 30-day streak
        if (streak >= 30) {
            tryUnlock(AchievementType.MARATHON)
        }

        // 7. COLLECTOR: 100 words in dictionary
        if (repository.getDictionaryWordCount() >= 100) {
            tryUnlock(AchievementType.COLLECTOR)
        }

        // 8. BEGINNER: 10 sessions
        if (repository.getSessionCount() >= 10) {
            tryUnlock(AchievementType.BEGINNER)
        }

        // 9. ERUDITE: words from 3+ different sources in dictionary
        val dictionaryWords = repository.getDictionaryWords()
        val dictSources = dictionaryWords.map { it.source }.filter { it.isNotEmpty() }.toSet()
        if (dictSources.size >= 3) {
            tryUnlock(AchievementType.ERUDITE)
        }

        // 10. WORD_MASTER: at least 1 word mastered in all 4 modes
        val anyFullyMastered = progressByWord.any { (_, entries) ->
            val masteredModes = entries
                .filter { it.masteryLevel >= MasteryLevels.MASTERED }
                .map { it.mode }
                .toSet()
            masteredModes.containsAll(allModes)
        }
        if (anyFullyMastered) {
            tryUnlock(AchievementType.WORD_MASTER)
        }

        // 11. GOAL_SETTER: daily goal met 7 days in a row
        val goalDaysInRow = DailyGoalManager.countConsecutiveGoalDays(repository)
        if (goalDaysInRow >= 7) {
            tryUnlock(AchievementType.GOAL_SETTER)
        }

        // 12. XP_HUNTER: 1000 total XP
        val totalXp = XpManager.getTotalXp(repository)
        if (totalXp >= 1000) {
            tryUnlock(AchievementType.XP_HUNTER)
        }

        // 13. LEVEL_5: reach level 5
        if (XpManager.levelForXp(totalXp) >= 5) {
            tryUnlock(AchievementType.LEVEL_5)
        }

        // 14. STREAK_GUARDIAN: used a streak freeze at least once
        val freezeUsedDates = StreakManager.getFreezeUsedDates(repository, now, lookbackDays = 365)
        if (freezeUsedDates.isNotEmpty()) {
            tryUnlock(AchievementType.STREAK_GUARDIAN)
        }

        // 15. COMEBACK: returned after 3+ days of inactivity
        if (allActivity.size >= 2) {
            val todayStr = DateUtil.epochMillisToDateString(now)
            if (todayStr in activeDates) {
                // Check gap before today
                var gapDays = 0
                for (i in 1..365) {
                    val dateStr = DateUtil.daysAgoFromMillis(now, i)
                    if (dateStr in activeDates) break
                    gapDays++
                }
                if (gapDays >= 3) {
                    tryUnlock(AchievementType.COMEBACK)
                }
            }
        }

        return newlyUnlocked
    }

}
