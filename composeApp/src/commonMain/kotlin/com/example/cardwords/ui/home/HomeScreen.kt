package com.example.cardwords.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightDotActive
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

// ─────────────────────────────────────────────
//  Design tokens — aliases pointing to Theme.kt
// ─────────────────────────────────────────────
private val BgPage       = LightBg
private val BgCard       = LightCard
private val Fg           = LightFg
private val FgSecondary  = LightFgSecondary
private val FgMuted      = LightFgMuted
private val DotActive    = LightDotActive
private val DotToday     = LightFg
private val ProgressBlue = LightProgressBar

// ─────────────────────────────────────────────
//  SVG path data (from provided icon files)
// ─────────────────────────────────────────────
private object Icons {
    // icon-home.svg (viewBox 0 0 24 24)
    const val HOME =
        "M3 10.5L12 3L21 10.5V20C21 20.2652 20.8946 20.5196 20.7071 20.7071C20.5196 20.8946 " +
        "20.2652 21 20 21H15V15H9V21H4C3.73478 21 3.48043 20.8946 3.29289 20.7071C3.10536 " +
        "20.5196 3 20.2652 3 20V10.5Z"

    // Frame.svg — graduation cap (viewBox 0 0 16 16, two paths)
    const val GRAD_LINE = "M8 2L0.666668 6L8 10L14 6.66667V11.3333"
    const val GRAD_ARC  = "M3.33333 7.66666V11C3.33333 12 5.66667 13 8 13C10.3333 13 12.6667 12 12.6667 11V7.66666"

    // Frame (1).svg — clipboard + checkmark (viewBox 0 0 16 16)
    const val CLIP_BODY  =
        "M5.99999 3.33334H4.66666C4.31304 3.33334 3.9739 3.47382 3.72385 3.72387C3.4738 3.97392 " +
        "3.33333 4.31305 3.33333 4.66668V12.6667C3.33333 13.0203 3.4738 13.3594 3.72385 13.6095C" +
        "3.9739 13.8595 4.31304 14 4.66666 14H11.3333C11.687 14 12.0261 13.8595 12.2761 13.6095C" +
        "12.5262 13.3594 12.6667 13.0203 12.6667 12.6667V4.66668C12.6667 4.31305 12.5262 3.97392 " +
        "12.2761 3.72387C12.0261 3.47382 11.687 3.33334 11.3333 3.33334H10"
    const val CLIP_TAB   =
        "M9.33333 1.33334H6.66667C6.29848 1.33334 6 1.63182 6 2.00001V3.33334C6 3.70153 6.29848 " +
        "4.00001 6.66667 4.00001H9.33333C9.70152 4.00001 10 3.70153 10 3.33334V2.00001C10 1.63182 " +
        "9.70152 1.33334 9.33333 1.33334Z"
    const val CLIP_CHECK = "M6 9.33333L7.33333 10.6667L10 8"

