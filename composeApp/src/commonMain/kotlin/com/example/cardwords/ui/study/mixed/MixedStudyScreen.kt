package com.example.cardwords.ui.study.mixed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.data.model.SessionReward
import com.example.cardwords.data.model.Word
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.ui.theme.Amber40
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightDotActive
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar
import com.example.cardwords.ui.theme.Red40
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
//  Design tokens — all from Theme.kt
// ─────────────────────────────────────────────
private val BgPage       = LightBg               // #F5F5F7
private val BgCard       = LightCard             // #FFFFFF
private val Fg           = LightFg               // #1D1D1F
private val FgSecondary  = LightFgSecondary      // #86868B
private val FgMuted      = LightFgMuted          // #C7C7CC
private val Accent       = LightFg               // primary accent = near-black
private val ProgressBar  = LightProgressBar      // #007AFF
private val Success      = Green40               // correct state
private val Failure      = Red40                 // wrong state
private val Warning      = Amber40               // streak / amber
private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg      = Color(0xFFF2F2F7)

// ─────────────────────────────────────────────
//  Mode icons — Canvas drawn, no emojis
// ─────────────────────────────────────────────
private object ModeIconPaths {
    // Test (✓ — simple checkmark)
    const val CHECK = "M4 12L9 17L20 6"
    // Typing (pencil — edit-3 feather style)
    const val PENCIL = "M17.5 3.5A2.121 2.121 0 0 1 20.5 6.5L7 20L3 21L4 17L17.5 3.5Z"
    // Assembly (2x2 grid — letter tiles)
    const val GRID_TL = "M3 3H10V10H3Z"
    const val GRID_TR = "M14 3H21V10H14Z"
    const val GRID_BL = "M3 14H10V21H3Z"
    const val GRID_BR = "M14 14H21V21H14Z"
}

