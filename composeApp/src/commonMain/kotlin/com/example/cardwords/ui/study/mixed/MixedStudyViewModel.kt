package com.example.cardwords.ui.study.mixed

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.AchievementChecker
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.RewardGenerator
import com.example.cardwords.data.model.SessionReward
import com.example.cardwords.data.model.SmartSessionSelector
import com.example.cardwords.data.model.StreakManager
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.data.model.XpManager
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.util.DateUtil
import com.example.cardwords.util.FuzzyMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MixedQuestion(
    val word: Word,
    val mode: StudyMode,
    val mcOptions: List<String> = emptyList(),
    val mcCorrectIndex: Int = -1,
)

data class LetterTile(
    val id: Int,
    val char: Char,
    val isPlaced: Boolean = false,
    val answerSlotIndex: Int = -1,
    val scrambleIndex: Int,
)

sealed interface MixedAnswerState {
    data object Unanswered : MixedAnswerState
    data class McAnswered(val selectedIndex: Int, val isCorrect: Boolean) : MixedAnswerState
    data class TypingCorrect(val answer: String) : MixedAnswerState
    data class TypingIncorrect(val userAnswer: String, val correctAnswer: String) : MixedAnswerState
    data class AssemblyCorrect(val assembledWord: String) : MixedAnswerState
    data class AssemblyIncorrect(val assembledWord: String, val correctAnswer: String) : MixedAnswerState
}

data class MixedStudyUiState(
    val questions: List<MixedQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answerState: MixedAnswerState = MixedAnswerState.Unanswered,
    val typingInput: String = "",
    val isFlipped: Boolean = false,
    val assemblyTiles: List<LetterTile> = emptyList(),
    val assemblyCorrectAnswer: String = "",
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val isFinished: Boolean = false,
    val isEmpty: Boolean = false,
    val newAchievements: List<AchievementType> = emptyList(),
    val sessionReward: SessionReward? = null,
) {
    val currentQuestion: MixedQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float get() = if (questions.isEmpty()) 0f else currentIndex.toFloat() / questions.size
    val totalCards: Int get() = questions.size
}