    // Frame (2).svg — puzzle piece (viewBox 0 0 16 16)
    const val PUZZLE =
        "M5.33334 2.66668C5.33334 2.31305 5.47382 1.97392 5.72387 1.72387C5.97392 1.47382 " +
        "6.31305 1.33334 6.66668 1.33334C7.0203 1.33334 7.35944 1.47382 7.60949 1.72387C7.85953 " +
        "1.97392 8.00001 2.31305 8.00001 2.66668V4.00001H10.6667C11.0203 4.00001 11.3594 4.14049 " +
        "11.6095 4.39053C11.8595 4.64058 12 4.97972 12 5.33334V8.00001H13.3333C13.687 8.00001 " +
        "14.0261 8.14049 14.2762 8.39053C14.5262 8.64058 14.6667 8.97972 14.6667 9.33334C14.6667 " +
        "9.68697 14.5262 10.0261 14.2762 10.2762C14.0261 10.5262 13.687 10.6667 13.3333 10.6667H12" +
        "V13.3333C12 13.687 11.8595 14.0261 11.6095 14.2762C11.3594 14.5262 11.0203 14.6667 " +
        "10.6667 14.6667H8.00001V13.3333C8.00001 12.9797 7.85953 12.6406 7.60949 12.3905C7.35944 " +
        "12.1405 7.0203 12 6.66668 12C6.31305 12 5.97392 12.1405 5.72387 12.3905C5.47382 12.6406 " +
        "5.33334 12.9797 5.33334 13.3333V14.6667H2.66668C2.31305 14.6667 1.97392 14.5262 1.72387 " +
        "14.2762C1.47382 14.0261 1.33334 13.687 1.33334 13.3333V10.6667H2.66668C3.0203 10.6667 " +
        "3.35944 10.5262 3.60949 10.2762C3.85953 10.0261 4.00001 9.68697 4.00001 9.33334C4.00001 " +
        "8.97972 3.85953 8.64058 3.60949 8.39053C3.35944 8.14049 3.0203 8.00001 2.66668 8.00001H" +
        "1.33334V5.33334C1.33334 4.97972 1.47382 4.64058 1.72387 4.39053C1.97392 4.14049 2.31305 " +
        "4.00001 2.66668 4.00001H5.33334V2.66668Z"

    // Frame (3).svg — two overlapping cards (viewBox 0 0 22 22)
    const val CARDS_FRONT =
        "M14.6667 6.41669H3.66668C2.65415 6.41669 1.83334 7.2375 1.83334 8.25002V15.5834C1.83334 " +
        "16.5959 2.65415 17.4167 3.66668 17.4167H14.6667C15.6792 17.4167 16.5 16.5959 16.5 " +
        "15.5834V8.25002C16.5 7.2375 15.6792 6.41669 14.6667 6.41669Z"
    const val CARDS_BACK  =
        "M5.5 6.41667V4.58333C5.5 4.0971 5.69315 3.63079 6.03697 3.28697C6.38079 2.94315 6.8471 " +
        "2.75 7.33333 2.75H18.3333C18.8196 2.75 19.2859 2.94315 19.6297 3.28697C19.9735 3.63079 " +
        "20.1667 4.0971 20.1667 4.58333V13.75C20.1667 14.2362 19.9735 14.7025 19.6297 15.0464C" +
        "19.2859 15.3902 18.8196 15.5833 18.3333 15.5833H16.5"

    // Frame (4).svg — trophy / achievement (viewBox 0 0 24 24)
    const val TROPHY_HEAD =
        "M12 15C13.8565 15 15.637 14.2625 16.9497 12.9497C18.2625 11.637 19 9.85652 19 8C19 " +
        "6.14348 18.2625 4.36301 16.9497 3.05025C15.637 1.7375 13.8565 1 12 1C10.1435 1 8.36301 " +
        "1.7375 7.05025 3.05025C5.7375 4.36301 5 6.14348 5 8C5 9.85652 5.7375 11.637 7.05025 " +
        "12.9497C8.36301 14.2625 10.1435 15 12 15Z"
    const val TROPHY_BASE = "M12 18V21M8 21H16"
    const val TROPHY_STAR =
        "M12 5L13.5 8L17 8.5L14.5 11L15 14.5L12 13L9 14.5L9.5 11L7 8.5L10.5 8L12 5Z"

    // Vector.svg — right arrow (viewBox 0 0 22 20)
    const val ARROW_RIGHT =
        "M0.75 9.75H20.75M20.75 9.75L10.75 0.75M20.75 9.75L10.75 18.75"
}

