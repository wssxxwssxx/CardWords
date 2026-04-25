package com.example.cardwords.ui.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.remote.TestQuestion
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar
import com.example.cardwords.ui.theme.Red40

// ─────────────────────────────────────────────
//  Design tokens — from Theme.kt
// ─────────────────────────────────────────────
private val BgPage       = LightBg
private val BgCard       = LightCard
private val Fg           = LightFg
private val FgSecondary  = LightFgSecondary
private val FgMuted      = LightFgMuted
private val BluePrimary  = LightProgressBar
private val Success      = Green40
private val Failure      = Red40
private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg      = Color(0xFFF2F2F7)
private val BlankAccent  = Color(0xFFEDEDF0)

// ─────────────────────────────────────────────
//  Icons (Canvas — no emojis)
// ─────────────────────────────────────────────
private object TestIcons {
    const val CHEVRON_L = "M15 5L8 12L15 19"
    const val CHECK = "M4 12L9 17L20 6"
    const val CROSS = "M6 6L18 18 M18 6L6 18"
    const val PLUS = "M12 5V19 M5 12H19"
    const val REFRESH = "M21 12A9 9 0 1 1 12 3 C15 3 17 4.3 19 6 M21 3V7H17"
    const val WARN = "M12 3L22 20H2L12 3Z M12 10V14 M12 17V17.01"
}

@Composable
private fun SvgLine(
    pathString: String,
    color: Color,
    modifier: Modifier,
    strokeWidth: Float = 2f,
) {
    val path = remember(pathString) { PathParser().parsePathString(pathString).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun ChevronLeftIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.CHEVRON_L, color, modifier)

@Composable
private fun CheckIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.CHECK, color, modifier, strokeWidth = 2.4f)

@Composable
private fun CrossIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.CROSS, color, modifier, strokeWidth = 2.2f)

@Composable
private fun PlusIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.PLUS, color, modifier, strokeWidth = 2.2f)

@Composable
private fun RefreshIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.REFRESH, color, modifier, strokeWidth = 1.8f)

@Composable
private fun WarnIcon(color: Color, modifier: Modifier) =
    SvgLine(TestIcons.WARN, color, modifier, strokeWidth = 1.8f)

