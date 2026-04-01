package com.example.cardwords.data.model

import com.example.cardwords.ui.study.StudyMode

data class WordProgress(
    val wordId: Long,
    val mode: StudyMode,
    val correctCount: Int,
    val totalCount: Int,
    val masteryLevel: Int,
    val lastStudiedAt: Long,
    val nextReviewAt: Long,
) {
    val accuracy: Float get() = if (totalCount > 0) correctCount.toFloat() / totalCount else 0f
}

data class StudySession(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val correctCount: Int,
    val totalCount: Int,
    val modesUsed: String,
    val wordSource: String,
)

data class DailyActivity(
    val date: String,
    val wordsStudied: Int,
    val sessionsCount: Int,
    val correctCount: Int,
    val totalCount: Int,
)

object MasteryLevels {
    const val NEW = 0
    const val SEEN = 1
    const val FAMILIAR = 2
    const val LEARNING = 3
    const val KNOWN = 4
    const val MASTERED = 5

    fun labelFor(level: Int): String = when (level) {
        NEW -> "Новое"
        SEEN -> "Видел"
        FAMILIAR -> "Знакомое"
        LEARNING -> "Учу"
        KNOWN -> "Знаю"
        MASTERED -> "Освоено"
        else -> "?"
    }

    fun intervalFor(level: Int): Long = when (level) {
        0 -> 0L
        1 -> 4L * 60 * 60 * 1000           // 4 hours
        2 -> 24L * 60 * 60 * 1000          // 1 day
        3 -> 3L * 24 * 60 * 60 * 1000      // 3 days
        4 -> 7L * 24 * 60 * 60 * 1000      // 7 days
        5 -> 30L * 24 * 60 * 60 * 1000     // 30 days
        else -> 30L * 24 * 60 * 60 * 1000
    }

    fun compute(correctCount: Int, totalCount: Int): Int {
        if (totalCount == 0) return NEW
        val acc = correctCount.toFloat() / totalCount
        return when {
            totalCount >= 8 && acc >= 0.85f -> MASTERED
            totalCount >= 5 && acc >= 0.7f -> KNOWN
            totalCount >= 3 && acc >= 0.4f -> LEARNING
            totalCount >= 2 -> FAMILIAR
            else -> SEEN
        }
    }

    /**
     * Groups raw per-level counts into 4 UI categories.
     * Учу = SEEN + FAMILIAR + LEARNING (levels 1-3)
     */
    fun groupedLabel(level: Int): String = when {
        level >= MASTERED -> "\u041E\u0441\u0432\u043E\u0435\u043D\u043E"
        level >= KNOWN -> "\u0417\u043D\u0430\u044E"
        level >= SEEN -> "\u0423\u0447\u0443"
        else -> "\u041D\u043E\u0432\u044B\u0435"
    }

    fun computeBreakdown(
        minMasteryPerWord: Map<Long, Int>,
        totalDictionaryWords: Int,
    ): MasteryBreakdown {
        val totalTrackedWords = minMasteryPerWord.size
        val newCount = (totalDictionaryWords - totalTrackedWords).coerceAtLeast(0)
        var learningCount = 0
        var knownCount = 0
        var masteredCount = 0
        for ((_, level) in minMasteryPerWord) {
            when {
                level >= MASTERED -> masteredCount++
                level >= KNOWN -> knownCount++
                else -> learningCount++
            }
        }
        return MasteryBreakdown(
            newCount = newCount,
            learningCount = learningCount,
            knownCount = knownCount,
            masteredCount = masteredCount,
        )
    }
}

data class MasteryBreakdown(
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val knownCount: Int = 0,
    val masteredCount: Int = 0,
) {
    val total: Int get() = newCount + learningCount + knownCount + masteredCount
}