// ─────────────────────────────────────────────
//  Main entry point
// ─────────────────────────────────────────────
@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToSmartSession: () -> Unit = {},
    onNavigateToWordFall: () -> Unit = {},
    onNavigateToDungeon: () -> Unit = {},
    onNavigateToTest: () -> Unit = {},
) {
    val viewModel = remember { HomeViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullRefresh(
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize().background(BgPage),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeHeader(state)

            ProgressCard(state)

            SmartSessionCard(
                reviewCount = state.reviewWordCount,
                onClick = onNavigateToSmartSession,
            )

            ActivitySection(
                state = state,
                onStudy = onNavigateToStudy,
                onWordFall = onNavigateToWordFall,
                onDungeon = onNavigateToDungeon,
                onTest = onNavigateToTest,
            )

            StatsCard(state)

            XpProgressCard(state)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────
//  1. Header
// ─────────────────────────────────────────────
@Composable
private fun HomeHeader(state: HomeUiState) {
    val greeting = when (state.greetingTime) {
        "morning" -> "Доброе утро"
        "evening" -> "Добрый вечер"
        "night"   -> "Доброй ночи"
        else      -> "Добрый день"
    }
    val greetingText = if (state.userName != null) "$greeting, ${state.userName}" else greeting

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(greetingText, fontSize = 14.sp, color = FgSecondary)
            Text(
                text = "Главная",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
        }

        if (state.currentLevel > 0) {
            Box(
                modifier = Modifier
                    .border(1.dp, Fg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Ур. ${state.currentLevel}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  2. Progress ring + week tracker
// ─────────────────────────────────────────────
@Composable
private fun ProgressCard(state: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProgressRing(state)
            if (state.weekDays.isNotEmpty()) {
                WeekTracker(state.weekDays)
            }
        }
    }
}

@Composable
private fun ProgressRing(state: HomeUiState) {
    val progress = state.dailyGoalProgress
    val animated by animateFloatAsState(
        targetValue = progress.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(800),
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokePx = 7.dp.toPx()
            val radius   = (size.minDimension - strokePx) / 2f
            val center   = Offset(size.width / 2f, size.height / 2f)
            val topLeft  = Offset(center.x - radius, center.y - radius)
            val arcSize  = Size(radius * 2f, radius * 2f)

            // Soft glow — only around the ring track, transparent inside the circle
            // Ring track sits at fraction ≈ 0.685 of glowRadius; keep center clear.
            val glowRadius = radius * 1.46f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.58f to Color.Transparent,
                        0.70f to Color(0xFF1D1D1F).copy(alpha = 0.11f),
                        0.82f to Color(0xFF1D1D1F).copy(alpha = 0.05f),
                        1.00f to Color.Transparent,
                    ),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )

            // Track ring
            drawCircle(
                color  = FgMuted.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style  = Stroke(strokePx),
            )
            // Fill arc
            if (animated > 0f) {
                drawArc(
                    color      = Fg,
                    startAngle = -90f,
                    sweepAngle = animated * 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
        }

        // Ring is 120dp, stroke 7dp → inner diameter ≈ 106dp.
        // Cap text width so it can never overflow the stroke (8dp clearance each side).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 90.dp),
        ) {
            val progressText = "${progress.wordsStudied}/${progress.goal}"
            // Scale number font down as the string grows so it never overflows the ring.
            val numberFontSize = when {
                progressText.length <= 4 -> 30.sp   // "9/10"
                progressText.length <= 5 -> 26.sp   // "99/10"
                progressText.length <= 6 -> 22.sp   // "290/10"
                progressText.length <= 7 -> 19.sp   // "999/100"
                progressText.length <= 8 -> 17.sp   // "9999/100"
                else                     -> 15.sp
            }
            Text(
                text       = progressText,
                fontSize   = numberFontSize,
                fontWeight = FontWeight.Bold,
                color      = Fg,
                maxLines   = 1,
                softWrap   = false,
            )
            Text(
                text     = "слов сегодня",
                fontSize = 12.sp,
                color    = FgSecondary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun WeekTracker(weekDays: List<WeekDay>) {
    Row(
        modifier               = Modifier.fillMaxWidth(),
        horizontalArrangement  = Arrangement.SpaceEvenly,
    ) {
        weekDays.forEach { day ->
            Column(
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text       = day.label,
                    fontSize   = 11.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color      = when {
                        day.isToday  -> Fg
                        day.isActive -> FgSecondary
                        else         -> FgMuted
                    },
                )
                val filled = day.isActive || day.isToday
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.isToday  -> DotToday
                                day.isActive -> DotActive
                                else         -> Color.Transparent
                            }
                        )
                        .then(
                            if (!filled) Modifier.border(1.dp, FgMuted, CircleShape)
                            else Modifier
                        ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  3. Smart session card (dark background)
// ─────────────────────────────────────────────
@Composable
private fun SmartSessionCard(reviewCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Fg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Умная сессия",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
            if (reviewCount > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = "$reviewCount ${wordsWord(reviewCount)} к повторению",
                    fontSize = 13.sp,
                    color    = FgSecondary,
                )
            }
        }
        ArrowIcon(modifier = Modifier.size(22.dp), color = FgSecondary)
    }
}

// ─────────────────────────────────────────────
//  4. Activity section (tabs + content)
// ─────────────────────────────────────────────
private enum class HomeTab { STUDY, TEST, GAMES }

@Composable
private fun ActivitySection(
    state: HomeUiState,
    onStudy: () -> Unit,
    onWordFall: () -> Unit,
    onDungeon: () -> Unit,
    onTest: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.STUDY) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Tab row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple(HomeTab.STUDY, "Учёба",  @Composable { mod: Modifier, c: Color -> GraduationIcon(mod, c) }),
                Triple(HomeTab.TEST,  "Тест",   @Composable { mod: Modifier, c: Color -> CardsIcon(mod, c) }),
                Triple(HomeTab.GAMES, "Игры",   @Composable { mod: Modifier, c: Color -> PuzzleIcon(mod, c) }),
            ).forEach { (tab, label, icon) ->
                val selected = selectedTab == tab
                val iconColor = if (selected) Color.White else Fg
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Fg else BgCard)
                        .border(
                            width = if (selected) 0.dp else 1.dp,
                            color = FgMuted.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    icon(Modifier.size(16.dp), iconColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = label,
                        fontSize   = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (selected) Color.White else Fg,
                    )
                }
            }
        }

        // Tab content
        AnimatedContent(
            targetState  = selectedTab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        ) { tab ->
            when (tab) {
                HomeTab.STUDY -> StudyTabContent(state, onStudy)
                HomeTab.TEST  -> TestTabContent(onTest)
                HomeTab.GAMES -> GamesTabContent(onWordFall, onDungeon)
            }
        }
    }
}

@Composable
private fun StudyTabContent(state: HomeUiState, onStudy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .clickable(onClick = onStudy)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp),
        ) {
            Text(
                text       = "Учить новые",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Fg,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "${state.reviewWordCount} ${wordsWord(state.reviewWordCount)}",
                fontSize = 13.sp,
                color    = FgSecondary,
            )
            Spacer(Modifier.height(10.dp))
            // Progress bar
            val fraction = state.dailyGoalProgress.progressFraction.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FgMuted.copy(alpha = 0.3f)),
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(ProgressBlue),
                    )
                }
            }
        }
        // Cards icon top-right
        CardsIcon(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd),
            color    = FgSecondary,
        )
    }
}

