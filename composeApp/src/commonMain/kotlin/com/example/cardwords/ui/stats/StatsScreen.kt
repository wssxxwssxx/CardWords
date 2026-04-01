package com.example.cardwords.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.DailyActivity
import com.example.cardwords.data.model.MasteryLevels
import com.example.cardwords.data.model.StudySession
import com.example.cardwords.data.model.WeeklySummary
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.AmberLight
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.GreenDark
import com.example.cardwords.ui.theme.Indigo
import com.example.cardwords.ui.theme.IndigoDark
import com.example.cardwords.ui.theme.IndigoMuted
import com.example.cardwords.ui.theme.Orange60
import com.example.cardwords.ui.theme.TextMuted
import com.example.cardwords.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    showTopBar: Boolean = true,
) {
    val viewModel = remember { StatsViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val contentModifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)

    val statsContent: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isLoaded) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Загрузка...")
                }
            } else if (uiState.totalSessions == 0L) {
                // Empty state
                EmptyStatsContent()
            } else {
                // Streak card
                StreakCard(streak = uiState.currentStreak)

                // Quick stats row
                QuickStatsRow(
                    totalSessions = uiState.totalSessions,
                    overallAccuracy = uiState.overallAccuracy,
                    wordsMastered = uiState.wordsMastered,
                )

                // Weekly summary
                WeeklySummaryCard(summary = uiState.weeklySummary)

                // Achievements card
                AchievementsQuickCard(
                    unlockedCount = uiState.unlockedAchievementsCount,
                    totalCount = uiState.totalAchievementsCount,
                    onClick = onNavigateToAchievements,
                )

                // Heatmap
                ActivityHeatmapCard(
                    heatmapData = uiState.heatmapData,
                    startDate = uiState.heatmapStartDate,
                    endDate = uiState.heatmapEndDate,
                )

                // Mastery distribution
                if (uiState.masteryBreakdown.total > 0) {
                    MasteryDistributionCard(
                        breakdown = uiState.masteryBreakdown,
                    )
                }

                // Recent sessions
                if (uiState.recentSessions.isNotEmpty()) {
                    RecentSessionsCard(
                        sessions = uiState.recentSessions,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043A\u0430",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Text(
                                text = "\u2190",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        ) { paddingValues ->
            statsContent(contentModifier.padding(paddingValues))
        }
    } else {
        statsContent(contentModifier)
    }
}

// --- Streak card ---

@Composable
private fun StreakCard(streak: Int) {
    val bgColor = AmberDark
    val textColor = AmberLight

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDD25",
                fontSize = 40.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "$streak ${streakDaysWord(streak)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    text = if (streak > 0) "Отличная серия!" else "Начните учить, чтобы начать серию",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                )
            }
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

// --- Quick stats row ---

@Composable
private fun QuickStatsRow(
    totalSessions: Long,
    overallAccuracy: Float,
    wordsMastered: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniStatCard(
            emoji = "\uD83C\uDFAF",
            value = totalSessions.toString(),
            label = "Сессий",
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            emoji = "\u2705",
            value = "${(overallAccuracy * 100).toInt()}%",
            label = "Точность",
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            emoji = "\u2B50",
            value = wordsMastered.toString(),
            label = "Освоено",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Activity Heatmap ---

@Composable
private fun ActivityHeatmapCard(
    heatmapData: List<DailyActivity>,
    startDate: String,
    endDate: String,
) {
    val activityMap = remember(heatmapData) {
        heatmapData.associate { it.date to it.sessionsCount }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Активность",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 13 weeks × 7 days grid
            HeatmapGrid(
                startDate = startDate,
                endDate = endDate,
                activityMap = activityMap,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            HeatmapLegend()
        }
    }
}

@Composable
private fun HeatmapGrid(
    startDate: String,
    endDate: String,
    activityMap: Map<String, Int>,
) {
    // Generate 91 days of date strings
    // Parse endDate to millis (approximate: just iterate from end date going back)
    val days = remember(startDate, endDate) {
        // Parse endDate back to millis by finding it from epoch
        val endMillis = dateStringToApproxMillis(endDate)
        (0..90).map { daysBack ->
            DateUtil.daysAgoFromMillis(endMillis, daysBack)
        }.reversed()
    }

    // Arrange into weeks (columns) × days (rows)
    // First, figure out the day of week for day 0
    // We'll just fill a 13×7 grid with cells
    val totalDays = days.size
    val weeks = (totalDays + 6) / 7

    val cellSize = 14.dp
    val cellSpacing = 3.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (week in 0 until weeks) {
            Column(
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
            ) {
                for (day in 0 until 7) {
                    val index = week * 7 + day
                    if (index < totalDays) {
                        val dateStr = days[index]
                        val sessions = activityMap[dateStr] ?: 0
                        val color = heatmapColor(sessions)
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color),
                        )
                    } else {
                        Box(modifier = Modifier.size(cellSize))
                    }
                }
            }
            if (week < weeks - 1) {
                Spacer(modifier = Modifier.width(cellSpacing))
            }
        }
    }
}

