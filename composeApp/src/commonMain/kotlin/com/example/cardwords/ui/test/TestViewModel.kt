package com.example.cardwords.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardwords.data.remote.SubmitAnswer
import com.example.cardwords.data.remote.TestQuestion
import com.example.cardwords.data.remote.TestResultResponse
import com.example.cardwords.di.AppModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestPhase { LOADING, QUESTION, ANSWERED, SUBMITTING, RESULTS, ERROR }

data class TestUiState(
    val phase: TestPhase = TestPhase.LOADING,
    val questions: List<TestQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val answers: Map<String, String> = emptyMap(),
    val sessionId: String = "",
    val result: TestResultResponse? = null,
    val error: String? = null,
    val selectedWordIndices: Set<Int> = emptySet(),
    val selectedPhrase: String? = null,
    val phraseTranslation: String? = null,
    val wordAdded: Boolean = false,
    // Card id that was created on the server just to fetch a translation
    // (the `createCard` API endpoint both translates and stores). If the
    // user does NOT explicitly add the word via the "+" button, we remove
    // this card from the server before moving on — otherwise every tapped
    // word would silently pollute the user's dictionary.
    val pendingServerCardId: String? = null,
) {
    val currentQuestion: TestQuestion? get() = questions.getOrNull(currentIndex)
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.size - 1
}

class TestViewModel : ViewModel() {
    private val apiClient = AppModule.cardWordsApiClient
    private val authManager = AppModule.authManager

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private var wordTapJob: Job? = null

    init {
        startTest()
    }

