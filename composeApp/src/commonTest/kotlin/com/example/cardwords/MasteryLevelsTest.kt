package com.example.cardwords

import com.example.cardwords.data.model.MasteryLevels
import kotlin.test.Test
import kotlin.test.assertEquals

class MasteryLevelsTest {

    @Test
    fun compute_noAttempts() {
        assertEquals(MasteryLevels.NEW, MasteryLevels.compute(0, 0))
    }

    @Test
    fun compute_oneAttempt() {
        assertEquals(MasteryLevels.SEEN, MasteryLevels.compute(1, 1))
        assertEquals(MasteryLevels.SEEN, MasteryLevels.compute(0, 1))
    }

    @Test
    fun compute_twoAttempts() {
        assertEquals(MasteryLevels.FAMILIAR, MasteryLevels.compute(2, 2))
        assertEquals(MasteryLevels.FAMILIAR, MasteryLevels.compute(0, 2))
    }

    @Test
    fun compute_learning() {
        // 3+ attempts, 40%+ accuracy
        assertEquals(MasteryLevels.LEARNING, MasteryLevels.compute(2, 3))
        assertEquals(MasteryLevels.LEARNING, MasteryLevels.compute(3, 5))
    }

    @Test
    fun compute_known() {
        // 5+ attempts, 70%+ accuracy
        assertEquals(MasteryLevels.KNOWN, MasteryLevels.compute(4, 5))
        assertEquals(MasteryLevels.KNOWN, MasteryLevels.compute(5, 7))
    }

    @Test
    fun compute_mastered() {
        // 8+ attempts, 85%+ accuracy
        assertEquals(MasteryLevels.MASTERED, MasteryLevels.compute(7, 8))
        assertEquals(MasteryLevels.MASTERED, MasteryLevels.compute(9, 10))
    }

    @Test
    fun compute_lowAccuracyHighAttempts() {
        // 8 attempts but only 50% accuracy → LEARNING
        assertEquals(MasteryLevels.LEARNING, MasteryLevels.compute(4, 8))
    }

    @Test
    fun compute_edgeCaseLearningThreshold() {
        // 3 attempts, exactly 33% → FAMILIAR (below 40% threshold)
        assertEquals(MasteryLevels.FAMILIAR, MasteryLevels.compute(1, 3))
    }

    @Test
    fun intervalFor_allLevels() {
        assertEquals(0L, MasteryLevels.intervalFor(0))
        assertEquals(4L * 60 * 60 * 1000, MasteryLevels.intervalFor(1))       // 4h
        assertEquals(24L * 60 * 60 * 1000, MasteryLevels.intervalFor(2))      // 1d
        assertEquals(3L * 24 * 60 * 60 * 1000, MasteryLevels.intervalFor(3))  // 3d
        assertEquals(7L * 24 * 60 * 60 * 1000, MasteryLevels.intervalFor(4))  // 7d
        assertEquals(30L * 24 * 60 * 60 * 1000, MasteryLevels.intervalFor(5)) // 30d
    }

    @Test
    fun labelFor_allLevels() {
        assertEquals("Новое", MasteryLevels.labelFor(0))
        assertEquals("Видел", MasteryLevels.labelFor(1))
        assertEquals("Знакомое", MasteryLevels.labelFor(2))
        assertEquals("Учу", MasteryLevels.labelFor(3))
        assertEquals("Знаю", MasteryLevels.labelFor(4))
        assertEquals("Освоено", MasteryLevels.labelFor(5))
        assertEquals("?", MasteryLevels.labelFor(99))
    }

    // --- computeBreakdown tests ---

