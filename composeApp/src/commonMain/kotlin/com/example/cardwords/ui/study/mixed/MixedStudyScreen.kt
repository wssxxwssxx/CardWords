package com.example.cardwords.ui.study.mixed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.BonusType
import com.example.cardwords.data.model.SessionReward
import com.example.cardwords.data.model.Word
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.AmberLight
import com.example.cardwords.ui.theme.AmberMuted
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.GreenDark
import com.example.cardwords.ui.theme.Indigo
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextSecondary
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedStudyScreen(
    multipleChoice: Boolean,
    flashcard: Boolean,
    typing: Boolean,
    letterAssembly: Boolean,
    wordCount: Int = 0,
    wordSource: String = "",
    wordIds: String = "",
    isSmartSession: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToAddWords: () -> Unit,
) {
    val viewModel = remember {
        MixedStudyViewModel(multipleChoice, flashcard, typing, letterAssembly, wordCount, wordSource, wordIds, isSmartSession)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Обучение",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                uiState.isEmpty -> EmptyContent(onNavigateToAddWords)
                uiState.isFinished -> ResultsContent(
                    correctCount = uiState.correctCount,
                    totalCards = uiState.totalCards,
                    newAchievements = uiState.newAchievements,
                    sessionReward = uiState.sessionReward,
                    onFinish = onNavigateBack,
                )
                else -> StudyContent(
                    uiState = uiState,
                    onSelectMcAnswer = { viewModel.selectAnswer(it) },
                    onFlipCard = { viewModel.flipCard() },
                    onKnew = { viewModel.markKnew() },
                    onDidNotKnow = { viewModel.markDidNotKnow() },
                    onTypingInput = { viewModel.updateInput(it) },
                    onSubmitTyping = { viewModel.submitTypingAnswer() },
                    onPlaceLetter = { viewModel.placeLetter(it) },
                    onRemoveLetter = { viewModel.removeLetter(it) },
                    onNextCard = { viewModel.nextCard() },
                    onSkip = { viewModel.skip() },
                )
            }
        }
    }
}

