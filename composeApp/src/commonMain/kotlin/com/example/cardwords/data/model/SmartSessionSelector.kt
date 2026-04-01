package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository

object SmartSessionSelector {

    data class SmartSessionResult(
        val wordIds: List<Long>,
        val hasWords: Boolean,
    )

    fun selectWords(
        repository: DatabaseRepository,
        maxWords: Int = 20,
    ): SmartSessionResult {
        val now = repository.currentTimeMillis()
        val dictionaryWords = repository.getDictionaryWords()
        if (dictionaryWords.isEmpty()) {
            return SmartSessionResult(wordIds = emptyList(), hasWords = false)
        }

        val dictionaryWordIds = dictionaryWords.map { it.id }.toSet()

        // 1. Words needing review (scheduled review time has passed)
        // Filter by dictionary — getWordsNeedingReview() returns ALL word_progress rows
        val reviewWordIds = repository.getWordsNeedingReview(now)
            .map { it.wordId }
            .filter { it in dictionaryWordIds }
            .distinct()

        // 2. New words (in dictionary but never studied in any mode)
        val wordsWithProgress = repository.getAllWordProgress()
            .map { it.wordId }
            .toSet()
        val newWordIds = dictionaryWords
            .filter { it.id !in wordsWithProgress }
            .map { it.id }

        // 3. Low-mastery words (below LEARNING threshold), dictionary only
        val minMasteryPerWord = repository.getMinMasteryPerWord()
        val lowMasteryIds = minMasteryPerWord
            .filter { it.key in dictionaryWordIds && it.value < MasteryLevels.LEARNING }
            .keys
            .toList()

        // Combine with priority: review first, then new, then low mastery
        val smartIds = (reviewWordIds + newWordIds + lowMasteryIds)
            .distinct()
            .take(maxWords)

        return SmartSessionResult(
            wordIds = smartIds,
            hasWords = smartIds.isNotEmpty(),
        )
    }
}