    fun startTest() {
        val token = authManager.getToken() ?: run {
            _uiState.update { it.copy(phase = TestPhase.ERROR, error = "Не авторизован") }
            return
        }

        _uiState.value = TestUiState(phase = TestPhase.LOADING)

        viewModelScope.launch {
            apiClient.startTest(token).fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            phase = TestPhase.QUESTION,
                            questions = session.questions,
                            sessionId = session.testSessionId,
                        )
                    }
                },
                onFailure = { e ->
                    val raw = e.message.orEmpty()
                    val msg = when {
                        raw.contains("401") -> "Сессия истекла, войдите заново"
                        raw.contains("402") -> "Лимит тестов исчерпан (Free план)"
                        raw.contains("429") -> "Слишком много запросов — подождите минуту"
                        raw.contains("500") -> "Сервер временно недоступен"
                        raw.contains("502") || raw.contains("504") ->
                            "Сервер не отвечает, попробуйте ещё раз"
                        raw.contains("503") -> "Сервер генерации недоступен"
                        raw.contains("ConnectException", ignoreCase = true) ||
                        raw.contains("UnresolvedAddress", ignoreCase = true) ||
                        raw.contains("Unable to resolve host", ignoreCase = true) ->
                            "Нет подключения к интернету"
                        raw.contains("timeout", ignoreCase = true) ||
                        raw.contains("HttpRequestTimeout", ignoreCase = true) ->
                            "Превышено время ожидания"
                        raw.contains("SerializationException") ||
                        raw.contains("JsonDecodingException") ||
                        raw.contains("NoTransformationFound") ->
                            "Ошибка формата ответа сервера"
                        else -> "Ошибка загрузки теста"
                    }
                    _uiState.update {
                        it.copy(phase = TestPhase.ERROR, error = msg)
                    }
                },
            )
        }
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.phase != TestPhase.QUESTION) return
        val question = state.currentQuestion ?: return
        if (index !in question.options.indices) return

        _uiState.update {
            it.copy(
                phase = TestPhase.ANSWERED,
                selectedAnswerIndex = index,
                answers = it.answers + (question.id to question.options[index]),
            )
        }
    }

    fun nextQuestion() {
        // Clean up any orphan server card from the previous question
        purgePendingServerCard()

        val state = _uiState.value
        if (state.isLastQuestion) {
            submitResults()
        } else {
            _uiState.update {
                it.copy(
                    phase = TestPhase.QUESTION,
                    currentIndex = it.currentIndex + 1,
                    selectedAnswerIndex = null,
                    selectedWordIndices = emptySet(),
                    selectedPhrase = null,
                    phraseTranslation = null,
                    pendingServerCardId = null,
                    wordAdded = false,
                )
            }
        }
    }

    private fun submitResults() {
        val state = _uiState.value
        val token = authManager.getToken() ?: return

        _uiState.update { it.copy(phase = TestPhase.SUBMITTING) }

        viewModelScope.launch {
            val answers = state.answers.map { (qId, answer) -> SubmitAnswer(qId, answer) }
            apiClient.submitTest(token, state.sessionId, answers).fold(
                onSuccess = { testResult ->
                    _uiState.update { it.copy(phase = TestPhase.RESULTS, result = testResult) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(phase = TestPhase.ERROR, error = "Ошибка отправки результатов")
                    }
                },
            )
        }
    }

    /** Single tap — select one word, or extend if already have selection */
    fun onWordTapped(index: Int, sentenceWords: List<String>) {
        val current = _uiState.value.selectedWordIndices

        // If already have selection and tapping a different word → extend selection
        if (current.isNotEmpty() && index !in current) {
            onWordLongPressed(index, sentenceWords)
            return
        }

        // If tapping the same word → dismiss
        if (current == setOf(index)) {
            dismissTooltip()
            return
        }

        wordTapJob?.cancel()
        // Remove the previously-fetched card from the server before overwriting selection
        purgePendingServerCard()
        val question = _uiState.value.currentQuestion ?: return

        _uiState.update {
            it.copy(
                selectedWordIndices = setOf(index),
                selectedPhrase = null,
                phraseTranslation = "...",
                wordAdded = false,
                pendingServerCardId = null,
            )
        }

        val cleanWord = sentenceWords[index].replace(Regex("[^a-zA-Z']"), "").lowercase()
        if (cleanWord.isBlank()) { dismissTooltip(); return }

        if (cleanWord == question.correctAnswer.lowercase()) {
            _uiState.update {
                it.copy(selectedPhrase = cleanWord, phraseTranslation = question.translationHint)
            }
            return
        }

        fetchTranslation(cleanWord)
    }

    /** Long press — add word to existing selection to build a phrase */
    fun onWordLongPressed(index: Int, sentenceWords: List<String>) {
        wordTapJob?.cancel()
        // Extending selection also discards the previous orphan card
        purgePendingServerCard()
        val current = _uiState.value.selectedWordIndices
        if (current.isEmpty()) {
            onWordTapped(index, sentenceWords)
            return
        }

        val newIndices = current + index
        val sortedIndices = newIndices.sorted()
        // Fill gaps — select all words between min and max
        val range = (sortedIndices.first()..sortedIndices.last()).toSet()

        val phrase = range
            .map { sentenceWords[it].replace(Regex("[^a-zA-Z']"), "").lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

        _uiState.update {
            it.copy(
                selectedWordIndices = range,
                selectedPhrase = phrase,
                phraseTranslation = "...",
                wordAdded = false,
                pendingServerCardId = null,
            )
        }

        if (phrase.isNotBlank()) fetchTranslation(phrase)
    }

    private fun fetchTranslation(text: String) {
        wordTapJob = viewModelScope.launch {
            val token = authManager.getToken() ?: return@launch
            apiClient.createCard(token, text).fold(
                onSuccess = { card ->
                    _uiState.update {
                        if (it.selectedPhrase == text || it.phraseTranslation == "...") {
                            it.copy(
                                selectedPhrase = text,
                                phraseTranslation = card.wordTranslation,
                                pendingServerCardId = card.id,
                            )
                        } else {
                            // Selection already changed — clean up this orphan card
                            cleanupServerCard(token, card.id)
                            it
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        if (it.phraseTranslation == "...") {
                            it.copy(selectedPhrase = text, phraseTranslation = text)
                        } else it
                    }
                },
            )
        }
    }

    /** Deletes a server card fire-and-forget. Runs on app-level scope so it
     *  survives viewModel clearing. */
    private fun cleanupServerCard(token: String, cardId: String) {
        AppModule.syncScope.launch {
            apiClient.deleteCard(token, cardId)
        }
    }

    /** Remove the server-side card that was auto-created during translation
     *  if the user never confirmed it via the "+" button. */
    private fun purgePendingServerCard() {
        val state = _uiState.value
        val cardId = state.pendingServerCardId ?: return
        if (state.wordAdded) return // user confirmed — keep it
        val token = authManager.getToken() ?: return
        cleanupServerCard(token, cardId)
    }

    fun addSelectedToDictionary() {
        val state = _uiState.value
        val phrase = state.selectedPhrase ?: return
        val translation = state.phraseTranslation ?: return
        if (phrase.isBlank() || translation == "...") return

        val repository = AppModule.databaseRepository
        val existing = repository.findByOriginal(phrase)
        val wordId = if (existing != null) {
            existing.id
        } else {
            repository.insertWord(
                com.example.cardwords.data.model.Word(
                    id = 0,
                    original = phrase,
                    translation = translation,
                    transcription = "",
                    source = "api",
                )
            )
        }
        repository.addToDictionary(wordId)
        _uiState.update { it.copy(wordAdded = true) }
    }

    fun dismissTooltip() {
        purgePendingServerCard()
        _uiState.update {
            it.copy(
                selectedWordIndices = emptySet(),
                selectedPhrase = null,
                phraseTranslation = null,
                pendingServerCardId = null,
                wordAdded = false,
            )
        }
    }
}