@Composable
private fun StudyContent(
    uiState: MixedStudyUiState,
    onSelectMcAnswer: (Int) -> Unit,
    onFlipCard: () -> Unit,
    onKnew: () -> Unit,
    onDidNotKnow: () -> Unit,
    onTypingInput: (String) -> Unit,
    onSubmitTyping: () -> Unit,
    onPlaceLetter: (Int) -> Unit,
    onRemoveLetter: (Int) -> Unit,
    onNextCard: () -> Unit,
    onSkip: () -> Unit,
) {
    // Progress
    LinearProgressIndicator(
        progress = { uiState.progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "${uiState.currentIndex + 1} / ${uiState.totalCards}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))

    val question = uiState.currentQuestion ?: return

    // Mode badge
    ModeBadge(question.mode)

    Spacer(modifier = Modifier.height(20.dp))

    // Mode-specific content
    when (question.mode) {
        StudyMode.MULTIPLE_CHOICE -> MultipleChoiceContent(
            question = question,
            answerState = uiState.answerState,
            isLast = uiState.currentIndex + 1 >= uiState.totalCards,
            onSelectAnswer = onSelectMcAnswer,
            onNext = onNextCard,
        )
        StudyMode.FLASHCARD -> FlashcardContent(
            word = question.word,
            isFlipped = uiState.isFlipped,
            onFlip = onFlipCard,
            onKnew = onKnew,
            onDidNotKnow = onDidNotKnow,
        )
        StudyMode.TYPING -> TypingContent(
            word = question.word,
            userInput = uiState.typingInput,
            answerState = uiState.answerState,
            isLast = uiState.currentIndex + 1 >= uiState.totalCards,
            onInputChange = onTypingInput,
            onSubmit = onSubmitTyping,
            onNext = onNextCard,
        )
        StudyMode.LETTER_ASSEMBLY -> LetterAssemblyContent(
            word = question.word,
            tiles = uiState.assemblyTiles,
            correctAnswer = uiState.assemblyCorrectAnswer,
            answerState = uiState.answerState,
            isLast = uiState.currentIndex + 1 >= uiState.totalCards,
            onPlaceLetter = onPlaceLetter,
            onRemoveLetter = onRemoveLetter,
            onNext = onNextCard,
        )
    }

    // Skip button — shown when the user hasn't answered yet
    if (uiState.answerState == MixedAnswerState.Unanswered) {
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Пропустить",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeBadge(mode: StudyMode) {
    val (emoji, label, bgColor, textColor) = when (mode) {
        StudyMode.MULTIPLE_CHOICE -> listOf(
            "\uD83C\uDFAF", "Тест",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        StudyMode.FLASHCARD -> listOf(
            "\uD83D\uDCCB", "Карточки",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        StudyMode.TYPING -> listOf(
            "\u270D\uFE0F", "Ввод",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        StudyMode.LETTER_ASSEMBLY -> listOf(
            "\uD83D\uDD24", "Сборка",
            AmberDark,
            AmberLight,
        )
    }

    Row(
        modifier = Modifier
            .background(
                color = bgColor as Color,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji as String, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label as String,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor as Color,
        )
    }
}

// --- Multiple Choice ---

@Composable
private fun MultipleChoiceContent(
    question: MixedQuestion,
    answerState: MixedAnswerState,
    isLast: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val successColor = Green60

    WordCard(question.word)

    Spacer(modifier = Modifier.height(28.dp))

    McOptions(
        options = question.mcOptions,
        correctIndex = question.mcCorrectIndex,
        answerState = answerState,
        onSelect = onSelectAnswer,
    )

    val mcAnswer = answerState as? MixedAnswerState.McAnswered
    if (mcAnswer != null) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (mcAnswer.isCorrect) "Правильно!"
            else "Неправильно. Ответ: ${question.word.original}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (mcAnswer.isCorrect) successColor else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        NextButton(isLast = isLast, onClick = onNext)
    }
}

@Composable
private fun McOptions(
    options: List<String>,
    correctIndex: Int,
    answerState: MixedAnswerState,
    onSelect: (Int) -> Unit,
) {
    val correctColor = Green60
    val correctContainerColor = GreenDark
    val isAnswered = answerState is MixedAnswerState.McAnswered
    val answered = answerState as? MixedAnswerState.McAnswered

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEachIndexed { index, option ->
            val containerColorAnim by animateColorAsState(
                targetValue = when {
                    !isAnswered -> MaterialTheme.colorScheme.surface
                    index == correctIndex -> correctContainerColor
                    index == answered?.selectedIndex -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(300),
            )
            val borderColor by animateColorAsState(
                targetValue = when {
                    !isAnswered -> MaterialTheme.colorScheme.outline
                    index == correctIndex -> correctColor
                    index == answered?.selectedIndex -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(300),
            )
            val textColor by animateColorAsState(
                targetValue = when {
                    !isAnswered -> MaterialTheme.colorScheme.onSurface
                    index == correctIndex -> correctColor
                    index == answered?.selectedIndex -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(300),
            )

            OutlinedButton(
                onClick = { onSelect(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isAnswered,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = containerColorAnim,
                    disabledContainerColor = containerColorAnim,
                ),
                border = BorderStroke(
                    width = if (isAnswered && (index == correctIndex || index == answered?.selectedIndex)) 2.dp else 1.dp,
                    color = borderColor,
                ),
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
            }
        }
    }
}

// --- Flashcard ---

@Composable
private fun FlashcardContent(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onKnew: () -> Unit,
    onDidNotKnow: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onFlip,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (rotation <= 90f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нажмите, чтобы перевернуть",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                ) {
                    Text(
                        text = word.original,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    if (word.transcription.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = word.transcription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    if (isFlipped) {
        Text(
            text = "Вы знали это слово?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDidNotKnow,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Не знал", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onKnew,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text("Знал", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Typing ---

@Composable
private fun TypingContent(
    word: Word,
    userInput: String,
    answerState: MixedAnswerState,
    isLast: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    val successColor = Green60
    val isAnswered = answerState != MixedAnswerState.Unanswered

    // Auto-focus when a new word appears
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(word.id) {
        delay(150)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    // Pulsing glow on empty field border
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    // Shake animation on incorrect answer
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(answerState) {
        if (answerState is MixedAnswerState.TypingIncorrect) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    WordCard(word)

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = userInput,
        onValueChange = onInputChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .graphicsLayer {
                translationX = shakeOffset.value
            },
        label = { Text("Как это по-английски?") },
        supportingText = if (!isAnswered && userInput.isEmpty()) {
            { Text("Введите перевод слова") }
        } else null,
        enabled = !isAnswered,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = if (userInput.isEmpty() && !isAnswered) {
                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
            } else {
                MaterialTheme.colorScheme.outline
            },
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (!isAnswered) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = userInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "ПРОВЕРИТЬ",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
            )
        }
    } else {
        when (answerState) {
            is MixedAnswerState.TypingCorrect -> {
                Text(
                    text = "Правильно!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = successColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is MixedAnswerState.TypingIncorrect -> {
                Text(
                    text = "Неправильно. Ответ: ${answerState.correctAnswer}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(20.dp))

        NextButton(isLast = isLast, onClick = onNext)
    }
}

// --- Common composables ---

@Composable
private fun WordCard(word: Word) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = word.translation,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NextButton(isLast: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(
            text = if (isLast) "РЕЗУЛЬТАТЫ" else "ДАЛЕЕ",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun EmptyContent(onNavigateToAddWords: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "\uD83D\uDCDD", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет слов для изучения",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Сначала добавьте слова в словарь",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToAddWords,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Добавить слова",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ResultsContent(
    correctCount: Int,
    totalCards: Int,
    newAchievements: List<AchievementType> = emptyList(),
    sessionReward: SessionReward? = null,
    onFinish: () -> Unit,
) {
    val percentage = if (totalCards > 0) (correctCount * 100) / totalCards else 0
    val successColor = Green60

    Spacer(modifier = Modifier.height(32.dp))

    // Reward emoji + message
    if (sessionReward != null) {
        Text(
            text = sessionReward.emoji,
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(
        text = "$percentage%",
        style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
        ),
        color = if (percentage >= 70) successColor else MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Правильных ответов: $correctCount из $totalCards",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Motivational message from RewardGenerator
    Text(
        text = sessionReward?.motivationalMessage ?: when {
            percentage == 100 -> "Отлично! Все ответы верные!"
            percentage >= 70 -> "Хороший результат! Продолжайте!"
            percentage >= 40 -> "Неплохо, но есть куда расти."
            else -> "Попробуйте ещё раз!"
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    // XP Reward breakdown
    if (sessionReward != null) {
        Spacer(modifier = Modifier.height(20.dp))
        XpRewardCard(sessionReward = sessionReward)
    }

    // Achievement banner
    if (newAchievements.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        AchievementUnlockedBanner(achievements = newAchievements)
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onFinish,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(
            text = "ГОТОВО",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        )
    }
}

@Composable
private fun XpRewardCard(sessionReward: SessionReward) {
    val xp = sessionReward.xpReward
    val bgColor = Surface2
    val textColor = AmberLight
    val subtextColor = TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "+${xp.totalXp} XP",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                XpBreakdownItem(
                    label = "Ответы",
                    value = "+${xp.baseXp}",
                    color = subtextColor,
                )
                if (xp.streakBonus > 0) {
                    XpBreakdownItem(
                        label = "Серия",
                        value = "+${xp.streakBonus}",
                        color = subtextColor,
                    )
                }
                if (xp.perfectBonus > 0) {
                    XpBreakdownItem(
                        label = "Идеально",
                        value = "+${xp.perfectBonus}",
                        color = subtextColor,
                    )
                }
            }

            // Level up notification
            if (xp.leveledUp) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AmberDark,
                    ),
                ) {
                    Text(
                        text = "\uD83C\uDF1F Уровень ${xp.newLevel}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun XpBreakdownItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
        )
    }
}

// --- Letter Assembly ---

@Composable
private fun LetterAssemblyContent(
    word: Word,
    tiles: List<LetterTile>,
    correctAnswer: String,
    answerState: MixedAnswerState,
    isLast: Boolean,
    onPlaceLetter: (Int) -> Unit,
    onRemoveLetter: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val successColor = Green60

    WordCard(word)

    Spacer(modifier = Modifier.height(24.dp))

    LetterAssemblyGrid(
        tiles = tiles,
        correctAnswer = correctAnswer,
        answerState = answerState,
        onPlaceLetter = onPlaceLetter,
        onRemoveLetter = onRemoveLetter,
    )

    when (answerState) {
        is MixedAnswerState.AssemblyCorrect -> {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Правильно!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = successColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            NextButton(isLast = isLast, onClick = onNext)
        }
        is MixedAnswerState.AssemblyIncorrect -> {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Неправильно. Ответ: ${answerState.correctAnswer}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            NextButton(isLast = isLast, onClick = onNext)
        }
        else -> {}
    }
}

private data class TilePosition(val x: Dp, val y: Dp)

@Composable
private fun LetterAssemblyGrid(
    tiles: List<LetterTile>,
    correctAnswer: String,
    answerState: MixedAnswerState,
    onPlaceLetter: (Int) -> Unit,
    onRemoveLetter: (Int) -> Unit,
) {
    val tileSize = 44.dp
    val tileSpacing = 6.dp
    val rowSpacing = 8.dp
    val sectionGap = 20.dp

    val density = LocalDensity.current

    val isAnswered = answerState != MixedAnswerState.Unanswered
    val isCorrect = answerState is MixedAnswerState.AssemblyCorrect

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val containerWidth = maxWidth
        val tileTotalWidth = tileSize + tileSpacing
        val maxPerRow = ((containerWidth + tileSpacing) / tileTotalWidth).toInt().coerceAtLeast(1)

        val slotCount = correctAnswer.length
        val answerRows = (slotCount + maxPerRow - 1) / maxPerRow

        val scrambleTiles = tiles.filter { !it.isPlaced }.sortedBy { it.scrambleIndex }
        val scrambleCount = tiles.size
        val scrambleRows = (scrambleCount + maxPerRow - 1) / maxPerRow

        val answerAreaHeight = (tileSize * answerRows) + (rowSpacing * (answerRows - 1).coerceAtLeast(0))
        val scrambleAreaHeight = (tileSize * scrambleRows) + (rowSpacing * (scrambleRows - 1).coerceAtLeast(0))
        val totalHeight = answerAreaHeight + sectionGap + scrambleAreaHeight

        // Compute answer slot positions (centered per row)
        fun answerSlotPosition(slotIndex: Int): TilePosition {
            val row = slotIndex / maxPerRow
            val itemsInRow = if (row < answerRows - 1) maxPerRow
            else slotCount - (answerRows - 1) * maxPerRow
            val col = slotIndex % maxPerRow
            val rowWidth = tileTotalWidth * itemsInRow - tileSpacing
            val startX = (containerWidth - rowWidth) / 2
            val x = startX + tileTotalWidth * col
            val y = (tileSize + rowSpacing) * row
            return TilePosition(x, y)
        }

        // Compute scramble positions (centered per row)
        fun scramblePosition(scrambleIndex: Int): TilePosition {
            val row = scrambleIndex / maxPerRow
            val itemsInRow = if (row < scrambleRows - 1) maxPerRow
            else scrambleCount - (scrambleRows - 1) * maxPerRow
            val col = scrambleIndex % maxPerRow
            val rowWidth = tileTotalWidth * itemsInRow - tileSpacing
            val startX = (containerWidth - rowWidth) / 2
            val x = startX + tileTotalWidth * col
            val y = answerAreaHeight + sectionGap + (tileSize + rowSpacing) * row
            return TilePosition(x, y)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
        ) {
            // Draw answer slot placeholders
            for (i in 0 until slotCount) {
                val pos = answerSlotPosition(i)
                AnswerSlotPlaceholder(
                    modifier = Modifier
                        .offset(x = pos.x, y = pos.y)
                        .size(tileSize),
                )
            }

            // Draw separator line
            Box(
                modifier = Modifier
                    .offset(y = answerAreaHeight + sectionGap / 2 - 0.5.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
            )

            // Draw each tile with animated position
            tiles.forEach { tile ->
                val targetPos = if (tile.isPlaced) {
                    answerSlotPosition(tile.answerSlotIndex)
                } else {
                    // Find visual position in scramble area
                    val unplacedTiles = tiles.filter { !it.isPlaced }.sortedBy { it.scrambleIndex }
                    val visualIndex = unplacedTiles.indexOfFirst { it.id == tile.id }
                        .coerceAtLeast(0)
                    scramblePosition(visualIndex)
                }

                val animatedX by animateDpAsState(
                    targetValue = targetPos.x,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
                val animatedY by animateDpAsState(
                    targetValue = targetPos.y,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )

                LetterTileView(
                    tile = tile,
                    isAnswered = isAnswered,
                    isCorrect = isCorrect,
                    modifier = Modifier
                        .offset(x = animatedX, y = animatedY)
                        .size(tileSize),
                    onClick = {
                        if (!isAnswered) {
                            if (tile.isPlaced) onRemoveLetter(tile.id)
                            else onPlaceLetter(tile.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AnswerSlotPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.5.dp,
                color = AmberMuted.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
            )
            .background(
                color = AmberDark.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
            ),
    )
}

@Composable
private fun LetterTileView(
    tile: LetterTile,
    isAnswered: Boolean,
    isCorrect: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> GreenDark
            isAnswered && !isCorrect && tile.isPlaced -> Color(0xFF3A1E1E)
            tile.isPlaced -> Amber.copy(alpha = 0.2f)
            else -> AmberDark
        },
        animationSpec = tween(300),
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> Green60
            isAnswered && !isCorrect && tile.isPlaced ->
                MaterialTheme.colorScheme.error
            tile.isPlaced -> Amber
            else -> AmberMuted
        },
        animationSpec = tween(300),
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> Green60
            isAnswered && !isCorrect && tile.isPlaced ->
                MaterialTheme.colorScheme.error
            else -> AmberLight
        },
        animationSpec = tween(300),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor, RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.char.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

// --- Achievement Unlocked Banner ---

@Composable
private fun AchievementUnlockedBanner(
    achievements: List<AchievementType>,
) {
    val bgColor = Surface2
    val textColor = Indigo

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83C\uDF89 ${if (achievements.size == 1) "Новое достижение!" else "Новые достижения!"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )

            Spacer(modifier = Modifier.height(12.dp))

            achievements.forEach { achievement ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Surface1,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = achievement.emoji,
                        fontSize = 28.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                        )
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
