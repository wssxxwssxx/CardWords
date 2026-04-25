package com.example.cardwords.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.MasteryBreakdown
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.WeeklySummary
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.Amber40
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar
import com.example.cardwords.ui.theme.Orange40
import com.example.cardwords.ui.theme.Red40
import com.example.cardwords.util.DateUtil

// ─────────────────────────────────────────────
//  Design tokens — all from Theme.kt
// ─────────────────────────────────────────────
private val BgPage       = LightBg
private val BgCard       = LightCard
private val Fg           = LightFg
private val FgSecondary  = LightFgSecondary
private val FgMuted      = LightFgMuted
private val Accent       = LightFg
private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg      = Color(0xFFF2F2F7)
private val Success      = Green40
private val Failure      = Red40
private val Warn         = Orange40
private val Gold         = Amber40
private val BluePrimary  = LightProgressBar

// ─────────────────────────────────────────────
//  Icons — Canvas, no emojis
// ─────────────────────────────────────────────
private object IconPaths {
    // Flame (streak)
    const val FLAME =
        "M12 2.5C12 2.5 7 7 7 12C7 15.3 9.3 17.5 12 17.5C14.7 17.5 17 15.3 17 12C17 10 16 9 15 9C15 9 16 7 13 4C13 4 13.5 6 11.5 7C10 8 10 10 12 11C10 11 9 10 9 8C9 7 10 6 10 5C10 4 10 3 12 2.5Z"
    // Target (sessions)
    const val TARGET_OUTER = "M12 3C16.9706 3 21 7.0294 21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.0294 7.02944 3 12 3Z"
    const val TARGET_MID   = "M12 7C14.7614 7 17 9.23858 17 12C17 14.7614 14.7614 17 12 17C9.23858 17 7 14.7614 7 12C7 9.23858 9.23858 7 12 7Z"
    const val TARGET_DOT   = "M12 11C12.5523 11 13 11.4477 13 12C13 12.5523 12.5523 13 12 13C11.4477 13 11 12.5523 11 12C11 11.4477 11.4477 11 12 11Z"
    // Check (accuracy)
    const val CHECK = "M4 12L9 17L20 6"
    // Star (mastered)
    const val STAR =
        "M12 2L14.9 8.5L22 9.3L16.5 14.1L18.1 21L12 17.5L5.9 21L7.5 14.1L2 9.3L9.1 8.5L12 2Z"
    // Trophy (achievements)
    const val TROPHY =
        "M7 4H17V9C17 11.7614 14.7614 14 12 14C9.23858 14 7 11.7614 7 9V4Z M12 14V18 M8 22H16 M8 22V18H16V22 M7 5H4V7C4 8.6569 5.34315 10 7 10 M17 5H20V7C20 8.6569 18.6569 10 17 10"
    // Chevron right
    const val CHEVRON_R = "M9 5L16 12L9 19"
    // Chevron left
    const val CHEVRON_L = "M15 5L8 12L15 19"
    // Chevron down
    const val CHEVRON_D = "M5 9L12 16L19 9"
    // Grid (letter)
    const val GRID_TL = "M3 3H10V10H3Z"
    const val GRID_TR = "M14 3H21V10H14Z"
    const val GRID_BL = "M3 14H10V21H3Z"
    const val GRID_BR = "M14 14H21V21H14Z"
    // Pencil (typing)
    const val PENCIL = "M17.5 3.5A2.121 2.121 0 0 1 20.5 6.5L7 20L3 21L4 17L17.5 3.5Z"
    // Cards (flashcard)
    const val CARD = "M7 3H20V19H7Z"
    const val CARD_BACK = "M3 5H16V21H3Z"
    // Chart (empty state)
    const val CHART = "M3 20H21 M7 16V11 M12 16V6 M17 16V13"
}