@Composable
private fun TestTabContent(onTest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .clickable(onClick = onTest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardsIcon(modifier = Modifier.size(20.dp), color = FgSecondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Тест",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Fg,
            )
            Text(
                text     = "Проверь знание слов",
                fontSize = 13.sp,
                color    = FgSecondary,
            )
        }
        ArrowIcon(modifier = Modifier.size(22.dp), color = FgMuted)
    }
}

@Composable
private fun GamesTabContent(onWordFall: () -> Unit, onDungeon: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("Словопад", "Угадывай падающие слова", onWordFall),
            Triple("Подземелье", "Рогалик с карточками слов", onDungeon),
        ).forEach { (title, subtitle, onClick) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PuzzleIcon(modifier = Modifier.size(20.dp), color = FgSecondary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Fg)
                    Text(subtitle, fontSize = 13.sp, color = FgSecondary)
                }
                ArrowIcon(modifier = Modifier.size(22.dp), color = FgMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────
//  5. Stats card (3 columns)
// ─────────────────────────────────────────────
@Composable
private fun StatsCard(state: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell(value = "${state.wordsMastered}", label = "выучено")
        StatDivider()
        StatCell(value = "${state.weeklyAccuracy}%", label = "точность")
        StatDivider()
        StatCell(value = "${state.weeklyTimeMinutes}м", label = "за неделю")
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = Fg,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = label,
            fontSize = 11.sp,
            color    = FgSecondary,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(FgMuted.copy(alpha = 0.4f)),
    )
}