private fun heatmapColor(sessions: Int): Color {
    return when {
        sessions == 0 -> Color(0xFF1E2530)
        sessions == 1 -> GreenDark
        sessions == 2 -> Color(0xFF006D32)
        sessions <= 4 -> Color(0xFF26A641)
        else -> Green60
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Меньше",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        listOf(0, 1, 2, 3, 5).forEach { level ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(heatmapColor(level)),
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = "Больше",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Achievements Quick Card ---

@Composable
private fun AchievementsQuickCard(
    unlockedCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
) {
    val bgColor = IndigoDark
    val textColor = Indigo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDFC6",
                fontSize = 32.sp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Достижения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )
                Text(
                    text = "$unlockedCount / $totalCount открыто",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                )
            }
            Text(
                text = "\u2192",
                fontSize = 20.sp,
                color = textColor,
            )
        }
    }
}

// --- Mastery Distribution ---

private val masteryColors = listOf(
    TextMuted,     // 0 - New (muted)
    Indigo,        // 1 - Seen (indigo)
    IndigoMuted,   // 2 - Familiar (muted indigo)
    Orange60,      // 3 - Learning (orange)
    Green60,       // 4 - Known (green)
    Amber,         // 5 - Mastered (amber/gold)
)

@Composable
private fun MasteryDistributionCard(
    breakdown: com.example.cardwords.data.model.MasteryBreakdown,
) {
    val categories = listOf(
        Triple("\u041E\u0441\u0432\u043E\u0435\u043D\u043E", breakdown.masteredCount, Green60),
        Triple("\u0417\u043D\u0430\u044E", breakdown.knownCount, Indigo),
        Triple("\u0423\u0447\u0443", breakdown.learningCount, Amber),
        Triple("\u041D\u043E\u0432\u044B\u0435", breakdown.newCount, TextMuted),
    )
    val total = breakdown.total.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "\u0423\u0440\u043E\u0432\u043D\u0438 \u0443\u0441\u0432\u043E\u0435\u043D\u0438\u044F",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                categories.forEach { (_, count, color) ->
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat() / total)
                                .height(24.dp)
                                .background(color),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { (label, count, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(80.dp),
                        )
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// --- Recent Sessions ---

@Composable
private fun RecentSessionsCard(
    sessions: List<StudySession>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Последние сессии",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            sessions.forEach { session ->
                SessionRow(session = session)
                Spacer(modifier = Modifier.height(8.dp))
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
        accuracy >= 70 -> Green60
        accuracy >= 40 -> Orange60
        else -> MaterialTheme.colorScheme.error
    }

    val dateStr = DateUtil.epochMillisToDateString(session.finishedAt)
    val durationMin = ((session.finishedAt - session.startedAt) / 60000).coerceAtLeast(1)

    val modeEmojis = session.modesUsed.split(",").mapNotNull { modeName ->
        when (modeName.trim()) {
            "MULTIPLE_CHOICE" -> "\uD83C\uDFAF"
            "FLASHCARD" -> "\uD83D\uDCCB"
            "TYPING" -> "\u270D\uFE0F"
            "LETTER_ASSEMBLY" -> "\uD83D\uDD24"
            else -> null
        }
    }.joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = modeEmojis,
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${session.correctCount}/${session.totalCount} \u2022 ${durationMin} мин",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = "$accuracy%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accuracyColor,
        )
    }
}

// --- Empty state ---

@Composable
private fun EmptyStatsContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "\uD83D\uDCCA", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Пока нет данных",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Пройдите хотя бы одну сессию обучения,\nчтобы увидеть статистику",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// --- Helpers ---

/** Approximate conversion from "YYYY-MM-DD" back to epoch millis (UTC midnight) */
private fun dateStringToApproxMillis(dateStr: String): Long {
    val parts = dateStr.split("-")
    if (parts.size != 3) return 0L
    val y = parts[0].toLongOrNull() ?: return 0L
    val m = parts[1].toLongOrNull() ?: return 0L
    val d = parts[2].toLongOrNull() ?: return 0L

    // Inverse of Howard Hinnant's civil_from_days (days_from_civil)
    val yr = y - (if (m <= 2) 1 else 0)
    val era = (if (yr >= 0) yr else yr - 399) / 400
    val yoe = yr - era * 400
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097 + doe - 719468
    return days * 86_400_000L
}

// ═══════════════════════════════════════════════════════════════
// Weekly Summary Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WeeklySummaryCard(summary: WeeklySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.example.cardwords.ui.theme.Surface1,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "\u0418\u0442\u043E\u0433\u0438 \u043D\u0435\u0434\u0435\u043B\u0438",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = com.example.cardwords.ui.theme.TextHeading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // This week
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.thisWeekWords}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Amber,
                    )
                    Text(
                        text = "\u044D\u0442\u0430 \u043D\u0435\u0434\u0435\u043B\u044F",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.cardwords.ui.theme.TextSecondary,
                    )
                }

                // Last week
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.lastWeekWords}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.cardwords.ui.theme.TextPrimary,
                    )
                    Text(
                        text = "\u043F\u0440\u043E\u0448\u043B\u0430\u044F",
                        style = MaterialTheme.typography.labelSmall,
                        color = com.example.cardwords.ui.theme.TextSecondary,
                    )
                }

                // Change
                if (summary.changePercent != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isPositive = summary.changePercent >= 0
                        val changeColor = if (isPositive) Green60 else Orange60
                        val prefix = if (isPositive) "+" else ""
                        Text(
                            text = "$prefix${summary.changePercent}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = changeColor,
                        )
                        Text(
                            text = "\u0438\u0437\u043C\u0435\u043D\u0435\u043D\u0438\u0435",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.cardwords.ui.theme.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