@Composable
private fun SvgIcon(
    pathString: String,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 1.8f,
    filled: Boolean = false,
) {
    val path = remember(pathString) { PathParser().parsePathString(pathString).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            if (filled) drawPath(path, color)
            else drawPath(
                path, color,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun FlameIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.FLAME, color, modifier, filled = true)

@Composable
private fun TargetIcon(color: Color, modifier: Modifier = Modifier) {
    val outer = remember { PathParser().parsePathString(IconPaths.TARGET_OUTER).toPath() }
    val mid = remember { PathParser().parsePathString(IconPaths.TARGET_MID).toPath() }
    val dot = remember { PathParser().parsePathString(IconPaths.TARGET_DOT).toPath() }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(outer, color, style = stroke)
            drawPath(mid, color, style = stroke)
            drawPath(dot, color)
        }
    }
}

@Composable
private fun CheckIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.CHECK, color, modifier, strokeWidth = 2.2f)

@Composable
private fun StarIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.STAR, color, modifier, filled = true)

@Composable
private fun TrophyIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.TROPHY, color, modifier, strokeWidth = 1.6f)

@Composable
private fun ChevronRightIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.CHEVRON_R, color, modifier, strokeWidth = 2f)

@Composable
private fun ChevronLeftIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.CHEVRON_L, color, modifier, strokeWidth = 2f)

@Composable
private fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.CHEVRON_D, color, modifier, strokeWidth = 2f)

@Composable
private fun ChartIcon(color: Color, modifier: Modifier = Modifier) =
    SvgIcon(IconPaths.CHART, color, modifier, strokeWidth = 2f)

