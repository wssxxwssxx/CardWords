package com.example.cardwords.data.local

import com.example.cardwords.data.model.Achievement
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.study.StudyMode
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    fun getAllWords(): List<Word>
    fun getWordById(id: Long): Word?
    fun getDictionaryWords(): List<Word>
    fun getDictionaryWordCount(): Long
    fun addToDictionary(wordId: Long)
    fun removeFromDictionary(wordId: Long)
    fun insertWord(word: Word): Long
    fun searchWords(query: String): List<Word>
    fun searchDictionaryWords(query: String): List<Word>
    fun findByOriginal(original: String): Word?
    fun observeDictionaryWordCount(): Flow<Long>
    fun prepopulateIfEmpty()
    fun getWordCountBySource(source: String): Long
    fun getDictionaryWordCountBySource(source: String): Long
    fun getWordsBySource(source: String): List<Word>
    fun insertWordsInTransaction(words: List<Word>)
    fun getDictionaryWordsBySource(source: String): List<Word>

    // --- Clock ---
    fun currentTimeMillis(): Long

    // --- Word Progress ---
    fun upsertWordProgress(progress: WordProgress)
    fun getWordProgress(wordId: Long, mode: StudyMode): WordProgress?
    fun getAllProgressForWord(wordId: Long): List<WordProgress>
    fun getWordsNeedingReview(now: Long): List<WordProgress>
    fun getProgressByMasteryLevel(): Map<Int, Long>
    fun getAllWordProgress(): List<WordProgress>
    fun getMinMasteryPerWord(): Map<Long, Int>

    // --- Study Sessions ---
    fun insertStudySession(session: StudySession)
    fun getRecentSessions(limit: Long): List<StudySession>
    fun getSessionCount(): Long

    // --- Daily Activity ---
    fun upsertDailyActivity(date: String, wordsStudied: Int, sessionsCount: Int, correctCount: Int, totalCount: Int)
    fun getDailyActivityRange(startDate: String, endDate: String): List<DailyActivity>
    fun getAllDailyActivity(): List<DailyActivity>

    // --- Achievements ---
    fun insertAchievement(type: String, unlockedAt: Long): Boolean
    fun getAchievement(type: String): Achievement?
    fun getAllAchievements(): List<Achievement>

    // --- User Settings ---
    fun getSetting(key: String): String?
    fun setSetting(key: String, value: String)
    fun getSettingOrDefault(key: String, default: String): String =
        getSetting(key) ?: default
}