@Composable
private fun CheckIcon(color: Color, modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(ModeIconPaths.CHECK).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            drawPath(path, color, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun CardsIcon(color: Color, modifier: Modifier = Modifier) {
    // Two stacked rectangles — representing flashcards
    Canvas(modifier) {
        val s = size.width / 24f
        val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(s, s, Offset.Zero) {
            // Back card (rotated-like — offset + slight)
            drawRoundRect(
                color = color,
                topLeft = Offset(7f, 3f),
                size = androidx.compose.ui.geometry.Size(14f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                style = stroke,
            )
            // Front card (covers part of back)
            drawRoundRect(
                color = BgCard,
                topLeft = Offset(3f, 5f),
                size = androidx.compose.ui.geometry.Size(14f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(3f, 5f),
                size = androidx.compose.ui.geometry.Size(14f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                style = stroke,
            )
        }
    }
}

@Composable
private fun PencilIcon(color: Color, modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(ModeIconPaths.PENCIL).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            drawPath(path, color, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun GridIcon(color: Color, modifier: Modifier = Modifier) {
    val tl = remember { PathParser().parsePathString(ModeIconPaths.GRID_TL).toPath() }
    val tr = remember { PathParser().parsePathString(ModeIconPaths.GRID_TR).toPath() }
    val bl = remember { PathParser().parsePathString(ModeIconPaths.GRID_BL).toPath() }
    val br = remember { PathParser().parsePathString(ModeIconPaths.GRID_BR).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(s, s, Offset.Zero) {
            drawPath(tl, color, style = stroke)
            drawPath(tr, color, style = stroke)
            drawPath(bl, color, style = stroke)
            drawPath(br, color, style = stroke)
        }
    }
}

@Composable
private fun ChevronLeftIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(15f, 5f)
                lineTo(8f, 12f)
                lineTo(15f, 19f)
            }
            drawPath(path, color, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ─────────────────────────────────────────────
//  Main screen
// ─────────────────────────────────────────────
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .statusBarsPadding(),
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center,
            ) {
                ChevronLeftIcon(color = Fg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Обучение",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
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
                    onSelectMcAnswer = viewModel::selectAnswer,
                    onFlipCard = viewModel::flipCard,
                    onKnew = viewModel::markKnew,
                    onDidNotKnow = viewModel::markDidNotKnow,
                    onTypingInput = viewModel::updateInput,
                    onSubmitTyping = viewModel::submitTypingAnswer,
                    onPlaceLetter = viewModel::placeLetter,
                    onRemoveLetter = viewModel::removeLetter,
                    onNextCard = viewModel::nextCard,
                    onSkip = viewModel::skip,
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
    // Progress + counter
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ProgressBar,
            trackColor = FgMuted.copy(alpha = 0.4f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${uiState.currentIndex + 1} / ${uiState.totalCards}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = FgSecondary,
        )
    }

    Spacer(Modifier.height(16.dp))

    val question = uiState.currentQuestion ?: return

    ModeBadge(question.mode)

    Spacer(Modifier.height(20.dp))

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

    if (uiState.answerState == MixedAnswerState.Unanswered) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Пропустить",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSkip)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ModeBadge(mode: StudyMode) {
    val label = when (mode) {
        StudyMode.MULTIPLE_CHOICE -> "Тест"
        StudyMode.FLASHCARD -> "Карточки"
        StudyMode.TYPING -> "Ввод"
        StudyMode.LETTER_ASSEMBLY -> "Сборка"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BgCard)
            .border(0.5.dp, DividerColor, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize = 14.dp
        when (mode) {
            StudyMode.MULTIPLE_CHOICE -> CheckIcon(Fg, Modifier.size(iconSize))
            StudyMode.FLASHCARD -> CardsIcon(Fg, Modifier.size(iconSize))
            StudyMode.TYPING -> PencilIcon(Fg, Modifier.size(iconSize))
            StudyMode.LETTER_ASSEMBLY -> GridIcon(Fg, Modifier.size(iconSize))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )
    }
}

// ─────────────────────────────────────────────
//  Multiple Choice
// ─────────────────────────────────────────────
@Composable
private fun MultipleChoiceContent(
    question: MixedQuestion,
    answerState: MixedAnswerState,
    isLast: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
) {
    WordCard(question.word)

    Spacer(Modifier.height(24.dp))

    McOptions(
        options = question.mcOptions,
        correctIndex = question.mcCorrectIndex,
        answerState = answerState,
        onSelect = onSelectAnswer,
    )

    val mcAnswer = answerState as? MixedAnswerState.McAnswered
    if (mcAnswer != null) {
        Spacer(Modifier.height(18.dp))
        FeedbackText(
            correct = mcAnswer.isCorrect,
            correctAnswer = question.word.original,
        )
        Spacer(Modifier.height(18.dp))
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
    val isAnswered = answerState is MixedAnswerState.McAnswered
    val answered = answerState as? MixedAnswerState.McAnswered

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, option ->
            val isCorrect = index == correctIndex
            val isSelected = index == answered?.selectedIndex

            val bg by animateColorAsState(
                targetValue = when {
                    !isAnswered -> BgCard
                    isCorrect -> Success.copy(alpha = 0.08f)
                    isSelected -> Failure.copy(alpha = 0.08f)
                    else -> BgCard
                },
                animationSpec = tween(250),
            )
            val borderCol by animateColorAsState(
                targetValue = when {
                    !isAnswered -> DividerColor
                    isCorrect -> Success
                    isSelected -> Failure
                    else -> DividerColor
                },
                animationSpec = tween(250),
            )
            val textCol by animateColorAsState(
                targetValue = when {
                    !isAnswered -> Fg
                    isCorrect -> Success
                    isSelected -> Failure
                    else -> FgSecondary
                },
                animationSpec = tween(250),
            )
            val borderWidth = if (isAnswered && (isCorrect || isSelected)) 1.5.dp else 0.5.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg, RoundedCornerShape(14.dp))
                    .border(borderWidth, borderCol, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isAnswered) { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = option,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textCol,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Flashcard
// ─────────────────────────────────────────────
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(0.5.dp, DividerColor, RoundedCornerShape(20.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onFlip,
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = word.translation,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Нажмите, чтобы перевернуть",
                    fontSize = 13.sp,
                    color = FgSecondary,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            ) {
                Text(
                    text = word.original,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg,
                    textAlign = TextAlign.Center,
                )
                if (word.transcription.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = word.transcription,
                        fontSize = 15.sp,
                        color = FgSecondary,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    if (isFlipped) {
        Text(
            text = "Вы знали это слово?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillButton(
                text = "Не знал",
                onClick = onDidNotKnow,
                bg = Failure.copy(alpha = 0.08f),
                textColor = Failure,
                border = Failure,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Знал",
                onClick = onKnew,
                bg = Success.copy(alpha = 0.08f),
                textColor = Success,
                border = Success,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Typing
// ─────────────────────────────────────────────
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
    val isAnswered = answerState != MixedAnswerState.Unanswered

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(word.id) {
        delay(150)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

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

    Spacer(Modifier.height(20.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Как это по-английски?",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        TextField(
            value = userInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .graphicsLayer { translationX = shakeOffset.value }
                .clip(RoundedCornerShape(14.dp)),
            placeholder = {
                Text("Введите перевод", color = FgMuted, fontSize = 15.sp)
            },
            singleLine = true,
            enabled = !isAnswered,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Fg,
                unfocusedTextColor = Fg,
                disabledTextColor = Fg,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                disabledContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Fg,
            ),
        )
    }

    Spacer(Modifier.height(16.dp))

    if (!isAnswered) {
        PrimaryButton(
            text = "Проверить",
            onClick = onSubmit,
            enabled = userInput.isNotBlank(),
        )
    } else {
        when (answerState) {
            is MixedAnswerState.TypingCorrect -> FeedbackText(correct = true, correctAnswer = word.original)
            is MixedAnswerState.TypingIncorrect -> FeedbackText(
                correct = false,
                correctAnswer = answerState.correctAnswer,
            )
            else -> {}
        }
        Spacer(Modifier.height(16.dp))
        NextButton(isLast = isLast, onClick = onNext)
    }
}

// ─────────────────────────────────────────────
//  Shared components
// ─────────────────────────────────────────────
@Composable
private fun WordCard(word: Word) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(0.5.dp, DividerColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word.translation,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Fg,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeedbackText(correct: Boolean, correctAnswer: String) {
    val color = if (correct) Success else Failure
    val label = if (correct) "Правильно!" else "Неправильно. Ответ: $correctAnswer"
    Text(
        text = label,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) Accent else FgMuted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BgCard,
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    bg: Color,
    textColor: Color,
    border: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

@Composable
private fun NextButton(isLast: Boolean, onClick: () -> Unit) {
    PrimaryButton(
        text = if (isLast) "Результаты" else "Далее",
        onClick = onClick,
    )
}

@Composable
private fun EmptyContent(onNavigateToAddWords: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Stylized empty-box icon instead of emoji
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCard)
                    .border(0.5.dp, DividerColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CardsIcon(FgMuted, Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Нет слов для изучения",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Сначала добавьте слова в словарь",
                fontSize = 15.sp,
                color = FgSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Accent)
                    .clickable(onClick = onNavigateToAddWords)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Добавить слова",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BgCard,
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

    Spacer(Modifier.height(32.dp))

    // Circular progress — visual percentage
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(160.dp),
            color = FgMuted.copy(alpha = 0.3f),
            strokeWidth = 8.dp,
        )
        CircularProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.size(160.dp),
            color = if (percentage >= 70) Success else if (percentage >= 40) Warning else Failure,
            strokeWidth = 8.dp,
        )
        Text(
            text = "$percentage%",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Fg,
        )
    }

    Spacer(Modifier.height(20.dp))

    Text(
        text = "Правильных ответов: $correctCount из $totalCards",
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = Fg,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = sessionReward?.motivationalMessage ?: when {
            percentage == 100 -> "Отлично! Все ответы верные"
            percentage >= 70 -> "Хороший результат, продолжайте"
            percentage >= 40 -> "Неплохо, но есть куда расти"
            else -> "Попробуйте ещё раз"
        },
        fontSize = 14.sp,
        color = FgSecondary,
        textAlign = TextAlign.Center,
    )

    if (sessionReward != null) {
        Spacer(Modifier.height(20.dp))
        XpRewardCard(sessionReward = sessionReward)
    }

    if (newAchievements.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        AchievementUnlockedBanner(achievements = newAchievements)
    }

    Spacer(Modifier.height(28.dp))

    PrimaryButton(text = "Готово", onClick = onFinish)
}

@Composable
private fun XpRewardCard(sessionReward: SessionReward) {
    val xp = sessionReward.xpReward

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(0.5.dp, DividerColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "+${xp.totalXp} XP",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Fg,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            XpBreakdownItem(label = "Ответы", value = "+${xp.baseXp}")
            if (xp.streakBonus > 0) {
                XpBreakdownItem(label = "Серия", value = "+${xp.streakBonus}")
            }
            if (xp.perfectBonus > 0) {
                XpBreakdownItem(label = "Идеально", value = "+${xp.perfectBonus}")
            }
        }

        if (xp.leveledUp) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Warning.copy(alpha = 0.12f))
                    .border(0.5.dp, Warning, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Warning),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Уровень ${xp.newLevel}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Warning,
                )
            }
        }
    }
}

@Composable
private fun XpBreakdownItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = FgSecondary,
        )
    }
}

// ─────────────────────────────────────────────
//  Letter Assembly
// ─────────────────────────────────────────────
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
    WordCard(word)

    Spacer(Modifier.height(24.dp))

    LetterAssemblyGrid(
        tiles = tiles,
        correctAnswer = correctAnswer,
        answerState = answerState,
        onPlaceLetter = onPlaceLetter,
        onRemoveLetter = onRemoveLetter,
    )

    when (answerState) {
        is MixedAnswerState.AssemblyCorrect -> {
            Spacer(Modifier.height(16.dp))
            FeedbackText(correct = true, correctAnswer = correctAnswer)
            Spacer(Modifier.height(18.dp))
            NextButton(isLast = isLast, onClick = onNext)
        }
        is MixedAnswerState.AssemblyIncorrect -> {
            Spacer(Modifier.height(16.dp))
            FeedbackText(correct = false, correctAnswer = answerState.correctAnswer)
            Spacer(Modifier.height(18.dp))
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

    val isAnswered = answerState != MixedAnswerState.Unanswered
    val isCorrect = answerState is MixedAnswerState.AssemblyCorrect

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val tileTotalWidth = tileSize + tileSpacing
        val maxPerRow = ((containerWidth + tileSpacing) / tileTotalWidth).toInt().coerceAtLeast(1)

        val slotCount = correctAnswer.length
        val answerRows = (slotCount + maxPerRow - 1) / maxPerRow

        val scrambleCount = tiles.size
        val scrambleRows = (scrambleCount + maxPerRow - 1) / maxPerRow

        val answerAreaHeight = (tileSize * answerRows) + (rowSpacing * (answerRows - 1).coerceAtLeast(0))
        val scrambleAreaHeight = (tileSize * scrambleRows) + (rowSpacing * (scrambleRows - 1).coerceAtLeast(0))
        val totalHeight = answerAreaHeight + sectionGap + scrambleAreaHeight

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
            // Answer slot placeholders
            for (i in 0 until slotCount) {
                val pos = answerSlotPosition(i)
                AnswerSlotPlaceholder(
                    modifier = Modifier
                        .offset(x = pos.x, y = pos.y)
                        .size(tileSize),
                )
            }

            // Separator line
            Box(
                modifier = Modifier
                    .offset(y = answerAreaHeight + sectionGap / 2 - 0.5.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor),
            )

            // Tiles with animated positions — draw-phase translation for smoothness
            val density = LocalDensity.current
            val unplaced = remember(tiles) {
                tiles.filter { !it.isPlaced }.sortedBy { it.scrambleIndex }
            }
            tiles.forEach { tile ->
                key(tile.id) {
                    val targetPos = if (tile.isPlaced) {
                        answerSlotPosition(tile.answerSlotIndex)
                    } else {
                        val visualIndex = unplaced.indexOfFirst { it.id == tile.id }
                            .coerceAtLeast(0)
                        scramblePosition(visualIndex)
                    }

                    val targetXpx = with(density) { targetPos.x.toPx() }
                    val targetYpx = with(density) { targetPos.y.toPx() }

                    val animX = remember { Animatable(targetXpx) }
                    val animY = remember { Animatable(targetYpx) }

                    LaunchedEffect(targetXpx, targetYpx) {
                        animX.animateTo(
                            targetXpx,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                    LaunchedEffect(targetXpx, targetYpx) {
                        animY.animateTo(
                            targetYpx,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }

                    LetterTileView(
                        tile = tile,
                        isAnswered = isAnswered,
                        isCorrect = isCorrect,
                        modifier = Modifier
                            .size(tileSize)
                            .graphicsLayer {
                                translationX = animX.value
                                translationY = animY.value
                            },
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
}

@Composable
private fun AnswerSlotPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FieldBg, RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = DividerColor,
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
    val bgCol by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> Success.copy(alpha = 0.12f)
            isAnswered && !isCorrect && tile.isPlaced -> Failure.copy(alpha = 0.12f)
            tile.isPlaced -> BgCard
            else -> BgCard
        },
        animationSpec = tween(250),
    )

    val borderCol by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> Success
            isAnswered && !isCorrect && tile.isPlaced -> Failure
            tile.isPlaced -> Fg
            else -> DividerColor
        },
        animationSpec = tween(250),
    )

    val textCol by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect && tile.isPlaced -> Success
            isAnswered && !isCorrect && tile.isPlaced -> Failure
            else -> Fg
        },
        animationSpec = tween(250),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgCol, RoundedCornerShape(10.dp))
            .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.char.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textCol,
        )
    }
}

// ─────────────────────────────────────────────
//  Achievements Banner
// ─────────────────────────────────────────────
@Composable
private fun AchievementUnlockedBanner(achievements: List<AchievementType>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(0.5.dp, DividerColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LightDotActive),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (achievements.size == 1) "Новое достижение" else "Новые достижения",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
        }

        Spacer(Modifier.height(12.dp))

        achievements.forEach { achievement ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FieldBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Small bullet instead of emoji
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCard)
                        .border(0.5.dp, DividerColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CheckIcon(Success, Modifier.size(14.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Fg,
                    )
                    Text(
                        text = achievement.description,
                        fontSize = 12.sp,
                        color = FgSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