// ─────────────────────────────────────────────
//  6. XP progress card
// ─────────────────────────────────────────────
@Composable
private fun XpProgressCard(state: HomeUiState) {
    val (xpCurrent, xpNeeded) = state.xpProgress
    val remaining = (xpNeeded - xpCurrent).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text     = "Ещё $remaining ${wordsWord(remaining)} до нового уровня",
            fontSize = 13.sp,
            color    = FgSecondary,
        )
        TrophyIcon(modifier = Modifier.size(22.dp))
    }
}

// ─────────────────────────────────────────────
//  SVG icon composables (drawn via PathParser)
// ─────────────────────────────────────────────
@Composable
private fun GraduationIcon(modifier: Modifier = Modifier, color: Color = Fg) {
    val path1 = remember { PathParser().parsePathString(Icons.GRAD_LINE).toPath() }
    val path2 = remember { PathParser().parsePathString(Icons.GRAD_ARC).toPath() }
    Canvas(modifier = modifier) {
        val sx = size.width / 16f
        val sy = size.height / 16f
        val stroke = Stroke(1.33f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(sx, sy, Offset.Zero) {
            drawPath(path1, color, style = stroke)
            drawPath(path2, color, style = stroke)
        }
    }
}

@Composable
private fun CardsIcon(modifier: Modifier = Modifier, color: Color = Fg) {
    val path1 = remember { PathParser().parsePathString(Icons.CARDS_FRONT).toPath() }
    val path2 = remember { PathParser().parsePathString(Icons.CARDS_BACK).toPath() }
    Canvas(modifier = modifier) {
        val sx = size.width / 22f
        val sy = size.height / 22f
        val stroke = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(sx, sy, Offset.Zero) {
            drawPath(path1, color, style = stroke)
            drawPath(path2, color, style = stroke)
        }
    }
}

@Composable
private fun PuzzleIcon(modifier: Modifier = Modifier, color: Color = Fg) {
    val path = remember { PathParser().parsePathString(Icons.PUZZLE).toPath() }
    Canvas(modifier = modifier) {
        val sx = size.width / 16f
        val sy = size.height / 16f
        scale(sx, sy, Offset.Zero) {
            drawPath(path, color, style = Stroke(1.2f, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun TrophyIcon(modifier: Modifier = Modifier, color: Color = FgSecondary) {
    val head = remember { PathParser().parsePathString(Icons.TROPHY_HEAD).toPath() }
    val base = remember { PathParser().parsePathString(Icons.TROPHY_BASE).toPath() }
    val star = remember { PathParser().parsePathString(Icons.TROPHY_STAR).toPath() }
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        scale(sx, sy, Offset.Zero) {
            drawPath(head, color, style = Stroke(1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(base, color, style = Stroke(1.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(star, color, style = Stroke(0.9f, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun ArrowIcon(modifier: Modifier = Modifier, color: Color = FgSecondary) {
    val path = remember { PathParser().parsePathString(Icons.ARROW_RIGHT).toPath() }
    Canvas(modifier = modifier) {
        val sx = size.width / 22f
        val sy = size.height / 20f
        scale(sx, sy, Offset.Zero) {
            drawPath(path, color, style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ─────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────
private fun wordsWord(count: Int): String = when {
    count % 100 in 11..19 -> "слов"
    count % 10 == 1        -> "слово"
    count % 10 in 2..4     -> "слова"
    else                   -> "слов"
}