// ─────────────────────────────────────────────
//  Entry point
// ─────────────────────────────────────────────
@Composable
fun TestScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { TestViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .statusBarsPadding(),
    ) {
        TestHeader(
            currentIndex = state.currentIndex,
            total = state.questions.size,
            progress = state.progress,
            showProgress = state.phase == TestPhase.QUESTION || state.phase == TestPhase.ANSWERED,
            onBack = onNavigateBack,
        )

        when (state.phase) {
            TestPhase.LOADING -> LoadingContent()
            TestPhase.QUESTION, TestPhase.ANSWERED -> QuestionContent(
                state = state,
                onSelectAnswer = viewModel::selectAnswer,
                onNext = viewModel::nextQuestion,
                onWordTapped = viewModel::onWordTapped,
                onDismissTooltip = viewModel::dismissTooltip,
                onAddWord = viewModel::addSelectedToDictionary,
            )
            TestPhase.SUBMITTING -> LoadingContent(text = "Отправка результатов…")
            TestPhase.RESULTS -> ResultsContent(
                state = state,
                onBack = onNavigateBack,
                onRetry = viewModel::startTest,
            )
            TestPhase.ERROR -> ErrorContent(
                error = state.error,
                onRetry = viewModel::startTest,
                onBack = onNavigateBack,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Header
// ─────────────────────────────────────────────
@Composable
private fun TestHeader(
    currentIndex: Int,
    total: Int,
    progress: Float,
    showProgress: Boolean,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                ChevronLeftIcon(color = Fg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Тест",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
            )
            Spacer(Modifier.weight(1f))
            if (showProgress && total > 0) {
                Text(
                    text = "${currentIndex + 1}/$total",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FgSecondary,
                    modifier = Modifier.padding(end = 16.dp),
                )
            } else {
                Spacer(Modifier.width(52.dp))
            }
        }

        if (showProgress) {
            val animatedProgress by animateFloatAsState(
                targetValue = if (total > 0) progress else 0f,
                animationSpec = tween(300),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .height(4.dp)
                    .cardBg(color = DividerColor, cornerRadius = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .cardBg(color = BluePrimary, cornerRadius = 2.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Loading
// ─────────────────────────────────────────────
@Composable
private fun LoadingContent(text: String = "Генерация вопросов…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(38.dp),
                color = Fg,
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Fg)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Это может занять 5-10 секунд",
                fontSize = 12.sp,
                color = FgSecondary,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Question
// ─────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionContent(
    state: TestUiState,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onWordTapped: (Int, List<String>) -> Unit,
    onDismissTooltip: () -> Unit,
    onAddWord: () -> Unit,
) {
    val question = state.currentQuestion ?: return
    val isAnswered = state.phase == TestPhase.ANSWERED
    val sentenceWords = remember(question.sentence) { question.sentence.split(" ") }
    val hasTooltip = state.selectedWordIndices.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { if (hasTooltip) onDismissTooltip() },
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // Hint — subtle helper above sentence (no translation, just instruction)
        Text(
            text = "Заполните пропуск",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FgSecondary,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        // Sentence card — no translation shown here
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cardBg(
                    color = BgCard,
                    borderColor = DividerColor,
                    borderWidth = 0.5.dp,
                    cornerRadius = 20.dp,
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sentenceWords.forEachIndexed { index, word ->
                    val isBlank = word.contains("___")
                    if (isBlank) {
                        // Blank placeholder — pill shape, soft accent
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                                .width(52.dp)
                                .height(22.dp)
                                .cardBg(color = BlankAccent, cornerRadius = 50.dp),
                        )
                    } else {
                        val isSelected = index in state.selectedWordIndices
                        Text(
                            text = word,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) BluePrimary else Fg,
                            modifier = Modifier
                                .clickable { onWordTapped(index, sentenceWords) }
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Word/phrase translation tooltip — shown ONLY when user explicitly taps a word
            AnimatedVisibility(
                visible = hasTooltip && state.phraseTranslation != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    TranslationTooltip(
                        phrase = state.selectedPhrase ?: "",
                        translation = state.phraseTranslation ?: "",
                        wordAdded = state.wordAdded,
                        onAdd = onAddWord,
                    )
                }
            }
        }

        // Options grid (2x2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        if (index < question.options.size) {
                            OptionButton(
                                text = question.options[index],
                                isSelected = state.selectedAnswerIndex == index,
                                isCorrect = question.options[index] == question.correctAnswer,
                                isAnswered = isAnswered,
                                onClick = { if (!isAnswered) onSelectAnswer(index) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (isAnswered) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .cardBg(color = Fg, cornerRadius = 50.dp, clipContent = true)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.isLastQuestion) "Завершить" else "Далее",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BgCard,
                )
            }
        }
    }
}

@Composable
private fun TranslationTooltip(
    phrase: String,
    translation: String,
    wordAdded: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = FieldBg, cornerRadius = 14.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phrase,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = translation,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BluePrimary,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (wordAdded) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .cardBg(color = Success.copy(alpha = 0.15f), cornerRadius = 50.dp),
                contentAlignment = Alignment.Center,
            ) {
                CheckIcon(color = Success, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .cardBg(color = BgCard, cornerRadius = 50.dp, clipContent = true)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                PlusIcon(color = Fg, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderWidth = if (isAnswered && (isCorrect || isSelected)) 1.5.dp else 0.5.dp
    val targetBg = when {
        !isAnswered -> BgCard
        isCorrect -> Success.copy(alpha = 0.08f)
        isSelected && !isCorrect -> Failure.copy(alpha = 0.08f)
        else -> BgCard
    }
    val targetBorder = when {
        !isAnswered -> if (isSelected) Fg else DividerColor
        isCorrect -> Success
        isSelected && !isCorrect -> Failure
        else -> DividerColor
    }
    val targetText = when {
        !isAnswered -> Fg
        isCorrect -> Success
        isSelected && !isCorrect -> Failure
        else -> FgSecondary
    }
    val bg by animateColorAsState(targetBg, tween(250))
    val border by animateColorAsState(targetBorder, tween(250))
    val textColor by animateColorAsState(targetText, tween(250))

    Box(
        modifier = modifier
            .height(54.dp)
            .cardBg(
                color = bg,
                borderColor = border,
                borderWidth = borderWidth,
                cornerRadius = 14.dp,
                clipContent = true,
            )
            .clickable(enabled = !isAnswered, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

// ─────────────────────────────────────────────
//  Results — per-question breakdown with translations
// ─────────────────────────────────────────────
@Composable
private fun ResultsContent(
    state: TestUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val result = state.result ?: return
    val percentage = result.correctPercentage.toInt()

    // Map answers by question id for fast lookup
    val answersById = remember(state.answers) { state.answers }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))

        // Score ring
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(140.dp),
                color = DividerColor,
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.size(140.dp),
                color = when {
                    percentage >= 70 -> Success
                    percentage >= 40 -> BluePrimary
                    else -> Failure
                },
                strokeWidth = 8.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percentage%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg,
                )
                Text(
                    text = "${result.score} из ${result.total}",
                    fontSize = 12.sp,
                    color = FgSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when {
                percentage == 100 -> "Идеально!"
                percentage >= 70 -> "Отличный результат"
                percentage >= 40 -> "Хороший старт"
                else -> "Не сдавайтесь, попробуйте ещё"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )

        Spacer(Modifier.height(24.dp))

        // Per-question review — user explicitly asked for the translation here
        if (state.questions.isNotEmpty()) {
            Text(
                text = "РАЗБОР ОТВЕТОВ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FgSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 10.dp),
                textAlign = TextAlign.Start,
            )

            state.questions.forEachIndexed { idx, question ->
                val userAnswer = answersById[question.id]
                val isCorrect = userAnswer == question.correctAnswer
                ReviewCard(
                    index = idx + 1,
                    question = question,
                    userAnswer = userAnswer,
                    isCorrect = isCorrect,
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Primary action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .cardBg(color = Fg, cornerRadius = 50.dp, clipContent = true)
                .clickable(onClick = onRetry),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RefreshIcon(color = BgCard, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Пройти ещё раз",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BgCard,
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "На главную",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(12.dp),
        )
    }
}

@Composable
private fun ReviewCard(
    index: Int,
    question: TestQuestion,
    userAnswer: String?,
    isCorrect: Boolean,
) {
    // Reconstruct sentence with blank replaced by the correct answer in color
    val parts = remember(question.sentence) { question.sentence.split("___") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = BgCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 16.dp,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Question index badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .cardBg(color = FieldBg, cornerRadius = 50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FgSecondary,
                )
            }
            Spacer(Modifier.width(10.dp))
            // Correct / wrong badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .cardBg(
                        color = if (isCorrect) Success.copy(alpha = 0.15f) else Failure.copy(alpha = 0.15f),
                        cornerRadius = 50.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCorrect) {
                    CheckIcon(color = Success, modifier = Modifier.size(12.dp))
                } else {
                    CrossIcon(color = Failure, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = question.translationHint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FgSecondary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(10.dp))

        // Sentence with correct answer inline (always shown in green)
        val sentenceText = if (parts.size == 2) {
            parts[0] + question.correctAnswer + parts[1]
        } else {
            question.sentence
        }
        Text(
            text = sentenceText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Fg,
        )

        Spacer(Modifier.height(12.dp))

        // Always show the user's answer, with correct answer on second row if wrong
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AnswerLine(
                label = "Ваш ответ",
                value = userAnswer ?: "—",
                color = if (isCorrect) Success else Failure,
                icon = if (isCorrect) AnswerIcon.CHECK else AnswerIcon.CROSS,
            )
            if (!isCorrect) {
                AnswerLine(
                    label = "Правильный",
                    value = question.correctAnswer,
                    color = Success,
                    icon = AnswerIcon.CHECK,
                )
            }
        }
    }
}

private enum class AnswerIcon { CHECK, CROSS }

@Composable
private fun AnswerLine(
    label: String,
    value: String,
    color: Color,
    icon: AnswerIcon,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = color.copy(alpha = 0.08f), cornerRadius = 10.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .cardBg(color = color.copy(alpha = 0.18f), cornerRadius = 50.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (icon) {
                AnswerIcon.CHECK -> CheckIcon(color = color, modifier = Modifier.size(10.dp))
                AnswerIcon.CROSS -> CrossIcon(color = color, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────
//  Error
// ─────────────────────────────────────────────
@Composable
private fun ErrorContent(
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .cardBg(color = Failure.copy(alpha = 0.12f), cornerRadius = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                WarnIcon(color = Failure, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = error ?: "Произошла ошибка",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Fg,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .height(48.dp)
                    .cardBg(color = Fg, cornerRadius = 50.dp, clipContent = true)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RefreshIcon(color = BgCard, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Попробовать снова",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BgCard,
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Назад",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FgSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(10.dp),
            )
        }
    }
}