@Composable
private fun StudyModeIcon(mode: String, color: Color, modifier: Modifier = Modifier) {
    when (mode.trim()) {
        "MULTIPLE_CHOICE" -> CheckIcon(color, modifier)
        "FLASHCARD" -> {
            Canvas(modifier) {
                val s = size.width / 24f
                scale(s, s, Offset.Zero) {
                    val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(PathParser().parsePathString(IconPaths.CARD_BACK).toPath(), color, style = stroke)
                }
            }
        }
        "TYPING" -> SvgIcon(IconPaths.PENCIL, color, modifier, strokeWidth = 1.8f)
        "LETTER_ASSEMBLY" -> {
            val tl = remember { PathParser().parsePathString(IconPaths.GRID_TL).toPath() }
            val tr = remember { PathParser().parsePathString(IconPaths.GRID_TR).toPath() }
            val bl = remember { PathParser().parsePathString(IconPaths.GRID_BL).toPath() }
            val br = remember { PathParser().parsePathString(IconPaths.GRID_BR).toPath() }
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
    }
}

// ─────────────────────────────────────────────
//  Main screen
// ─────────────────────────────────────────────
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    showTopBar: Boolean = true,
) {
    val viewModel = remember { StatsViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Info dialog state
    var shownInfo by remember { mutableStateOf<StatInfoContent?>(null) }

    shownInfo?.let { info ->
        InfoDialog(info = info, onDismiss = { shownInfo = null })
    }

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
            if (showTopBar) {
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
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text(
                text = "Статистика",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
        }

        PullRefresh(
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    !uiState.isLoaded -> LoadingState()
                    uiState.totalSessions == 0L -> EmptyStatsContent()
                    else -> {
                        StreakCard(
                            streak = uiState.currentStreak,
                            onClick = { shownInfo = streakInfo(uiState.currentStreak) },
                        )
                        QuickStatsRow(
                            totalSessions = uiState.totalSessions,
                            overallAccuracy = uiState.overallAccuracy,
                            wordsMastered = uiState.wordsMastered,
                            onSessionsClick = { shownInfo = sessionsInfo(uiState.totalSessions) },
                            onAccuracyClick = { shownInfo = accuracyInfo(uiState.overallAccuracy) },
                            onMasteredClick = { shownInfo = masteredInfo(uiState.wordsMastered) },
                        )
                        WeeklySummaryCard(
                            summary = uiState.weeklySummary,
                            onClick = { shownInfo = weeklyInfo(uiState.weeklySummary) },
                        )
                        AchievementsQuickCard(
                            unlockedCount = uiState.unlockedAchievementsCount,
                            totalCount = uiState.totalAchievementsCount,
                            onClick = {
                                shownInfo = achievementsInfo(
                                    uiState.unlockedAchievementsCount,
                                    uiState.totalAchievementsCount,
                                    onOpen = onNavigateToAchievements,
                                )
                            },
                        )
                        ActivityHeatmapCard(
                            heatmapData = uiState.heatmapData,
                            startDate = uiState.heatmapStartDate,
                            endDate = uiState.heatmapEndDate,
                            onClick = { shownInfo = heatmapInfo() },
                        )
                        if (uiState.masteryBreakdown.total > 0) {
                            MasteryDistributionCard(
                                breakdown = uiState.masteryBreakdown,
                                onClick = { shownInfo = masteryInfo() },
                            )
                        }
                        if (uiState.recentSessions.isNotEmpty()) {
                            RecentSessionsCard(sessions = uiState.recentSessions)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Loading / Empty
// ─────────────────────────────────────────────
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = "Загрузка…",
            fontSize = 14.sp,
            color = FgSecondary,
        )
    }
}

@Composable
private fun EmptyStatsContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCard)
                    .border(0.5.dp, DividerColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ChartIcon(FgMuted, Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Пока нет данных",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Пройдите хотя бы одну сессию\nчтобы увидеть статистику",
                fontSize = 14.sp,
                color = FgSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Streak Card
// ─────────────────────────────────────────────
@Composable
private fun StreakCard(streak: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Warn.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            FlameIcon(color = Warn, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$streak ${streakDaysWord(streak)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
            Text(
                text = if (streak > 0) "Отличная серия" else "Начните учить, чтобы начать серию",
                fontSize = 13.sp,
                color = FgSecondary,
            )
        }
    }
}

private fun streakDaysWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}

// ─────────────────────────────────────────────
//  Quick Stats
// ─────────────────────────────────────────────
@Composable
private fun QuickStatsRow(
    totalSessions: Long,
    overallAccuracy: Float,
    wordsMastered: Int,
    onSessionsClick: () -> Unit,
    onAccuracyClick: () -> Unit,
    onMasteredClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MiniStatCard(
            iconContent = { TargetIcon(BluePrimary, Modifier.size(18.dp)) },
            value = totalSessions.toString(),
            label = "Сессий",
            onClick = onSessionsClick,
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            iconContent = { CheckIcon(Success, Modifier.size(18.dp)) },
            value = "${(overallAccuracy * 100).toInt()}%",
            label = "Точность",
            onClick = onAccuracyClick,
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            iconContent = { StarIcon(Gold, Modifier.size(18.dp)) },
            value = wordsMastered.toString(),
            label = "Выучено",
            onClick = onMasteredClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStatCard(
    iconContent: @Composable () -> Unit,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 14.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        iconContent()
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Fg,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = FgSecondary,
        )
    }
}

// ─────────────────────────────────────────────
//  Weekly Summary
// ─────────────────────────────────────────────
@Composable
private fun WeeklySummaryCard(summary: WeeklySummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            text = "Итоги недели",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            WeekStat(
                value = summary.thisWeekWords.toString(),
                label = "Эта неделя",
                accent = Fg,
            )
            WeekStat(
                value = summary.lastWeekWords.toString(),
                label = "Прошлая",
                accent = FgSecondary,
            )
            if (summary.changePercent != null) {
                val isPositive = summary.changePercent >= 0
                WeekStat(
                    value = "${if (isPositive) "+" else ""}${summary.changePercent}%",
                    label = "Изменение",
                    accent = if (isPositive) Success else Warn,
                )
            }
        }
    }
}

@Composable
private fun WeekStat(value: String, label: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = FgSecondary,
        )
    }
}

