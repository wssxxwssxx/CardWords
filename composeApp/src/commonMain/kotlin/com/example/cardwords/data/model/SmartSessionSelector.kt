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
        val dictionaryWords = repository.getDictionaryWords()
        if (dictionaryWords.isEmpty()) {
            return SmartSessionResult(emptyList(), hasWords = false)
        }

        val dictionaryWordIds = dictionaryWords.mapTo(HashSet()) { it.id }
        val now = repository.currentTimeMillis()

        // 1. Words needing review — filter to dictionary words only
        val reviewWordIds = repository.getWordsNeedingReview(now)
            .mapNotNull { wp -> wp.wordId.takeIf { it in dictionaryWordIds } }
            .distinct()

        // 2. New words (never studied)
        val studiedWordIds = repository.getAllWordProgress()
            .mapTo(HashSet()) { it.wordId }
        val newWordIds = dictionaryWords
            .filter { it.id !in studiedWordIds }
            .map { it.id }

        // 3. Low-mastery words
        val lowMasteryIds = repository.getMinMasteryPerWord()
            .filter { (id, level) -> id in dictionaryWordIds && level < MasteryLevels.LEARNING }
            .keys

        // Combine: review → new → low mastery, deduplicated
        val seen = HashSet<Long>(maxWords)
        val smartIds = buildList(maxWords) {
            for (id in reviewWordIds) if (seen.add(id) && size < maxWords) add(id)
            for (id in newWordIds) if (seen.add(id) && size < maxWords) add(id)
            for (id in lowMasteryIds) if (seen.add(id) && size < maxWords) add(id)
        }

        return SmartSessionResult(smartIds, hasWords = smartIds.isNotEmpty())
    }
}
