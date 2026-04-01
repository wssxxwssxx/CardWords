package com.example.cardwords.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.DungeonPurple
import com.example.cardwords.ui.theme.DungeonPurpleLight
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.SkyBlue
import com.example.cardwords.ui.theme.SoftGold
import com.example.cardwords.ui.theme.Surface0
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextDim
import com.example.cardwords.ui.theme.TextHeading
import com.example.cardwords.ui.theme.TextMuted
import com.example.cardwords.ui.theme.TextPrimary
import com.example.cardwords.ui.theme.TextSecondary

// ═══════════════════════════════════════════════════════════════
// Midnight Blue — card border
// ═══════════════════════════════════════════════════════════════
private val CardBorder = Surface2

@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToSmartSession: () -> Unit = {},
    onNavigateToWordFall: () -> Unit = {},
    onNavigateToDungeon: () -> Unit = {},
) {
    val viewModel = remember { HomeViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Header(state = state)

        // Daily Progress Ring
        if (state.isLoaded) {
            DailyProgressSection(state = state)
        }

        // Smart Session CTA
        if (state.wordCount > 0) {
            SmartSessionCard(
                reviewCount = state.reviewWordCount,
                onClick = onNavigateToSmartSession,
            )
        } else {
            EmptyHeroCard()
        }

        // Activity Cards
        if (state.wordCount > 0) {
            ActivityCards(
                state = state,
                onStudy = onNavigateToStudy,
                onWordFall = onNavigateToWordFall,
                onDungeon = onNavigateToDungeon,
            )
        }

        // Weekly Stats
        if (state.isLoaded && state.totalReviews > 0) {
            WeeklyStatsCard(state = state)
        }

        // Motivational
        if (state.isLoaded && state.wordCount > 0) {
            MotivationalBanner(state = state)
        }

        // Mastery Progress
        if (state.isLoaded && state.totalTrackedWords > 0) {
            MasterySection(state = state)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// 1. Header
// ═══════════════════════════════════════════════════════════════

@Composable
private fun Header(state: HomeUiState) {
    val greeting = when (state.greetingTime) {
        "morning" -> "Доброе утро"
        "evening" -> "Добрый вечер"
        "night" -> "Доброй ночи"
        else -> "Добрый день"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = greeting,
                fontSize = 12.sp,
                color = TextMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Главная",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (state.currentStreak > 0) {
                // Streak badge — amber glow
                Box(
                    modifier = Modifier
                        .background(Surface1, RoundedCornerShape(20.dp))
                        .border(1.dp, Amber.copy(alpha = 0.27f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Amber),
                        )
                        Text(
                            text = "${state.currentStreak}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber,
                        )
                    }
                }
            }

            if (state.currentLevel > 0) {
                // XP badge — purple glow
                Box(
                    modifier = Modifier
                        .background(Surface1, RoundedCornerShape(20.dp))
                        .border(1.dp, DungeonPurple.copy(alpha = 0.27f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DungeonPurple),
                        )
                        Text(
                            text = "Ур. ${state.currentLevel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DungeonPurpleLight,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. Daily Progress Ring — double ring (blue + purple accent)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DailyProgressSection(state: HomeUiState) {
    val progress = state.dailyGoalProgress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(800),
    )
    val (xpCurrent, xpNeeded) = state.xpProgress
    val xpFraction = if (xpNeeded > 0) (xpCurrent.toFloat() / xpNeeded).coerceIn(0f, 1f) else 0f
    val animatedXpProgress by animateFloatAsState(
        targetValue = xpFraction,
        animationSpec = tween(800),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Double ring
        val displayText = "${progress.wordsStudied}/${progress.goal}"
        val textSize = when {
            displayText.length >= 6 -> 14.sp
            displayText.length >= 5 -> 16.sp
            else -> 18.sp
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp),
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val outerStroke = 9.dp.toPx()
                val outerRadius = 40.dp.toPx()
                val center = Offset(size.width / 2, size.height / 2)

                // Outer track
                drawCircle(
                    color = Surface2,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = outerStroke),
                )

                // Blue progress arc — daily goal
                if (animatedProgress > 0f) {
                    val arcSize = Size(outerRadius * 2, outerRadius * 2)
                    val arcOffset = Offset(
                        center.x - outerRadius,
                        center.y - outerRadius,
                    )
                    drawArc(
                        color = SkyBlue,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
                    )
                }

                // Inner purple ring — XP progress to next level
                val innerRadius = 30.dp.toPx()
                val innerStroke = 3.dp.toPx()

                // Inner track
                drawCircle(
                    color = Surface2.copy(alpha = 0.4f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = innerStroke),
                )

                if (animatedXpProgress > 0f) {
                    val innerArcSize = Size(innerRadius * 2, innerRadius * 2)
                    val innerArcOffset = Offset(
                        center.x - innerRadius,
                        center.y - innerRadius,
                    )
                    drawArc(
                        color = DungeonPurple,
                        startAngle = -90f,
                        sweepAngle = animatedXpProgress * 360f,
                        useCenter = false,
                        topLeft = innerArcOffset,
                        size = innerArcSize,
                        style = Stroke(width = innerStroke, cap = StrokeCap.Round),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayText,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                )
                Text(
                    text = "СЛОВ",
                    fontSize = 9.sp,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Week dots with labels
        if (state.weekDays.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WeekDot(day = day)
                        Text(
                            text = day.label,
                            fontSize = 9.sp,
                            color = if (day.isToday) TextSecondary else TextDim,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDot(day: WeekDay) {
    val isCompleted = day.isActive
    val isCurrent = day.isToday

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCurrent -> DungeonPurple
                    isCompleted -> SkyBlue
                    else -> Surface1
                },
            )
            .then(
                if (!isCompleted && !isCurrent) {
                    Modifier.border(1.5.dp, Surface2, CircleShape)
                } else Modifier,
            ),
    )
}

// ═══════════════════════════════════════════════════════════════
// 3. Smart Session — hero card with left blue border
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SmartSessionCard(
    reviewCount: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1)
            .border(1.dp, Surface2, shape)
            .border(
                width = 3.dp,
                color = SkyBlue,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp, 18.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Star icon in blue container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SkyBlue.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\u2B50", fontSize = 16.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Умная сессия",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (reviewCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$reviewCount ${wordsWord(reviewCount)} к повторению",
                        fontSize = 11.sp,
                        color = TextMuted,
                    )
                }
            }

            Text(
                text = "\u203A",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = SkyBlue,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. Activity Cards — two-col + dungeon
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActivityCards(
    state: HomeUiState,
    onStudy: () -> Unit,
    onWordFall: () -> Unit,
    onDungeon: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Top row: 2 cards
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActivityCard(
                emoji = "\uD83D\uDCDA",
                title = "Учить новые",
                subtitle = "${state.wordCount} ${wordsWord(state.wordCount.toInt())}",
                progressFraction = 0.6f,
                progressColor = SkyBlue,
                onClick = onStudy,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            if (state.wordCount >= 5) {
                ActivityCard(
                    emoji = "\uD83C\uDF27\uFE0F",
                    title = "Словопад",
                    subtitle = "Поймай слово",
                    progressFraction = 0.35f,
                    progressColor = DungeonPurple,
                    borderColor = SkyBlue.copy(alpha = 0.13f),
                    accentLineColor = DungeonPurple,
                    onClick = onWordFall,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        // Dungeon card
        if (state.wordCount >= 3) {
            DungeonCard(
                highestFloor = state.dungeonHighestFloor,
                isLocked = false,
                onClick = onDungeon,
            )
        }
    }
}

@Composable
private fun ActivityCard(
    emoji: String,
    title: String,
    subtitle: String,
    progressFraction: Float,
    progressColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = Surface2,
    accentLineColor: Color? = null,
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                // Emoji
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                )

                Spacer(Modifier.weight(1f))

                // Title
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                )

                Spacer(Modifier.height(8.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Surface2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressColor),
                    )
                }
            }

            // Bottom accent line
            if (accentLineColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            accentLineColor,
                            RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun DungeonCard(
    highestFloor: Int,
    isLocked: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .then(if (!isLocked) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (isLocked) Modifier.alpha(0.6f) else Modifier),
    ) {
        // Top purple border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(DungeonPurple),
        )

        // Side + bottom border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Surface2, RoundedCornerShape(18.dp)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp, 14.dp, 18.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DungeonPurple.copy(alpha = 0.13f))
                    .border(1.dp, DungeonPurple.copy(alpha = 0.27f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\uD83C\uDFF0", fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Данжен",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isLocked) "Открывается на Ур.10" else "Сражайся словами",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }

            if (isLocked) {
                Text(text = "\uD83D\uDD12", fontSize = 16.sp)
            } else {
                // Level badge
                Box(
                    modifier = Modifier
                        .background(DungeonPurple.copy(alpha = 0.13f), RoundedCornerShape(10.dp))
                        .border(1.dp, DungeonPurple.copy(alpha = 0.27f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (highestFloor > 0) "Этаж $highestFloor" else "Новый",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DungeonPurpleLight,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 5. Weekly Stats Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WeeklyStatsCard(state: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Surface2, RoundedCornerShape(18.dp))
            .padding(14.dp, 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                value = "${state.weeklySummary.thisWeekWords}",
                label = "слов выучено",
                valueColor = TextPrimary,
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Surface2),
            )

            StatColumn(
                value = "${state.weeklyAccuracy}%",
                label = "точность",
                valueColor = Green60,
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(Surface2),
            )

            val hours = state.weeklyTimeMinutes / 60
            val mins = state.weeklyTimeMinutes % 60
            val timeStr = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"

            StatColumn(
                value = timeStr,
                label = "за неделю",
                valueColor = Amber,
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextDim,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// 6. Motivational Banner — left amber border
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MotivationalBanner(state: HomeUiState) {
    val (xpCurrent, xpNeeded) = state.xpProgress
    val wordsToLevel = if (xpNeeded > 0) {
        val xpLeft = xpNeeded - xpCurrent
        (xpLeft / 10).coerceAtLeast(1)
    } else 5

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1)
            .border(1.dp, Amber.copy(alpha = 0.33f), shape)
            .border(3.dp, Amber, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Amber, CircleShape),
            )

            Text(
                text = "Ещё $wordsToLevel ${wordsWord(wordsToLevel)} до нового уровня!",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 7. Mastery Progress
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MasterySection(state: HomeUiState) {
    val m = state.masteryBreakdown
    val total = m.newCount + m.learningCount + m.knownCount + m.masteredCount
    if (total == 0) return

    val masteredFraction by animateFloatAsState(
        targetValue = m.masteredCount.toFloat() / total,
        animationSpec = tween(600),
    )
    val knownFraction by animateFloatAsState(
        targetValue = m.knownCount.toFloat() / total,
        animationSpec = tween(600),
    )
    val learningFraction by animateFloatAsState(
        targetValue = m.learningCount.toFloat() / total,
        animationSpec = tween(600),
    )
    val newFraction by animateFloatAsState(
        targetValue = m.newCount.toFloat() / total,
        animationSpec = tween(600),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Прогресс",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (masteredFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(masteredFraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(Green60),
                )
            }
            if (knownFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(knownFraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(SkyBlue),
                )
            }
            if (learningFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(learningFraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(Amber),
                )
            }
            if (newFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(newFraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(Surface2),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MasteryLegendItem(color = Green60, label = "Освоено", count = m.masteredCount)
            MasteryLegendItem(color = SkyBlue, label = "Знаю", count = m.knownCount)
            MasteryLegendItem(color = Amber, label = "Учу", count = m.learningCount)
            MasteryLegendItem(color = TextDim, label = "Новые", count = m.newCount)
        }
    }
}

@Composable
private fun MasteryLegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Empty State
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Surface2, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "\uD83D\uDCDA", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Добавьте слова для начала",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Выберите набор слов или добавьте свои",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════

private fun wordsWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "слов"
        mod10 == 1 -> "слово"
        mod10 in 2..4 -> "слова"
        else -> "слов"
    }
}
