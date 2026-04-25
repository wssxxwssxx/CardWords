package com.example.cardwords.ui.packs

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

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
private val DividerColor = Color(0xFFE5E5EA)
private val TrackColor   = Color(0xFFE5E5EA)
private val IconBgAccent = Color(0xFFF0F0F4)
private val SectionLabel = Color(0xFF8A8A8A)

@Composable
fun WordPacksScreen(
    onNavigateToPackDetail: (String) -> Unit,
    onNavigateToWordSearch: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { WordPacksViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

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
                text = "Наборы слов",
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
        ) {
            SearchWordCard(onClick = onNavigateToWordSearch)

            Spacer(Modifier.height(20.dp))

            if (uiState.byLevel.isNotEmpty()) {
                SectionHeader(title = "По уровню")
                Spacer(Modifier.height(10.dp))
                uiState.byLevel.forEach { overview ->
                    PackCard(
                        overview = overview,
                        onClick = { onNavigateToPackDetail(overview.pack.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.byTopic.isNotEmpty()) {
                SectionHeader(title = "По теме")
                Spacer(Modifier.height(10.dp))
                uiState.byTopic.forEach { overview ->
                    PackCard(
                        overview = overview,
                        onClick = { onNavigateToPackDetail(overview.pack.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.special.isNotEmpty()) {
                SectionHeader(title = "Особые")
                Spacer(Modifier.height(10.dp))
                uiState.special.forEach { overview ->
                    PackCard(
                        overview = overview,
                        onClick = { onNavigateToPackDetail(overview.pack.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionLabel,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun PackCard(
    overview: PackOverview,
    onClick: () -> Unit,
) {
    val pack = overview.pack
    val addedCount = overview.inDictionaryCount.toInt()
    val totalCount = pack.wordCount
    val isComplete = addedCount >= totalCount && totalCount > 0
    val fraction = overview.progress.coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = BgCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 18.dp,
                clipContent = true,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .cardBg(color = IconBgAccent, cornerRadius = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            PackIconFor(packId = pack.id, color = Fg, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.title,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pack.subtitle,
                    fontSize = 12.sp,
                    color = FgSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = FgMuted,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isComplete) "Добавлен" else "$addedCount/$totalCount",
                    fontSize = 12.sp,
                    fontWeight = if (isComplete) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isComplete) Success else FgSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Inline progress track drawn via cardBg — two rounded rects stacked.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .cardBg(color = TrackColor, cornerRadius = 2.dp),
            ) {
                // Filled portion
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(4.dp)
                            .cardBg(
                                color = if (isComplete) Success else BluePrimary,
                                cornerRadius = 2.dp,
                            ),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        ChevronRightIcon(color = FgMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchWordCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = Fg,
                cornerRadius = 18.dp,
                clipContent = true,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .cardBg(color = Color.White.copy(alpha = 0.15f), cornerRadius = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            SearchIcon(color = BgCard, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Найти слово",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BgCard,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Поиск в базе или онлайн",
                fontSize = 12.sp,
                color = BgCard.copy(alpha = 0.7f),
            )
        }
        ChevronRightIcon(color = BgCard.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}