// ─────────────────────────────────────────────
//  Achievements Quick Card (clickable)
// ─────────────────────────────────────────────
@Composable
private fun AchievementsQuickCard(
    unlockedCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            TrophyIcon(color = Gold, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Достижения",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
            )
            Text(
                text = "$unlockedCount / $totalCount открыто",
                fontSize = 12.sp,
                color = FgSecondary,
            )
        }
        ChevronRightIcon(color = FgMuted, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────────────────────────
//  Activity Heatmap
// ─────────────────────────────────────────────
@Composable
private fun ActivityHeatmapCard(
    heatmapData: List<DailyActivity>,
    startDate: String,
    endDate: String,
    onClick: () -> Unit,
) {
    val activityMap = remember(heatmapData) {
        heatmapData.associate { it.date to it.sessionsCount }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            text = "Активность",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )
        Spacer(Modifier.height(14.dp))
        HeatmapGrid(startDate = startDate, endDate = endDate, activityMap = activityMap)
        Spacer(Modifier.height(10.dp))
        HeatmapLegend()
    }
}

private fun heatmapColor(sessions: Int): Color {
    // Use iOS-blue accent with varying opacity — fits the light palette
    return when {
        sessions == 0 -> Color(0xFFEDEDF0)
        sessions == 1 -> BluePrimary.copy(alpha = 0.25f)
        sessions == 2 -> BluePrimary.copy(alpha = 0.50f)
        sessions <= 4 -> BluePrimary.copy(alpha = 0.75f)
        else -> BluePrimary
    }
}

/**
 * Heatmap — single Canvas that batches all 91 cells into one draw pass.
 * Replaces 13×7 nested Column/Row/Box which created ~105 layout nodes and
 * ~91 DrawModifier instances, each allocating color + clip state.
 *
 * Skia coalesces the `drawRoundRect` calls into a single batch on iOS/Desktop.
 */
@Composable
private fun HeatmapGrid(
    startDate: String,
    endDate: String,
    activityMap: Map<String, Int>,
) {
    // Pre-compute one stable int array of session counts, column-by-column.
    // The whole component skips re-draw when this array is referentially equal.
    val sessionCounts = remember(startDate, endDate, activityMap) {
        val endMillis = dateStringToApproxMillis(endDate)
        val days = (0..90).map { daysBack ->
            DateUtil.daysAgoFromMillis(endMillis, daysBack)
        }.reversed()
        IntArray(days.size) { i -> activityMap[days[i]] ?: 0 }
    }

    val cellSizeDp = 13.dp
    val cellSpacingDp = 3.dp
    val cornerDp = 3.dp

    val totalDays = sessionCounts.size
    val weeks = (totalDays + 6) / 7

    val widthDp = cellSizeDp * weeks + cellSpacingDp * (weeks - 1)
    val heightDp = cellSizeDp * 7 + cellSpacingDp * 6

    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(width = widthDp, height = heightDp),
    ) {
        val cellPx = cellSizeDp.toPx()
        val gapPx = cellSpacingDp.toPx()
        val step = cellPx + gapPx
        val corner = androidx.compose.ui.geometry.CornerRadius(cornerDp.toPx())
        val cellSize = androidx.compose.ui.geometry.Size(cellPx, cellPx)

        for (week in 0 until weeks) {
            for (day in 0 until 7) {
                val index = week * 7 + day
                if (index >= totalDays) continue
                val sessions = sessionCounts[index]
                val color = heatmapColor(sessions)
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(week * step, day * step),
                    size = cellSize,
                    cornerRadius = corner,
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Меньше", fontSize = 10.sp, color = FgSecondary)
        Spacer(Modifier.width(6.dp))
        listOf(0, 1, 2, 3, 5).forEach { level ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(heatmapColor(level)),
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(text = "Больше", fontSize = 10.sp, color = FgSecondary)
    }
}

// ─────────────────────────────────────────────
//  Mastery Distribution
// ─────────────────────────────────────────────
@Composable
private fun MasteryDistributionCard(breakdown: MasteryBreakdown, onClick: () -> Unit) {
    val categories = listOf(
        MasteryCat("Выучено", breakdown.masteredCount, Success),
        MasteryCat("Знаю", breakdown.knownCount, BluePrimary),
        MasteryCat("Учу", breakdown.learningCount, Warn),
        MasteryCat("Новые", breakdown.newCount, FgMuted),
    )
    val total = breakdown.total.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp, clipContent = true)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            text = "Уровни усвоения",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
        )

        Spacer(Modifier.height(14.dp))

        // Segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(FieldBg),
        ) {
            categories.forEach { cat ->
                if (cat.count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(cat.count.toFloat() / total)
                            .height(10.dp)
                            .background(cat.color),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(cat.color),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = cat.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Fg,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = cat.count.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FgSecondary,
                    )
                }
            }
        }
    }
}