    @Test
    fun breakdown_emptyDictionary() {
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = emptyMap(),
            totalDictionaryWords = 0,
        )
        assertEquals(0, breakdown.newCount)
        assertEquals(0, breakdown.learningCount)
        assertEquals(0, breakdown.knownCount)
        assertEquals(0, breakdown.masteredCount)
        assertEquals(0, breakdown.total)
    }

    @Test
    fun breakdown_allWordsNew_noProgress() {
        // 10 dictionary words, none studied yet
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = emptyMap(),
            totalDictionaryWords = 10,
        )
        assertEquals(10, breakdown.newCount)
        assertEquals(0, breakdown.learningCount)
        assertEquals(0, breakdown.knownCount)
        assertEquals(0, breakdown.masteredCount)
        assertEquals(10, breakdown.total)
    }

    @Test
    fun breakdown_groupsSeenFamiliarLearning_intoLearning() {
        // Levels 1 (SEEN), 2 (FAMILIAR), 3 (LEARNING) all go into "learningCount"
        val minMastery = mapOf(
            1L to MasteryLevels.SEEN,      // level 1
            2L to MasteryLevels.FAMILIAR,  // level 2
            3L to MasteryLevels.LEARNING,  // level 3
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 3,
        )
        assertEquals(0, breakdown.newCount)
        assertEquals(3, breakdown.learningCount) // all 3 grouped as "Учу"
        assertEquals(0, breakdown.knownCount)
        assertEquals(0, breakdown.masteredCount)
    }

    @Test
    fun breakdown_known_isLevel4Only() {
        val minMastery = mapOf(
            1L to MasteryLevels.KNOWN, // level 4
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 1,
        )
        assertEquals(0, breakdown.newCount)
        assertEquals(0, breakdown.learningCount)
        assertEquals(1, breakdown.knownCount)
        assertEquals(0, breakdown.masteredCount)
    }

    @Test
    fun breakdown_mastered_isLevel5() {
        val minMastery = mapOf(
            1L to MasteryLevels.MASTERED, // level 5
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 1,
        )
        assertEquals(0, breakdown.newCount)
        assertEquals(0, breakdown.learningCount)
        assertEquals(0, breakdown.knownCount)
        assertEquals(1, breakdown.masteredCount)
    }

    @Test
    fun breakdown_mixedLevels_matchesExpected() {
        // Simulates real scenario: 15 dictionary words
        // 3 not studied (new), 2 SEEN, 2 FAMILIAR, 1 LEARNING, 3 KNOWN, 4 MASTERED
        val minMastery = mapOf(
            1L to MasteryLevels.SEEN,
            2L to MasteryLevels.SEEN,
            3L to MasteryLevels.FAMILIAR,
            4L to MasteryLevels.FAMILIAR,
            5L to MasteryLevels.LEARNING,
            6L to MasteryLevels.KNOWN,
            7L to MasteryLevels.KNOWN,
            8L to MasteryLevels.KNOWN,
            9L to MasteryLevels.MASTERED,
            10L to MasteryLevels.MASTERED,
            11L to MasteryLevels.MASTERED,
            12L to MasteryLevels.MASTERED,
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 15,
        )
        assertEquals(3, breakdown.newCount)       // 15 - 12 tracked = 3
        assertEquals(5, breakdown.learningCount)  // SEEN(2) + FAMILIAR(2) + LEARNING(1)
        assertEquals(3, breakdown.knownCount)     // KNOWN(3)
        assertEquals(4, breakdown.masteredCount)  // MASTERED(4)
        assertEquals(15, breakdown.total)
    }

    @Test
    fun breakdown_totalAlwaysMatchesDictionarySize() {
        // Even with some tracked words, total should equal totalDictionaryWords
        val minMastery = mapOf(
            1L to MasteryLevels.MASTERED,
            2L to MasteryLevels.LEARNING,
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 5,
        )
        assertEquals(5, breakdown.total)
        assertEquals(3, breakdown.newCount)       // 5 - 2 = 3
        assertEquals(1, breakdown.learningCount)
        assertEquals(0, breakdown.knownCount)
        assertEquals(1, breakdown.masteredCount)
    }

    @Test
    fun breakdown_trackedMoreThanDictionary_newCountZero() {
        // Edge case: more tracked words than dictionary (shouldn't happen, but be safe)
        val minMastery = mapOf(
            1L to MasteryLevels.SEEN,
            2L to MasteryLevels.KNOWN,
        )
        val breakdown = MasteryLevels.computeBreakdown(
            minMasteryPerWord = minMastery,
            totalDictionaryWords = 1,
        )
        assertEquals(0, breakdown.newCount) // coerceAtLeast(0)
        assertEquals(1, breakdown.learningCount)
        assertEquals(1, breakdown.knownCount)
    }

    // --- groupedLabel tests ---

    @Test
    fun groupedLabel_level0_isNew() {
        assertEquals("Новые", MasteryLevels.groupedLabel(MasteryLevels.NEW))
    }

    @Test
    fun groupedLabel_levels1to3_isLearning() {
        assertEquals("Учу", MasteryLevels.groupedLabel(MasteryLevels.SEEN))
        assertEquals("Учу", MasteryLevels.groupedLabel(MasteryLevels.FAMILIAR))
        assertEquals("Учу", MasteryLevels.groupedLabel(MasteryLevels.LEARNING))
    }

    @Test
    fun groupedLabel_level4_isKnown() {
        assertEquals("Знаю", MasteryLevels.groupedLabel(MasteryLevels.KNOWN))
    }

    @Test
    fun groupedLabel_level5_isMastered() {
        assertEquals("Освоено", MasteryLevels.groupedLabel(MasteryLevels.MASTERED))
    }

    // --- Consistency: computeBreakdown groups match groupedLabel ---

    @Test
    fun breakdown_grouping_matchesGroupedLabel() {
        // Verify that computeBreakdown uses the same thresholds as groupedLabel
        // For each level, check which breakdown bucket it lands in
        for (level in 0..5) {
            val minMastery = mapOf(1L to level)
            val breakdown = MasteryLevels.computeBreakdown(minMastery, 1)
            val label = MasteryLevels.groupedLabel(level)

            when (label) {
                "Новые" -> {
                    // NEW level with tracked word → learningCount (since it's tracked)
                    // Actually level 0 with tracking means it IS tracked but at level 0
                    // computeBreakdown: level < KNOWN and level < MASTERED → learningCount
                    assertEquals(0, breakdown.newCount, "Level $level: newCount")
                    assertEquals(1, breakdown.learningCount, "Level $level: learningCount")
                }
                "Учу" -> {
                    assertEquals(0, breakdown.newCount, "Level $level: newCount")
                    assertEquals(1, breakdown.learningCount, "Level $level: learningCount")
                    assertEquals(0, breakdown.knownCount, "Level $level: knownCount")
                    assertEquals(0, breakdown.masteredCount, "Level $level: masteredCount")
                }
                "Знаю" -> {
                    assertEquals(0, breakdown.newCount, "Level $level: newCount")
                    assertEquals(0, breakdown.learningCount, "Level $level: learningCount")
                    assertEquals(1, breakdown.knownCount, "Level $level: knownCount")
                    assertEquals(0, breakdown.masteredCount, "Level $level: masteredCount")
                }
                "Освоено" -> {
                    assertEquals(0, breakdown.newCount, "Level $level: newCount")
                    assertEquals(0, breakdown.learningCount, "Level $level: learningCount")
                    assertEquals(0, breakdown.knownCount, "Level $level: knownCount")
                    assertEquals(1, breakdown.masteredCount, "Level $level: masteredCount")
                }
            }
        }
    }
}
