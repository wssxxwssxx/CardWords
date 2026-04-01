package com.example.cardwords.data.local

import app.cash.sqldelight.db.SqlDriver
import com.example.cardwords.data.model.Achievement
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.data.repository.HardcodedWordRepository
import com.example.cardwords.db.CardWordsDatabase
import com.example.cardwords.ui.study.StudyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SqlDelightDatabaseRepository(
    private val driver: SqlDriver,
    private val clock: () -> Long = { 0L },
) : DatabaseRepository {

    private val database = CardWordsDatabase(driver)
    private val queries = database.cardWordsQueries

    private val _wordCountTrigger = MutableStateFlow(0L)

    init {
        migrateIfNeeded()
    }

    private fun migrateIfNeeded() {
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS user_settings (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS word_progress (
                word_id INTEGER NOT NULL,
                mode TEXT NOT NULL,
                correct_count INTEGER NOT NULL DEFAULT 0,
                total_count INTEGER NOT NULL DEFAULT 0,
                mastery_level INTEGER NOT NULL DEFAULT 0,
                last_studied_at INTEGER NOT NULL DEFAULT 0,
                next_review_at INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (word_id, mode)
            )
        """.trimIndent(), 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_wp_next_review ON word_progress(next_review_at)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_wp_mastery ON word_progress(mastery_level)", 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS study_session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at INTEGER NOT NULL,
                finished_at INTEGER NOT NULL,
                correct_count INTEGER NOT NULL,
                total_count INTEGER NOT NULL,
                modes_used TEXT NOT NULL DEFAULT '',
                word_source TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS daily_activity (
                date TEXT NOT NULL PRIMARY KEY,
                words_studied INTEGER NOT NULL DEFAULT 0,
                sessions_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                total_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS achievement (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL UNIQUE,
                unlocked_at INTEGER NOT NULL
            )
        """.trimIndent(), 0)
    }

    private fun refreshCount() {
        _wordCountTrigger.value = queries.getWordCount().executeAsOne()
    }

    // --- Existing Word methods ---

    override fun getAllWords(): List<Word> {
        return queries.getAllWords().executeAsList().map { it.toWord() }
    }

    override fun getWordById(id: Long): Word? {
        return queries.getWordById(id).executeAsOneOrNull()?.toWord()
    }

    override fun getDictionaryWords(): List<Word> {
        return queries.getDictionaryWords().executeAsList().map { it.toWord() }
    }

    override fun getDictionaryWordCount(): Long {
        return queries.getWordCount().executeAsOne()
    }

    override fun addToDictionary(wordId: Long) {
        queries.addToDictionary(added_at = clock(), id = wordId)
        refreshCount()
    }

    override fun removeFromDictionary(wordId: Long) {
        queries.removeFromDictionary(wordId)
        refreshCount()
    }

    override fun insertWord(word: Word): Long {
        queries.insertWord(
            original = word.original,
            translation = word.translation,
            transcription = word.transcription,
            category = word.category,
            is_in_dictionary = if (word.isInDictionary) 1L else 0L,
            added_at = word.addedAt,
            source = word.source,
        )
        return queries.getAllWords().executeAsList().lastOrNull()?.id ?: 0L
    }

    override fun searchWords(query: String): List<Word> {
        val pattern = "%$query%"
        return queries.searchWords(pattern, pattern).executeAsList().map { it.toWord() }
    }

    override fun searchDictionaryWords(query: String): List<Word> {
        val pattern = "%$query%"
        return queries.searchDictionaryWords(pattern, pattern).executeAsList().map { it.toWord() }
    }

    override fun findByOriginal(original: String): Word? {
        return queries.findByOriginal(original).executeAsOneOrNull()?.toWord()
    }

    override fun observeDictionaryWordCount(): Flow<Long> = _wordCountTrigger

    override fun prepopulateIfEmpty() {
        val count = queries.getAllWordCount().executeAsOne()
        if (count == 0L) {
            val seedWords = HardcodedWordRepository().getAllWords()
            queries.transaction {
                seedWords.forEach { word ->
                    queries.insertWord(
                        original = word.original,
                        translation = word.translation,
                        transcription = word.transcription,
                        category = word.category,
                        is_in_dictionary = 0L,
                        added_at = null,
                        source = "hardcoded",
                    )
                }
            }
        }
        refreshCount()
    }

    override fun getWordCountBySource(source: String): Long {
        return queries.getWordCountBySource(source).executeAsOne()
    }

    override fun getDictionaryWordCountBySource(source: String): Long {
        return queries.getDictionaryWordCountBySource(source).executeAsOne()
    }

    override fun getWordsBySource(source: String): List<Word> {
        return queries.getWordsBySource(source).executeAsList().map { it.toWord() }
    }

    override fun insertWordsInTransaction(words: List<Word>) {
        queries.transaction {
            words.forEach { word ->
                queries.insertWord(
                    original = word.original,
                    translation = word.translation,
                    transcription = word.transcription,
                    category = word.category,
                    is_in_dictionary = if (word.isInDictionary) 1L else 0L,
                    added_at = word.addedAt,
                    source = word.source,
                )
            }
        }
        refreshCount()
    }

    override fun getDictionaryWordsBySource(source: String): List<Word> {
        return queries.getDictionaryWordsBySource(source).executeAsList().map { it.toWord() }
    }

    // --- Clock ---

    override fun currentTimeMillis(): Long = clock()

    // --- Word Progress ---

    override fun upsertWordProgress(progress: WordProgress) {
        queries.upsertWordProgress(
            word_id = progress.wordId,
            mode = progress.mode.name,
            correct_count = progress.correctCount.toLong(),
            total_count = progress.totalCount.toLong(),
            mastery_level = progress.masteryLevel.toLong(),
            last_studied_at = progress.lastStudiedAt,
            next_review_at = progress.nextReviewAt,
        )
    }

    override fun getWordProgress(wordId: Long, mode: StudyMode): WordProgress? {
        return queries.getWordProgressByMode(wordId, mode.name).executeAsOneOrNull()?.toWordProgress()
    }

    override fun getAllProgressForWord(wordId: Long): List<WordProgress> {
        return queries.getAllProgressForWord(wordId).executeAsList().map { it.toWordProgress() }
    }

    override fun getWordsNeedingReview(now: Long): List<WordProgress> {
        return queries.getWordsNeedingReview(now).executeAsList().map { it.toWordProgress() }
    }

    override fun getProgressByMasteryLevel(): Map<Int, Long> {
        return queries.getProgressByMasteryLevel().executeAsList().associate {
            it.mastery_level.toInt() to it.cnt
        }
    }

    override fun getAllWordProgress(): List<WordProgress> {
        return queries.getAllWordProgress().executeAsList().map { it.toWordProgress() }
    }

    override fun getMinMasteryPerWord(): Map<Long, Int> {
        return queries.getMinMasteryPerWord().executeAsList().associate {
            it.word_id to (it.min_level?.toInt() ?: 0)
        }
    }

    // --- Study Sessions ---

    override fun insertStudySession(session: StudySession) {
        queries.insertStudySession(
            started_at = session.startedAt,
            finished_at = session.finishedAt,
            correct_count = session.correctCount.toLong(),
            total_count = session.totalCount.toLong(),
            modes_used = session.modesUsed,
            word_source = session.wordSource,
        )
    }

    override fun getRecentSessions(limit: Long): List<StudySession> {
        return queries.getRecentSessions(limit).executeAsList().map { row ->
            StudySession(
                id = row.id,
                startedAt = row.started_at,
                finishedAt = row.finished_at,
                correctCount = row.correct_count.toInt(),
                totalCount = row.total_count.toInt(),
                modesUsed = row.modes_used,
                wordSource = row.word_source,
            )
        }
    }

    override fun getSessionCount(): Long {
        return queries.getSessionCount().executeAsOne()
    }

    // --- Daily Activity ---

    override fun upsertDailyActivity(
        date: String,
        wordsStudied: Int,
        sessionsCount: Int,
        correctCount: Int,
        totalCount: Int,
    ) {
        val existing = queries.getDailyActivity(date).executeAsOneOrNull()
        if (existing != null) {
            queries.insertDailyActivity(
                date = date,
                words_studied = existing.words_studied + wordsStudied,
                sessions_count = existing.sessions_count + sessionsCount,
                correct_count = existing.correct_count + correctCount,
                total_count = existing.total_count + totalCount,
            )
        } else {
            queries.insertDailyActivity(
                date = date,
                words_studied = wordsStudied.toLong(),
                sessions_count = sessionsCount.toLong(),
                correct_count = correctCount.toLong(),
                total_count = totalCount.toLong(),
            )
        }
    }

    override fun getDailyActivityRange(startDate: String, endDate: String): List<DailyActivity> {
        return queries.getDailyActivityRange(startDate, endDate).executeAsList().map { row ->
            DailyActivity(
                date = row.date,
                wordsStudied = row.words_studied.toInt(),
                sessionsCount = row.sessions_count.toInt(),
                correctCount = row.correct_count.toInt(),
                totalCount = row.total_count.toInt(),
            )
        }
    }

    override fun getAllDailyActivity(): List<DailyActivity> {
        return queries.getAllDailyActivity().executeAsList().map { row ->
            DailyActivity(
                date = row.date,
                wordsStudied = row.words_studied.toInt(),
                sessionsCount = row.sessions_count.toInt(),
                correctCount = row.correct_count.toInt(),
                totalCount = row.total_count.toInt(),
            )
        }
    }

    // --- Achievements ---

    override fun insertAchievement(type: String, unlockedAt: Long): Boolean {
        val existing = queries.getAchievement(type).executeAsOneOrNull()
        if (existing != null) return false
        queries.insertAchievement(type, unlockedAt)
        return true
    }

    override fun getAchievement(type: String): Achievement? {
        return queries.getAchievement(type).executeAsOneOrNull()?.toAchievement()
    }

    override fun getAllAchievements(): List<Achievement> {
        return queries.getAllAchievements().executeAsList().map { it.toAchievement() }
    }

    // --- User Settings ---

    override fun getSetting(key: String): String? {
        return queries.getSetting(key).executeAsOneOrNull()
    }

    override fun setSetting(key: String, value: String) {
        queries.setSetting(key, value)
    }

    // --- Mappers ---

    private fun com.example.cardwords.db.Word.toWord(): Word {
        return Word(
            id = id,
            original = original,
            translation = translation,
            transcription = transcription,
            category = category,
            isInDictionary = is_in_dictionary == 1L,
            addedAt = added_at,
            source = source,
        )
    }

    private fun com.example.cardwords.db.Word_progress.toWordProgress(): WordProgress {
        return WordProgress(
            wordId = word_id,
            mode = try { StudyMode.valueOf(mode) } catch (_: Exception) { StudyMode.MULTIPLE_CHOICE },
            correctCount = correct_count.toInt(),
            totalCount = total_count.toInt(),
            masteryLevel = mastery_level.toInt(),
            lastStudiedAt = last_studied_at,
            nextReviewAt = next_review_at,
        )
    }

    private fun com.example.cardwords.db.Achievement.toAchievement(): Achievement {
        return Achievement(
            id = id,
            type = try { AchievementType.valueOf(type) } catch (_: Exception) { AchievementType.FIRST_STEPS },
            unlockedAt = unlocked_at,
        )
    }
}
