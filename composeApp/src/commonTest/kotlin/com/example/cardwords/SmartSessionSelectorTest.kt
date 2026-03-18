package com.example.cardwords

import com.example.cardwords.data.local.InMemoryDatabaseRepository
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.SmartSessionSelector
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.study.StudyMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartSessionSelectorTest {

    private fun createRepo(now: Long = 1_000_000L): InMemoryDatabaseRepository {
        return InMemoryDatabaseRepository(clock = { now })
    }

    private fun InMemoryDatabaseRepository.addDictionaryWord(
        original: String,
        translation: String,
    ): Long {
        val id = insertWord(
            Word(
                id = 0,
                original = original,
                translation = translation,
                isInDictionary = true,
                addedAt = currentTimeMillis(),
            )
        )
        return id
    }

    // --- Empty state ---

    @Test
    fun emptyDictionary_returnsNoWords() {
        val repo = createRepo()
        val result = SmartSessionSelector.selectWords(repo)
        assertFalse(result.hasWords)
        assertTrue(result.wordIds.isEmpty())
    }

    // --- New words (no progress) ---

    @Test
    fun singleNewWord_isSelected() {
        val repo = createRepo()
        val id = repo.addDictionaryWord("hello", "привет")
        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        assertEquals(listOf(id), result.wordIds)
    }

    @Test
    fun multipleNewWords_allSelected() {
        val repo = createRepo()
        val id1 = repo.addDictionaryWord("hello", "привет")
        val id2 = repo.addDictionaryWord("world", "мир")
        val id3 = repo.addDictionaryWord("cat", "кот")
        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        assertEquals(3, result.wordIds.size)
        assertTrue(id1 in result.wordIds)
        assertTrue(id2 in result.wordIds)
        assertTrue(id3 in result.wordIds)
    }

    @Test
    fun newWords_limitedToMaxWords() {
        val repo = createRepo()
        // Add 25 words
        repeat(25) { i ->
            repo.addDictionaryWord("word$i", "слово$i")
        }
        val result = SmartSessionSelector.selectWords(repo, maxWords = 20)
        assertEquals(20, result.wordIds.size)
    }

    // --- Words needing review ---

    @Test
    fun wordNeedingReview_isSelected() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val id = repo.addDictionaryWord("hello", "привет")

        // Add progress with next_review_at in the past (due for review)
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 3,
                totalCount = 5,
                masteryLevel = MasteryLevels.LEARNING,
                lastStudiedAt = now - 100_000,
                nextReviewAt = now - 1000, // overdue
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        assertTrue(id in result.wordIds)
    }

    @Test
    fun wordNotYetDue_notInReviewList_butInLowMastery() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val id = repo.addDictionaryWord("hello", "привет")

        // Word has been studied, mastery = SEEN (level 1, < LEARNING), but not yet due
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 1,
                totalCount = 1,
                masteryLevel = MasteryLevels.SEEN,
                lastStudiedAt = now - 1000,
                nextReviewAt = now + 100_000, // far future
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        // Should be selected via low-mastery path (SEEN < LEARNING)
        assertTrue(id in result.wordIds)
    }

    // --- Low mastery words ---

    @Test
    fun lowMasteryWord_isSelected() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val id = repo.addDictionaryWord("hello", "привет")

        // FAMILIAR level (< LEARNING threshold)
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.TYPING,
                correctCount = 2,
                totalCount = 2,
                masteryLevel = MasteryLevels.FAMILIAR,
                lastStudiedAt = now - 5000,
                nextReviewAt = now + 500_000,
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        assertTrue(id in result.wordIds)
    }

    @Test
    fun masteredWord_notSelected_whenNoReviewDue() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val id = repo.addDictionaryWord("hello", "привет")

        // Fully mastered, review far in future
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 9,
                totalCount = 10,
                masteryLevel = MasteryLevels.MASTERED,
                lastStudiedAt = now - 1000,
                nextReviewAt = now + 1_000_000, // far future
            )
        )
        // Also mastered in another mode
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.TYPING,
                correctCount = 8,
                totalCount = 9,
                masteryLevel = MasteryLevels.MASTERED,
                lastStudiedAt = now - 1000,
                nextReviewAt = now + 1_000_000,
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertFalse(result.hasWords)
        assertTrue(result.wordIds.isEmpty())
    }

    // --- Priority order ---

    @Test
    fun reviewWordsComeBefore_newWords() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val newId = repo.addDictionaryWord("new", "новое")
        val reviewId = repo.addDictionaryWord("review", "повтор")

        // reviewId has overdue review
        repo.upsertWordProgress(
            WordProgress(
                wordId = reviewId,
                mode = StudyMode.FLASHCARD,
                correctCount = 3,
                totalCount = 4,
                masteryLevel = MasteryLevels.KNOWN,
                lastStudiedAt = now - 100_000,
                nextReviewAt = now - 500, // overdue
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        // Review word should come first
        assertEquals(reviewId, result.wordIds.first())
        assertTrue(newId in result.wordIds)
    }

    // --- Deduplication ---

    @Test
    fun wordInMultipleCategories_appearsOnce() {
        val now = 1_000_000L
        val repo = createRepo(now)
        val id = repo.addDictionaryWord("hello", "привет")

        // Low mastery AND overdue review
        repo.upsertWordProgress(
            WordProgress(
                wordId = id,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 1,
                totalCount = 2,
                masteryLevel = MasteryLevels.SEEN,
                lastStudiedAt = now - 50_000,
                nextReviewAt = now - 1000, // overdue
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        // Should appear exactly once despite being in both review and low-mastery lists
        assertEquals(1, result.wordIds.count { it == id })
    }

    // --- Mixed realistic scenario ---

    @Test
    fun mixedScenario_correctSelection() {
        val now = 1_000_000L
        val repo = createRepo(now)

        // 1. New word (no progress)
        val newWord = repo.addDictionaryWord("new", "новое")

        // 2. Overdue review word
        val overdueWord = repo.addDictionaryWord("overdue", "просрочено")
        repo.upsertWordProgress(
            WordProgress(
                wordId = overdueWord,
                mode = StudyMode.TYPING,
                correctCount = 4,
                totalCount = 5,
                masteryLevel = MasteryLevels.KNOWN,
                lastStudiedAt = now - 200_000,
                nextReviewAt = now - 5000,
            )
        )

        // 3. Low mastery word (not due for review yet)
        val lowMastery = repo.addDictionaryWord("low", "низкий")
        repo.upsertWordProgress(
            WordProgress(
                wordId = lowMastery,
                mode = StudyMode.FLASHCARD,
                correctCount = 1,
                totalCount = 2,
                masteryLevel = MasteryLevels.SEEN,
                lastStudiedAt = now - 10_000,
                nextReviewAt = now + 500_000,
            )
        )

        // 4. Mastered word (not due, not selected)
        val masteredWord = repo.addDictionaryWord("mastered", "освоено")
        repo.upsertWordProgress(
            WordProgress(
                wordId = masteredWord,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 9,
                totalCount = 10,
                masteryLevel = MasteryLevels.MASTERED,
                lastStudiedAt = now - 5000,
                nextReviewAt = now + 2_000_000,
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)

        // Should include: overdue (review), new (no progress), low mastery
        assertTrue(overdueWord in result.wordIds, "Overdue word should be selected")
        assertTrue(newWord in result.wordIds, "New word should be selected")
        assertTrue(lowMastery in result.wordIds, "Low mastery word should be selected")
        // Mastered word should NOT be included (mastery >= LEARNING, not due)
        assertFalse(masteredWord in result.wordIds, "Mastered word should not be selected")

        // Order: review first
        val overdueIndex = result.wordIds.indexOf(overdueWord)
        val newIndex = result.wordIds.indexOf(newWord)
        assertTrue(overdueIndex < newIndex, "Review words should come before new words")
    }

    // --- Non-dictionary words excluded ---

    @Test
    fun nonDictionaryWords_excluded() {
        val repo = createRepo()
        // Insert word but NOT in dictionary
        repo.insertWord(
            Word(
                id = 0,
                original = "hello",
                translation = "привет",
                isInDictionary = false,
            )
        )

        val result = SmartSessionSelector.selectWords(repo)
        assertFalse(result.hasWords)
    }

    // --- Non-dictionary words with progress should not pollute results ---

    @Test
    fun nonDictionaryWordsWithOverdueReview_notSelected() {
        val now = 1_000_000L
        val repo = createRepo(now)

        // Non-dictionary word with overdue review
        val nonDictId = repo.insertWord(
            Word(
                id = 0,
                original = "ghost",
                translation = "призрак",
                isInDictionary = false,
            )
        )
        repo.upsertWordProgress(
            WordProgress(
                wordId = nonDictId,
                mode = StudyMode.MULTIPLE_CHOICE,
                correctCount = 3,
                totalCount = 5,
                masteryLevel = MasteryLevels.LEARNING,
                lastStudiedAt = now - 100_000,
                nextReviewAt = now - 1000, // overdue
            )
        )

        // Dictionary word (new, no progress)
        val dictId = repo.addDictionaryWord("hello", "привет")

        val result = SmartSessionSelector.selectWords(repo)
        assertTrue(result.hasWords)
        assertTrue(dictId in result.wordIds, "Dictionary word should be selected")
        assertFalse(nonDictId in result.wordIds, "Non-dictionary word should NOT be selected")
    }

    @Test
    fun manyNonDictionaryOverdueWords_dontBlockDictionaryWords() {
        val now = 1_000_000L
        val repo = createRepo(now)

        // Add 25 non-dictionary words with overdue reviews
        repeat(25) { i ->
            val id = repo.insertWord(
                Word(
                    id = 0,
                    original = "nope$i",
                    translation = "нет$i",
                    isInDictionary = false,
                )
            )
            repo.upsertWordProgress(
                WordProgress(
                    wordId = id,
                    mode = StudyMode.TYPING,
                    correctCount = 2,
                    totalCount = 3,
                    masteryLevel = MasteryLevels.SEEN,
                    lastStudiedAt = now - 50_000,
                    nextReviewAt = now - 1000, // overdue
                )
            )
        }

        // Add 5 dictionary words
        val dictIds = (1..5).map { i ->
            repo.addDictionaryWord("word$i", "слово$i")
        }

        val result = SmartSessionSelector.selectWords(repo, maxWords = 20)
        assertTrue(result.hasWords)
        assertEquals(5, result.wordIds.size)
        dictIds.forEach { id ->
            assertTrue(id in result.wordIds, "Dictionary word $id should be selected")
        }
    }
}
