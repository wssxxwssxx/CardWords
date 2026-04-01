package com.example.cardwords.data.local

import com.example.cardwords.data.model.Achievement
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.data.repository.HardcodedWordRepository
import com.example.cardwords.ui.study.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryDatabaseRepository(
    private val clock: () -> Long = { 0L },
) : DatabaseRepository {

    private val words = mutableListOf<Word>()
    private var nextId = 1L
    private val _wordCountTrigger = MutableStateFlow(0L)

    private val wordProgressMap = mutableMapOf<Pair<Long, StudyMode>, WordProgress>()
    private val sessions = mutableListOf<StudySession>()
    private var nextSessionId = 1L
    private val dailyActivityMap = mutableMapOf<String, DailyActivity>()
    private val achievementMap = mutableMapOf<String, Achievement>()
    private var nextAchievementId = 1L
    private val settingsMap = mutableMapOf<String, String>()

    private fun refreshCount() {
        _wordCountTrigger.value = words.count { it.isInDictionary }.toLong()
    }

    override fun getAllWords(): List<Word> = words.toList()

    override fun getWordById(id: Long): Word? = words.find { it.id == id }

    override fun getDictionaryWords(): List<Word> {
        return words.filter { it.isInDictionary }.sortedByDescending { it.addedAt }
    }

    override fun getDictionaryWordCount(): Long {
        return words.count { it.isInDictionary }.toLong()
    }

    override fun addToDictionary(wordId: Long) {
        val index = words.indexOfFirst { it.id == wordId }
        if (index >= 0) {
            words[index] = words[index].copy(isInDictionary = true, addedAt = nextId++)
        }
        refreshCount()
    }

    override fun removeFromDictionary(wordId: Long) {
        val index = words.indexOfFirst { it.id == wordId }
        if (index >= 0) {
            words[index] = words[index].copy(isInDictionary = false, addedAt = null)
        }
        refreshCount()
    }

    override fun insertWord(word: Word): Long {
        val id = nextId++
        words.add(word.copy(id = id))
        return id
    }

    override fun searchWords(query: String): List<Word> {
        val q = query.lowercase()
        return words.filter {
            it.original.lowercase().contains(q) || it.translation.lowercase().contains(q)
        }
    }

    override fun searchDictionaryWords(query: String): List<Word> {
        val q = query.lowercase()
        return words.filter {
            it.isInDictionary && (it.original.lowercase().contains(q) || it.translation.lowercase().contains(q))
        }
    }

    override fun findByOriginal(original: String): Word? {
        return words.find { it.original.equals(original, ignoreCase = true) }
    }

    override fun observeDictionaryWordCount(): Flow<Long> = _wordCountTrigger

    override fun getWordCountBySource(source: String): Long {
        return words.count { it.source == source }.toLong()
    }

    override fun getDictionaryWordCountBySource(source: String): Long {
        return words.count { it.source == source && it.isInDictionary }.toLong()
    }

    override fun getWordsBySource(source: String): List<Word> {
        return words.filter { it.source == source }
    }

    override fun insertWordsInTransaction(words: List<Word>) {
        words.forEach { word ->
            this.words.add(word.copy(id = nextId++))
        }
        refreshCount()
    }

    override fun getDictionaryWordsBySource(source: String): List<Word> {
        return words.filter { it.source == source && it.isInDictionary }
            .sortedByDescending { it.addedAt }
    }

    override fun prepopulateIfEmpty() {
        if (words.isEmpty()) {
            val seedWords = HardcodedWordRepository().getAllWords()
            seedWords.forEach { word ->
                words.add(word.copy(id = nextId++))
            }
        }
        refreshCount()
    }

    // --- Clock ---

    override fun currentTimeMillis(): Long = clock()

    // --- Word Progress ---

    override fun upsertWordProgress(progress: WordProgress) {
        wordProgressMap[Pair(progress.wordId, progress.mode)] = progress
    }

    override fun getWordProgress(wordId: Long, mode: StudyMode): WordProgress? {
        return wordProgressMap[Pair(wordId, mode)]
    }

    override fun getAllProgressForWord(wordId: Long): List<WordProgress> {
        return wordProgressMap.values.filter { it.wordId == wordId }
    }

    override fun getWordsNeedingReview(now: Long): List<WordProgress> {
        return wordProgressMap.values
            .filter { it.nextReviewAt in 1..now }
            .sortedBy { it.nextReviewAt }
    }

    override fun getProgressByMasteryLevel(): Map<Int, Long> {
        return wordProgressMap.values.groupBy { it.masteryLevel }
            .mapValues { it.value.size.toLong() }
    }

    override fun getAllWordProgress(): List<WordProgress> {
        return wordProgressMap.values.toList()
    }

    override fun getMinMasteryPerWord(): Map<Long, Int> {
        return wordProgressMap.values.groupBy { it.wordId }
            .mapValues { entry -> entry.value.minOf { it.masteryLevel } }
    }

    // --- Study Sessions ---

    override fun insertStudySession(session: StudySession) {
        sessions.add(session.copy(id = nextSessionId++))
    }

    override fun getRecentSessions(limit: Long): List<StudySession> {
        return sessions.sortedByDescending { it.finishedAt }.take(limit.toInt())
    }

    override fun getSessionCount(): Long = sessions.size.toLong()

    // --- Daily Activity ---

    override fun upsertDailyActivity(
        date: String,
        wordsStudied: Int,
        sessionsCount: Int,
        correctCount: Int,
        totalCount: Int,
    ) {
        val existing = dailyActivityMap[date]
        dailyActivityMap[date] = if (existing != null) {
            existing.copy(
                wordsStudied = existing.wordsStudied + wordsStudied,
                sessionsCount = existing.sessionsCount + sessionsCount,
                correctCount = existing.correctCount + correctCount,
                totalCount = existing.totalCount + totalCount,
            )
        } else {
            DailyActivity(date, wordsStudied, sessionsCount, correctCount, totalCount)
        }
    }

    override fun getDailyActivityRange(startDate: String, endDate: String): List<DailyActivity> {
        return dailyActivityMap.values
            .filter { it.date in startDate..endDate }
            .sortedBy { it.date }
    }

    override fun getAllDailyActivity(): List<DailyActivity> {
        return dailyActivityMap.values.sortedByDescending { it.date }
    }

    // --- Achievements ---

    override fun insertAchievement(type: String, unlockedAt: Long): Boolean {
        if (achievementMap.containsKey(type)) return false
        achievementMap[type] = Achievement(
            id = nextAchievementId++,
            type = try { AchievementType.valueOf(type) } catch (_: Exception) { AchievementType.FIRST_STEPS },
            unlockedAt = unlockedAt,
        )
        return true
    }

    override fun getAchievement(type: String): Achievement? {
        return achievementMap[type]
    }

    override fun getAllAchievements(): List<Achievement> {
        return achievementMap.values.sortedByDescending { it.unlockedAt }
    }

    // --- User Settings ---

    override fun getSetting(key: String): String? = settingsMap[key]

    override fun setSetting(key: String, value: String) {
        settingsMap[key] = value
    }
}
