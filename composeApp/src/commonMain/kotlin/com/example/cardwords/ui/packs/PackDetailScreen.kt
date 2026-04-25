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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
//  Design tokens
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
fun PackDetailScreen(
    packId: String,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember(packId) { PackDetailViewModel(packId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pack = uiState.pack ?: return

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
                text = pack.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Hoisted stable callback for items
        val onAdd: (com.example.cardwords.data.model.PackWord) -> Unit = viewModel::addSingleWord

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 4.dp, bottom = 24.dp,
            ),
        ) {
            // Header: icon + description
            item {
                HeaderCard(pack = pack)
                Spacer(Modifier.height(14.dp))
            }

            // Progress + CTA
            item {
                ProgressCard(
                    addedCount = uiState.addedCount,
                    totalCount = uiState.totalCount,
                    fraction = uiState.progress.coerceIn(0f, 1f),
                    allAdded = uiState.allAdded,
                    isInstalling = uiState.isInstalling,
                    onInstallAll = viewModel::installAll,
                )
                Spacer(Modifier.height(18.dp))
            }

            // Word list header
            item {
                Text(
                    text = "СЛОВА · ${pack.wordCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SectionLabel,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }

            // Word rows
            items(
                items = uiState.words,
                key = { it.packWord.original },
                contentType = { "pack_word" },
            ) { item ->
                WordRow(item = item, onAdd = onAdd)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard(pack: com.example.cardwords.data.model.WordPack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = BgCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 18.dp,
            )
            .padding(18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .cardBg(color = IconBgAccent, cornerRadius = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            PackIconFor(packId = pack.id, color = Fg, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BluePrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pack.description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = FgSecondary,
            )
        }
    }
}

@Composable
private fun ProgressCard(
    addedCount: Int,
    totalCount: Int,
    fraction: Float,
    allAdded: Boolean,
    isInstalling: Boolean,
    onInstallAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = BgCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 18.dp,
            )
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$addedCount / $totalCount",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "слов добавлено",
                fontSize = 13.sp,
                color = FgSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(fraction * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (allAdded) Success else BluePrimary,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Progress track + fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .cardBg(color = TrackColor, cornerRadius = 3.dp),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .cardBg(
                            color = if (allAdded) Success else BluePrimary,
                            cornerRadius = 3.dp,
                        ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // CTA button
        if (allAdded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .cardBg(
                        color = Success.copy(alpha = 0.12f),
                        cornerRadius = 50.dp,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckIcon(color = Success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Все слова добавлены",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Success,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .cardBg(
                        color = if (isInstalling) FgMuted else Fg,
                        cornerRadius = 50.dp,
                        clipContent = true,
                    )
                    .clickable(enabled = !isInstalling, onClick = onInstallAll),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isInstalling) "Добавляем…" else "Добавить все",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BgCard,
                )
            }
        }
    }
}

@Composable
private fun WordRow(
    item: PackWordItem,
    onAdd: (com.example.cardwords.data.model.PackWord) -> Unit,
) {
    val pw = item.packWord
    val added = item.isAdded

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = if (added) Success.copy(alpha = 0.06f) else BgCard,
                borderColor = if (added) Success.copy(alpha = 0.25f) else DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 14.dp,
                clipContent = !added,
            )
            .then(
                if (added) Modifier else Modifier.clickable { onAdd(pw) }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pw.original,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (added) FgSecondary else Fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pw.transcription.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = pw.transcription,
                        fontSize = 11.sp,
                        color = FgMuted,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = pw.translation,
                fontSize = 12.sp,
                color = if (added) FgMuted else FgSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Action: + plus (not added), check (added)
        Box(
            modifier = Modifier
                .size(32.dp)
                .cardBg(
                    color = if (added) Success.copy(alpha = 0.15f) else IconBgAccent,
                    cornerRadius = 50.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (added) {
                CheckIcon(color = Success, modifier = Modifier.size(14.dp))
            } else {
                PlusIcon(color = Fg, modifier = Modifier.size(14.dp))
            }
        }
    }
}