private data class MasteryCat(val label: String, val count: Int, val color: Color)

// ─────────────────────────────────────────────
//  Recent Sessions
// ─────────────────────────────────────────────
@Composable
private fun RecentSessionsCard(sessions: List<StudySession>) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(color = BgCard, borderColor = DividerColor, borderWidth = 0.5.dp, cornerRadius = 18.dp)
            .clickable { expanded = !expanded }
            .padding(18.dp),
    ) {
        // Header row — always visible
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Последние сессии",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
                modifier = Modifier.weight(1f),
            )
            // Count pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(FieldBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = sessions.size.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FgSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            // Chevron rotates 180° when expanded
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                contentAlignment = Alignment.Center,
            ) {
                ChevronDownIcon(color = FgMuted, modifier = Modifier.size(14.dp))
            }
        }

        // Expanded list — renders only when opened
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                sessions.forEachIndexed { idx, session ->
                    SessionRow(session = session)
                    if (idx < sessions.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(DividerColor),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: StudySession) {
    val accuracy = if (session.totalCount > 0) {
        (session.correctCount * 100) / session.totalCount
    } else 0
    val accuracyColor = when {
        accuracy >= 70 -> Success
        accuracy >= 40 -> Warn
        else -> Failure
    }

    val dateStr = DateUtil.epochMillisToDateString(session.finishedAt)
    val durationMin = ((session.finishedAt - session.startedAt) / 60000).coerceAtLeast(1)
    val modes = session.modesUsed.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = FgSecondary,
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    modes.forEach { mode ->
                        Box(
                            modifier = Modifier
                                .size(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            StudyModeIcon(mode, FgSecondary, Modifier.size(12.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${session.correctCount}/${session.totalCount} · $durationMin мин",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Fg,
            )
        }

        Text(
            text = "$accuracy%",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = accuracyColor,
        )
    }
}

// ─────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────
// ─────────────────────────────────────────────
//  Info Dialog — shows when a card is tapped
// ─────────────────────────────────────────────
private data class StatInfoContent(
    val title: String,
    val subtitle: String,
    val iconContent: @Composable () -> Unit,
    val accentColor: Color,
    val description: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
private fun InfoDialog(info: StatInfoContent, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(BgCard)
                .border(0.5.dp, DividerColor, RoundedCornerShape(22.dp))
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(info.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                info.iconContent()
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = info.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = info.subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = info.accentColor,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = info.description,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = FgSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(22.dp))

            // Optional action button
            if (info.actionLabel != null && info.onAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Accent)
                        .clickable {
                            info.onAction.invoke()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = info.actionLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BgCard,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FieldBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Закрыть",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Fg,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Info factories — what each stat means
// ─────────────────────────────────────────────
private fun streakInfo(streak: Int): StatInfoContent = StatInfoContent(
    title = "Серия дней",
    subtitle = "$streak ${streakDaysWord(streak)} подряд",
    iconContent = { FlameIcon(color = Warn, modifier = Modifier.size(30.dp)) },
    accentColor = Warn,
    description = "Серия растёт каждый день, когда вы проходите хотя бы одну сессию обучения. " +
        "Пропустите день — серия сбрасывается. " +
        "Занимайтесь регулярно, чтобы удерживать серию и быстрее осваивать слова.",
)

private fun sessionsInfo(total: Long): StatInfoContent = StatInfoContent(
    title = "Всего сессий",
    subtitle = "$total завершено",
    iconContent = { TargetIcon(color = BluePrimary, modifier = Modifier.size(30.dp)) },
    accentColor = BluePrimary,
    description = "Сессия — это отдельный заход на обучение с набором вопросов. " +
        "Короткие частые сессии эффективнее редких долгих: лучше работает интервальное повторение.",
)

private fun accuracyInfo(accuracy: Float): StatInfoContent = StatInfoContent(
    title = "Общая точность",
    subtitle = "${(accuracy * 100).toInt()}% правильных ответов",
    iconContent = { CheckIcon(color = Success, modifier = Modifier.size(30.dp)) },
    accentColor = Success,
    description = "Доля правильных ответов от общего числа попыток во всех режимах. " +
        "Не гонитесь за 100% — ошибки тоже учат. Норма ~70–85%.",
)

private fun masteredInfo(count: Int): StatInfoContent = StatInfoContent(
    title = "Выученные слова",
    subtitle = "$count слов",
    iconContent = { StarIcon(color = Gold, modifier = Modifier.size(30.dp)) },
    accentColor = Gold,
    description = "Слово считается выученным, когда оно достигает максимального уровня " +
        "во всех режимах обучения. После этого оно реже возвращается на повторение.",
)

private fun weeklyInfo(summary: WeeklySummary): StatInfoContent = StatInfoContent(
    title = "Итоги недели",
    subtitle = "${summary.thisWeekWords} слов на этой неделе",
    iconContent = { ChartIcon(color = BluePrimary, modifier = Modifier.size(30.dp)) },
    accentColor = BluePrimary,
    description = "Сравнение количества изученных слов за текущую и прошлую неделю. " +
        "Зелёный процент означает, что вы занимаетесь больше чем раньше — отличный знак прогресса.",
)

private fun achievementsInfo(
    unlocked: Int,
    total: Int,
    onOpen: () -> Unit,
): StatInfoContent = StatInfoContent(
    title = "Достижения",
    subtitle = "$unlocked из $total открыто",
    iconContent = { TrophyIcon(color = Gold, modifier = Modifier.size(30.dp)) },
    accentColor = Gold,
    description = "Достижения открываются за определённые события: длинные серии, высокую точность, " +
        "изучение слов и многое другое. Нажмите «Посмотреть все», чтобы увидеть список.",
    actionLabel = "Посмотреть все",
    onAction = onOpen,
)

private fun heatmapInfo(): StatInfoContent = StatInfoContent(
    title = "Карта активности",
    subtitle = "Последние 13 недель",
    iconContent = { TargetIcon(color = BluePrimary, modifier = Modifier.size(30.dp)) },
    accentColor = BluePrimary,
    description = "Каждая клетка — это день. Цвет показывает, сколько сессий вы провели: " +
        "чем насыщеннее синий, тем активнее день. Пустые клетки — дни без занятий.",
)

private fun masteryInfo(): StatInfoContent = StatInfoContent(
    title = "Уровни усвоения",
    subtitle = "Распределение ваших слов",
    iconContent = { StarIcon(color = Gold, modifier = Modifier.size(30.dp)) },
    accentColor = Gold,
    description = "Слова проходят через 6 уровней: Новое → Видел → Знакомое → Учу → Знаю → Выучено. " +
        "Уровень растёт с каждым правильным ответом и падает при ошибках. " +
        "Ваша цель — перевести как можно больше слов в «Выучено».",
)

/** Approximate conversion from "YYYY-MM-DD" back to epoch millis (UTC midnight) */
private fun dateStringToApproxMillis(dateStr: String): Long {
    val parts = dateStr.split("-")
    if (parts.size != 3) return 0L
    val y = parts[0].toLongOrNull() ?: return 0L
    val m = parts[1].toLongOrNull() ?: return 0L
    val d = parts[2].toLongOrNull() ?: return 0L

    val yr = y - (if (m <= 2) 1 else 0)
    val era = (if (yr >= 0) yr else yr - 399) / 400
    val yoe = yr - era * 400
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097 + doe - 719468
    return days * 86_400_000L
}

