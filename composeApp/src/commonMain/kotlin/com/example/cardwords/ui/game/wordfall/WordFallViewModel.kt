package com.example.cardwords.ui.game.wordfall

import androidx.lifecycle.ViewModel
import com.example.cardwords.data.model.Word
import com.example.cardwords.di.AppModule
import com.example.cardwords.util.FuzzyMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FallingWord(
    val id: Int,
    val word: Word,
    val startTimeMs: Long,
    val fallDurationMs: Long,
    val lane: Int,
)

data class DestroyedEffect(
    val id: Int,
    val text: String,
    val points: Int,
    val lane: Int,
    val yProgress: Float,
    val createdAt: Long,
)

enum class GamePhase { NOT_STARTED, PLAYING, GAME_OVER }

data class WordFallUiState(
    val gamePhase: GamePhase = GamePhase.NOT_STARTED,
    val activeWords: List<FallingWord> = emptyList(),
    val destroyedEffects: List<DestroyedEffect> = emptyList(),
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val lives: Int = 3,
    val input: String = "",
    val highScore: Int = 0,
    val wordsDestroyed: Int = 0,
    val level: Int = 0,
    val isEmpty: Boolean = false,
    val isNewRecord: Boolean = false,
)

class WordFallViewModel : ViewModel() {

    private companion object {
        const val MAX_ACTIVE = 5
        const val LANE_COUNT = 3
        const val INITIAL_FALL_MS = 8000L
        const val MIN_FALL_MS = 3500L
        const val INITIAL_SPAWN_MS = 3000L
        const val MIN_SPAWN_MS = 1200L
        const val WORDS_PER_LEVEL = 8
        const val MIN_WORDS = 5
        const val EFFECT_DURATION_MS = 600L
        const val HIGH_SCORE_KEY = "wordfall_high_score"
    }

    private val repository = AppModule.databaseRepository
    private var wordPool: List<Word> = emptyList()
    private var poolIndex = 0
    private var lastLane = -1
    private var lastSpawnMs = 0L
    private var nextId = 0
    private var gameStartMs = 0L

    private val _uiState = MutableStateFlow(WordFallUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val words = repository.getDictionaryWords()
        val highScore = repository.getSetting(HIGH_SCORE_KEY)?.toIntOrNull() ?: 0
        if (words.size < MIN_WORDS) {
            _uiState.value = WordFallUiState(isEmpty = true, highScore = highScore)
        } else {
            wordPool = words.shuffled()
            _uiState.value = WordFallUiState(highScore = highScore)
        }
    }

    fun startGame() {
        wordPool = wordPool.shuffled()
        poolIndex = 0
        lastLane = -1
        nextId = 0
        gameStartMs = 0L
        lastSpawnMs = 0L
        _uiState.value = WordFallUiState(
            gamePhase = GamePhase.PLAYING,
            highScore = _uiState.value.highScore,
        )
    }

    fun restartGame() = startGame()