class MixedStudyViewModel(
    multipleChoice: Boolean,
    flashcard: Boolean,
    typing: Boolean,
    letterAssembly: Boolean,
    wordCount: Int = 0,
    private val wordSource: String = "",
    wordIds: String = "",
    isSmartSession: Boolean = false,
) : ViewModel() {

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(MixedStudyUiState())
    val uiState: StateFlow<MixedStudyUiState> = _uiState.asStateFlow()

    // --- Progress tracking ---
    private val sessionStartedAt = repository.currentTimeMillis()
    private val answerResults = mutableMapOf<Pair<Long, StudyMode>, Pair<Int, Int>>() // (correct, total)
    private var sessionPersisted = false

    init {
        val dictionaryWords = if (isSmartSession) {
            val smartResult = SmartSessionSelector.selectWords(repository)
            val smartIds = smartResult.wordIds.toSet()
            if (smartIds.isEmpty()) {
                repository.getDictionaryWords()
            } else {
                repository.getDictionaryWords().filter { it.id in smartIds }
            }
        } else if (wordIds.isNotEmpty()) {
            val ids = wordIds.split(",").mapNotNull { it.toLongOrNull() }.toSet()
            val filtered = repository.getDictionaryWords().filter { it.id in ids }
            filtered.ifEmpty { repository.getDictionaryWords() }
        } else {
            val allWords = if (wordSource.isEmpty()) {
                repository.getDictionaryWords()
            } else {
                repository.getDictionaryWordsBySource(wordSource)
            }

            if (wordCount > 0 && wordCount < allWords.size) {
                allWords.shuffled().take(wordCount)
            } else {
                allWords
            }
        }

        if (dictionaryWords.isEmpty()) {
            _uiState.update { it.copy(isEmpty = true) }
        } else {
            val selectedModes = buildList {
                if (multipleChoice) add(StudyMode.MULTIPLE_CHOICE)
                if (flashcard) add(StudyMode.FLASHCARD)
                if (typing) add(StudyMode.TYPING)
                if (letterAssembly) add(StudyMode.LETTER_ASSEMBLY)
            }

            val shuffledWords = dictionaryWords.shuffled()
            val allOriginals = repository.getAllWords().map { it.original }

            // Smart mode selection for smart sessions
            @Suppress("NAME_SHADOWING")
            val isSmartSession = isSmartSession || wordIds.isNotEmpty()
            val allProgress = if (isSmartSession) repository.getAllWordProgress() else emptyList()
            val progressByWord = allProgress.groupBy { it.wordId }
            val modeUsageCounts = mutableMapOf<StudyMode, Int>()

            val questions = shuffledWords.mapIndexed { index, word ->
                val mode = if (isSmartSession) {
                    pickSmartMode(word.id, selectedModes, progressByWord, modeUsageCounts)
                } else {
                    selectedModes[index % selectedModes.size]
                }
                if (mode == StudyMode.MULTIPLE_CHOICE) {
                    val distractors = allOriginals
                        .filter { it != word.original }
                        .shuffled()
                        .take(3)
                    val options = (distractors + word.original).shuffled()
                    MixedQuestion(
                        word = word,
                        mode = mode,
                        mcOptions = options,
                        mcCorrectIndex = options.indexOf(word.original),
                    )
                } else {
                    MixedQuestion(word = word, mode = mode)
                }
            }

            _uiState.update { it.copy(questions = questions) }
            refreshAssemblyState()
        }
    }

    // --- Progress recording ---

    private fun recordAnswer(wordId: Long, mode: StudyMode, correct: Boolean) {
        val key = Pair(wordId, mode)
        val prev = answerResults[key] ?: Pair(0, 0)
        answerResults[key] = Pair(
            prev.first + if (correct) 1 else 0,
            prev.second + 1,
        )
    }

    private fun persistSessionIfFinished() {
        if (sessionPersisted) return
        val state = _uiState.value
        if (!state.isFinished) return
        sessionPersisted = true

        val now = repository.currentTimeMillis()
        val todayStr = DateUtil.epochMillisToDateString(now)

        // 1. Update word progress for each (word, mode) pair
        val uniqueWords = mutableSetOf<Long>()
        answerResults.forEach { (key, result) ->
            val (wordId, mode) = key
            val (sessionCorrect, sessionTotal) = result
            uniqueWords.add(wordId)

            val existing = repository.getWordProgress(wordId, mode)
            val newCorrect = (existing?.correctCount ?: 0) + sessionCorrect
            val newTotal = (existing?.totalCount ?: 0) + sessionTotal
            val newLevel = MasteryLevels.compute(newCorrect, newTotal)
            val nextReview = now + MasteryLevels.intervalFor(newLevel)

            repository.upsertWordProgress(
                WordProgress(
                    wordId = wordId,
                    mode = mode,
                    correctCount = newCorrect,
                    totalCount = newTotal,
                    masteryLevel = newLevel,
                    lastStudiedAt = now,
                    nextReviewAt = nextReview,
                )
            )
        }

        // 2. Save study session
        val modesUsed = state.questions.map { it.mode }.distinct().joinToString(",") { it.name }
        repository.insertStudySession(
            StudySession(
                id = 0,
                startedAt = sessionStartedAt,
                finishedAt = now,
                correctCount = state.correctCount,
                totalCount = state.totalCards,
                modesUsed = modesUsed,
                wordSource = wordSource,
            )
        )

        // 3. Update daily activity
        repository.upsertDailyActivity(
            date = todayStr,
            wordsStudied = uniqueWords.size,
            sessionsCount = 1,
            correctCount = state.correctCount,
            totalCount = state.totalCards,
        )

        // 4. Compute streak for XP bonus
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()
        val freezeUsedDates = StreakManager.getFreezeUsedDates(repository, now)
        val currentStreak = StreakManager.computeStreakWithFreeze(activeDates, freezeUsedDates, now)

        // 5. Award XP
        val xpReward = XpManager.awardSessionXp(
            repository = repository,
            correctCount = state.correctCount,
            totalCount = state.totalCards,
            streakDays = currentStreak,
        )

        // 6. Award streak freeze for milestones
        StreakManager.awardFreezeForMilestone(repository, currentStreak)

        // 7. Generate session reward
        val accuracy = if (state.totalCards > 0) state.correctCount.toFloat() / state.totalCards else 0f
        val sessionReward = RewardGenerator.generateSessionReward(xpReward, accuracy, currentStreak)

        // 8. Check achievements
        val unlocked = AchievementChecker.checkAll(
            repository = repository,
            sessionCorrectCount = state.correctCount,
            sessionTotalCount = state.totalCards,
        )

        _uiState.update {
            it.copy(
                newAchievements = if (unlocked.isNotEmpty()) unlocked else it.newAchievements,
                sessionReward = sessionReward,
            )
        }
    }

    // --- Smart mode selection ---

    private fun pickSmartMode(
        wordId: Long,
        selectedModes: List<StudyMode>,
        progressByWord: Map<Long, List<WordProgress>>,
        modeUsageCounts: MutableMap<StudyMode, Int>,
    ): StudyMode {
        val wordProgress = progressByWord[wordId]
            ?.associateBy { it.mode }
            ?: emptyMap()

        // Score each mode: lower mastery = higher priority. No progress = -1 (highest)
        val modeScores = selectedModes.map { mode ->
            val level = wordProgress[mode]?.masteryLevel ?: -1
            mode to level
        }

        val minLevel = modeScores.minOf { it.second }
        val candidates = modeScores.filter { it.second == minLevel }.map { it.first }

        // Among candidates, pick the least used for variety
        val chosen = candidates.minByOrNull { modeUsageCounts[it] ?: 0 } ?: selectedModes.first()
        modeUsageCounts[chosen] = (modeUsageCounts[chosen] ?: 0) + 1
        return chosen
    }

    // --- Letter Assembly ---

    private fun generateAssemblyTiles(translation: String): List<LetterTile> {
        val chars = translation.toList()
        val shuffledIndices = chars.indices.toList().shuffled()
        return shuffledIndices.mapIndexed { scramblePos, originalCharIndex ->
            LetterTile(
                id = scramblePos,
                char = chars[originalCharIndex],
                scrambleIndex = scramblePos,
            )
        }
    }

    private fun refreshAssemblyState() {
        val question = _uiState.value.currentQuestion ?: return
        if (question.mode == StudyMode.LETTER_ASSEMBLY) {
            _uiState.update {
                it.copy(
                    assemblyTiles = generateAssemblyTiles(question.word.original),
                    assemblyCorrectAnswer = question.word.original,
                )
            }
        }
    }

    fun placeLetter(tileId: Int) {
        val state = _uiState.value
        if (state.answerState != MixedAnswerState.Unanswered) return

        val tiles = state.assemblyTiles
        val tile = tiles.find { it.id == tileId } ?: return
        if (tile.isPlaced) return

        val totalSlots = state.assemblyCorrectAnswer.length
        val occupiedSlots = tiles.filter { it.isPlaced }.map { it.answerSlotIndex }.toSet()
        val nextSlot = (0 until totalSlots).firstOrNull { it !in occupiedSlots } ?: return

        val updatedTiles = tiles.map {
            if (it.id == tileId) it.copy(isPlaced = true, answerSlotIndex = nextSlot)
            else it
        }

        _uiState.update { it.copy(assemblyTiles = updatedTiles) }

        if (updatedTiles.all { it.isPlaced }) {
            checkAssemblyAnswer(updatedTiles)
        }
    }

    fun removeLetter(tileId: Int) {
        val state = _uiState.value
        if (state.answerState != MixedAnswerState.Unanswered) return

        val tiles = state.assemblyTiles
        val tile = tiles.find { it.id == tileId } ?: return
        if (!tile.isPlaced) return

        val removedSlot = tile.answerSlotIndex

        val updatedTiles = tiles.map {
            when {
                it.id == tileId -> it.copy(isPlaced = false, answerSlotIndex = -1)
                it.isPlaced && it.answerSlotIndex > removedSlot ->
                    it.copy(answerSlotIndex = it.answerSlotIndex - 1)
                else -> it
            }
        }

        _uiState.update { it.copy(assemblyTiles = updatedTiles) }
    }

    private fun checkAssemblyAnswer(tiles: List<LetterTile>) {
        val currentQuestion = _uiState.value.currentQuestion

        val assembled = tiles
            .filter { it.isPlaced }
            .sortedBy { it.answerSlotIndex }
            .map { it.char }
            .joinToString("")

        val correctAnswer = _uiState.value.assemblyCorrectAnswer
        val isCorrect = assembled.equals(correctAnswer, ignoreCase = true)

        if (currentQuestion != null) {
            recordAnswer(currentQuestion.word.id, currentQuestion.mode, isCorrect)
        }

        _uiState.update {
            it.copy(
                answerState = if (isCorrect) {
                    MixedAnswerState.AssemblyCorrect(assembled)
                } else {
                    MixedAnswerState.AssemblyIncorrect(assembled, correctAnswer)
                },
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                incorrectCount = if (!isCorrect) it.incorrectCount + 1 else it.incorrectCount,
            )
        }
    }

    // --- Multiple choice ---

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.answerState != MixedAnswerState.Unanswered) return
        val question = state.currentQuestion ?: return

        val isCorrect = index == question.mcCorrectIndex
        recordAnswer(question.word.id, question.mode, isCorrect)

        _uiState.update {
            it.copy(
                answerState = MixedAnswerState.McAnswered(index, isCorrect),
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                incorrectCount = if (!isCorrect) it.incorrectCount + 1 else it.incorrectCount,
            )
        }
    }

    // --- Flashcard ---

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun markKnew() {
        advanceWithResult(correct = true)
    }

    fun markDidNotKnow() {
        advanceWithResult(correct = false)
    }

    private fun advanceWithResult(correct: Boolean) {
        val currentQuestion = _uiState.value.currentQuestion
        if (currentQuestion != null) {
            recordAnswer(currentQuestion.word.id, currentQuestion.mode, correct)
        }

        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            val newCorrect = if (correct) state.correctCount + 1 else state.correctCount
            val newIncorrect = if (!correct) state.incorrectCount + 1 else state.incorrectCount
            if (nextIndex >= state.questions.size) {
                state.copy(
                    correctCount = newCorrect,
                    incorrectCount = newIncorrect,
                    isFinished = true,
                )
            } else {
                state.copy(
                    correctCount = newCorrect,
                    incorrectCount = newIncorrect,
                    currentIndex = nextIndex,
                    answerState = MixedAnswerState.Unanswered,
                    isFlipped = false,
                    typingInput = "",
                    assemblyTiles = emptyList(),
                    assemblyCorrectAnswer = "",
                )
            }
        }
        refreshAssemblyState()
        persistSessionIfFinished()
    }

    // --- Typing ---

    fun updateInput(text: String) {
        _uiState.update { it.copy(typingInput = text) }
    }

    fun submitTypingAnswer() {
        val state = _uiState.value
        if (state.answerState != MixedAnswerState.Unanswered) return
        val question = state.currentQuestion ?: return
        val word = question.word

        val userAnswer = state.typingInput.trim()
        val isCorrect = FuzzyMatcher.isCloseEnough(userAnswer, word.original)

        recordAnswer(word.id, question.mode, isCorrect)

        _uiState.update {
            it.copy(
                answerState = if (isCorrect) {
                    MixedAnswerState.TypingCorrect(userAnswer)
                } else {
                    MixedAnswerState.TypingIncorrect(userAnswer, word.original)
                },
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                incorrectCount = if (!isCorrect) it.incorrectCount + 1 else it.incorrectCount,
            )
        }
    }

    fun skip() {
        advanceWithResult(correct = false)
    }

    // --- Common next (for MC, Typing, Assembly after feedback) ---

    fun nextCard() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(isFinished = true)
            } else {
                state.copy(
                    currentIndex = nextIndex,
                    answerState = MixedAnswerState.Unanswered,
                    isFlipped = false,
                    typingInput = "",
                    assemblyTiles = emptyList(),
                    assemblyCorrectAnswer = "",
                )
            }
        }
        refreshAssemblyState()
        persistSessionIfFinished()
    }
}
