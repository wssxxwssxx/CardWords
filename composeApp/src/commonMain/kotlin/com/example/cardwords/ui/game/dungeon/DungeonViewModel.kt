package com.example.cardwords.ui.game.dungeon

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StreakManager
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.data.model.XpManager
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DungeonViewModel : ViewModel() {

    private companion object {
        const val KEY_HIGHEST_FLOOR = "dungeon_highest_floor"
        const val KEY_TOTAL_RUNS = "dungeon_total_runs"
        const val KEY_TOTAL_CLEARS = "dungeon_total_clears"
    }

    private val repository = AppModule.databaseRepository

    private val _uiState = MutableStateFlow(DungeonUiState())
    val uiState = _uiState.asStateFlow()

    private var allWords = repository.getDictionaryWords()
    private var masteryMap = repository.getMinMasteryPerWord()

    init {
        val highestFloor = repository.getSetting(KEY_HIGHEST_FLOOR)?.toIntOrNull() ?: 0
        val totalRuns = repository.getSetting(KEY_TOTAL_RUNS)?.toIntOrNull() ?: 0

        if (allWords.size < DungeonConstants.MIN_WORDS) {
            _uiState.value = DungeonUiState(isEmpty = true, highestFloor = highestFloor, totalRuns = totalRuns)
        } else {
            _uiState.value = DungeonUiState(highestFloor = highestFloor, totalRuns = totalRuns)
        }
    }

    fun startRun() {
        allWords = repository.getDictionaryWords()
        masteryMap = repository.getMinMasteryPerWord()
        val now = repository.currentTimeMillis()
        val state = DungeonGameEngine.initRun(allWords, masteryMap, now)
        _uiState.value = state.copy(
            highestFloor = _uiState.value.highestFloor,
            totalRuns = _uiState.value.totalRuns,
        )
    }

    fun beginCombat() {
        _uiState.update { DungeonGameEngine.startCombat(it) }
    }

    fun selectCard(index: Int) {
        _uiState.update { DungeonGameEngine.selectCard(it, index) }
    }

    fun onInputChanged(input: String) {
        _uiState.update { DungeonGameEngine.updateInput(it, input) }
    }

    fun submitAnswer() {
        _uiState.update { DungeonGameEngine.submitAnswer(it) }
    }

    fun clearCombatResult() {
        _uiState.update { state ->
            val combat = state.combat ?: return@update state
            state.copy(combat = combat.copy(lastResult = null))
        }
    }

    fun generateRewards() {
        _uiState.update { DungeonGameEngine.generateRewards(it, allWords, masteryMap) }
    }

    fun selectReward(index: Int) {
        _uiState.update { DungeonGameEngine.selectReward(it, index) }
    }

    fun removeCard(index: Int) {
        _uiState.update { DungeonGameEngine.removeCardFromDeck(it, index) }
    }

    fun skipReward() {
        _uiState.update { DungeonGameEngine.skipReward(it) }
    }

    fun onRunEnd() {
        val state = _uiState.value
        val now = repository.currentTimeMillis()

        // Update word mastery
        val wordCorrectMap = mutableMapOf<Long, Pair<Int, Int>>() // wordId -> (correct, total)
        for (result in state.wordResults) {
            val (c, t) = wordCorrectMap.getOrDefault(result.wordId, Pair(0, 0))
            wordCorrectMap[result.wordId] = Pair(
                c + if (result.correct) 1 else 0,
                t + 1,
            )
        }

        for ((wordId, counts) in wordCorrectMap) {
            val existing = repository.getWordProgress(wordId, StudyMode.TYPING)
            val newCorrect = (existing?.correctCount ?: 0) + counts.first
            val newTotal = (existing?.totalCount ?: 0) + counts.second
            val newMastery = MasteryLevels.compute(newCorrect, newTotal)
            val nextReview = now + MasteryLevels.intervalFor(newMastery)
            repository.upsertWordProgress(
                WordProgress(
                    wordId = wordId,
                    mode = StudyMode.TYPING,
                    correctCount = newCorrect,
                    totalCount = newTotal,
                    masteryLevel = newMastery,
                    lastStudiedAt = now,
                    nextReviewAt = nextReview,
                ),
            )
        }

        // Study session
        repository.insertStudySession(
            StudySession(
                id = 0,
                startedAt = state.runStartedAt,
                finishedAt = now,
                correctCount = state.totalCorrect,
                totalCount = state.totalAttempts,
                modesUsed = "dungeon",
                wordSource = "dungeon",
            ),
        )

        // Daily activity
        val todayStr = DateUtil.epochMillisToDateString(now)
        val wordsPlayed = state.wordResults.map { it.wordId }.distinct().size
        repository.upsertDailyActivity(todayStr, wordsPlayed, 1, state.totalCorrect, state.totalAttempts)

        // XP
        val allActivity = repository.getAllDailyActivity()
        val activeDates = allActivity.map { it.date }.toSet()
        val freezeUsedDates = StreakManager.getFreezeUsedDates(repository, now)
        val streakDays = StreakManager.computeStreakWithFreeze(activeDates, freezeUsedDates, now)
        val xpReward = XpManager.awardSessionXp(repository, state.totalCorrect, state.totalAttempts, streakDays)

        // Persist meta-progression
        val prevHighest = repository.getSetting(KEY_HIGHEST_FLOOR)?.toIntOrNull() ?: 0
        if (state.floorsCleared > prevHighest) {
            repository.setSetting(KEY_HIGHEST_FLOOR, state.floorsCleared.toString())
        }
        val prevRuns = repository.getSetting(KEY_TOTAL_RUNS)?.toIntOrNull() ?: 0
        repository.setSetting(KEY_TOTAL_RUNS, (prevRuns + 1).toString())

        if (state.phase == DungeonPhase.VICTORY) {
            val prevClears = repository.getSetting(KEY_TOTAL_CLEARS)?.toIntOrNull() ?: 0
            repository.setSetting(KEY_TOTAL_CLEARS, (prevClears + 1).toString())
        }

        _uiState.update { it.copy(xpEarned = xpReward.totalXp) }
    }
}