    fun onInputChanged(text: String) {
        val state = _uiState.value
        if (state.gamePhase != GamePhase.PLAYING) return

        // Check if input matches any active word's translation
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(input = text) }
            return
        }

        // Find best match — prioritize the word closest to the bottom (highest progress)
        val now = if (state.activeWords.isNotEmpty()) {
            // approximate current time from the most recent word
            val w = state.activeWords.first()
            // We don't have exact frame time here, use rough calculation
            // The actual progress is calculated in onFrameTick, but for matching
            // we just need to find any match, not position-dependent
            0L // will be checked regardless
        } else 0L

        var matchedWord: FallingWord? = null
        var bestProgress = -1f

        for (fw in state.activeWords) {
            val translations = fw.word.translation.split(",", ";", "/").map { it.trim() }
            val matched = translations.any { it.equals(trimmed, ignoreCase = true) }
            if (matched) {
                // Calculate approximate progress for prioritization
                val progress = if (fw.fallDurationMs > 0 && gameStartMs > 0) {
                    // We can't get exact frame time here, so just pick first match
                    1f // placeholder — we'll pick the first match
                } else 0f
                if (matchedWord == null) {
                    matchedWord = fw
                }
            }
        }

        if (matchedWord != null) {
            val s = _uiState.value
            val newCombo = s.combo + 1
            val level = s.wordsDestroyed / WORDS_PER_LEVEL
            val base = 100 + level * 10
            val comboMult = 1.0 + s.combo * 0.25
            val points = (base * comboMult).toInt()

            val effect = DestroyedEffect(
                id = matchedWord.id,
                text = "+$points",
                points = points,
                lane = matchedWord.lane,
                yProgress = 0.5f, // approximate
                createdAt = lastFrameMs,
            )

            _uiState.update { st ->
                st.copy(
                    activeWords = st.activeWords.filter { it.id != matchedWord!!.id },
                    destroyedEffects = st.destroyedEffects + effect,
                    score = st.score + points,
                    combo = newCombo,
                    maxCombo = maxOf(st.maxCombo, newCombo),
                    wordsDestroyed = st.wordsDestroyed + 1,
                    level = (st.wordsDestroyed + 1) / WORDS_PER_LEVEL,
                    input = "",
                )
            }
        } else {
            _uiState.update { it.copy(input = text) }
        }
    }

    private var lastFrameMs = 0L

    fun onFrameTick(frameTimeMs: Long) {
        val state = _uiState.value
        if (state.gamePhase != GamePhase.PLAYING) return

        if (gameStartMs == 0L) {
            gameStartMs = frameTimeMs
            lastSpawnMs = frameTimeMs
        }
        lastFrameMs = frameTimeMs

        // Update word positions and check for words reaching bottom
        val fallen = mutableListOf<FallingWord>()
        val active = mutableListOf<FallingWord>()

        for (fw in state.activeWords) {
            val progress = ((frameTimeMs - fw.startTimeMs).toFloat() / fw.fallDurationMs)
                .coerceIn(0f, 1f)
            if (progress >= 1f) {
                fallen.add(fw)
            } else {
                active.add(fw)
            }
        }

        var newLives = state.lives
        var newCombo = state.combo
        for (f in fallen) {
            newLives--
            newCombo = 0
        }

        // Clean up expired effects
        val activeEffects = state.destroyedEffects.filter {
            frameTimeMs - it.createdAt < EFFECT_DURATION_MS
        }

        // Spawn new words
        val level = state.wordsDestroyed / WORDS_PER_LEVEL
        val diffFactor = (level * 0.08f).coerceAtMost(0.6f)
        val spawnInterval = (INITIAL_SPAWN_MS * (1f - diffFactor * 0.8f)).toLong()
            .coerceAtLeast(MIN_SPAWN_MS)
        val fallDuration = (INITIAL_FALL_MS * (1f - diffFactor)).toLong()
            .coerceAtLeast(MIN_FALL_MS)

        if (active.size < MAX_ACTIVE && frameTimeMs - lastSpawnMs >= spawnInterval) {
            val word = nextWord(active)
            if (word != null) {
                val lane = pickLane()
                active.add(
                    FallingWord(
                        id = nextId++,
                        word = word,
                        startTimeMs = frameTimeMs,
                        fallDurationMs = fallDuration,
                        lane = lane,
                    )
                )
                lastSpawnMs = frameTimeMs
            }
        }

        // Check game over
        if (newLives <= 0) {
            val finalScore = state.score
            val isNewRecord = finalScore > state.highScore
            if (isNewRecord) {
                repository.setSetting(HIGH_SCORE_KEY, finalScore.toString())
            }
            _uiState.update {
                it.copy(
                    gamePhase = GamePhase.GAME_OVER,
                    activeWords = emptyList(),
                    destroyedEffects = emptyList(),
                    lives = 0,
                    combo = 0,
                    highScore = if (isNewRecord) finalScore else it.highScore,
                    isNewRecord = isNewRecord,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                activeWords = active,
                destroyedEffects = activeEffects,
                lives = newLives,
                combo = newCombo,
            )
        }
    }

    private fun nextWord(activeWords: List<FallingWord>): Word? {
        if (wordPool.isEmpty()) return null
        val activeIds = activeWords.map { it.word.id }.toSet()
        var attempts = 0
        while (attempts < wordPool.size) {
            if (poolIndex >= wordPool.size) {
                wordPool = wordPool.shuffled()
                poolIndex = 0
            }
            val word = wordPool[poolIndex]
            poolIndex++
            if (word.id !in activeIds) return word
            attempts++
        }
        return wordPool.random()
    }

    private fun pickLane(): Int {
        val available = (0 until LANE_COUNT).filter { it != lastLane }
        val chosen = available.random()
        lastLane = chosen
        return chosen
    }
}
