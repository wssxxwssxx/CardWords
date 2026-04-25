package com.example.cardwords.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.packs.WordPackRegistry
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

// ─────────────────────────────────────────────
//  SVG paths (viewBox 20×20)
// ─────────────────────────────────────────────
private object SettingsIconPaths {
    // Book — Frame (11)
    val BOOK_COVER = "M15 2H5C3.89543 2 3 2.89543 3 4V16C3 17.1046 3.89543 18 5 18H15C16.1046 18 17 17.1046 17 16V4C17 2.89543 16.1046 2 15 2Z"
    val BOOK_SPINE = "M7 2V18"
    val BOOK_L1    = "M10 7H14"
    val BOOK_L2    = "M10 11H14"
    // Numbered list — Frame (12)
    val LIST_L1    = "M8 5H17"
    val LIST_L2    = "M8 10H17"
    val LIST_L3    = "M8 15H17"
    val LIST_C1A   = "M3 5L4 6"
    val LIST_C1B   = "M4 6L6 3.5"
    val LIST_C2A   = "M3 10L4 11"
    val LIST_C2B   = "M4 11L6 8.5"
    val LIST_CIRC  = "M4.49995 16.8C5.49406 16.8 6.29995 15.9941 6.29995 15C6.29995 14.0059 5.49406 13.2 4.49995 13.2C3.50584 13.2 2.69995 14.0059 2.69995 15C2.69995 15.9941 3.50584 16.8 4.49995 16.8Z"
}

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyWordSettingsScreen(
    multipleChoice: Boolean,
    flashcard: Boolean,
    typing: Boolean,
    letterAssembly: Boolean,
    onStartStudy: (wordCount: Int, wordSource: String, wordIds: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedWordCount    by remember { mutableStateOf(0) }
    var selectedSource       by remember { mutableStateOf("") }
    var selectedWordIds      by remember { mutableStateOf(setOf<Long>()) }
    var isSourceExpanded     by remember { mutableStateOf(false) }
    var isWordPickerExpanded by remember { mutableStateOf(false) }

    val repository           = AppModule.databaseRepository
    val totalDictionaryCount = remember { repository.getDictionaryWordCount() }
    val availablePacks       = remember {
        WordPackRegistry.allPacks.filter { repository.getDictionaryWordCountBySource(it.sourceTag) > 0 }
    }
    val packWordCounts = remember(availablePacks) {
        availablePacks.associate { it.sourceTag to repository.getDictionaryWordCountBySource(it.sourceTag) }
    }
    val sourceWords = remember(selectedSource) {
        if (selectedSource.isEmpty()) repository.getDictionaryWords()
        else repository.getDictionaryWordsBySource(selectedSource)
    }
    val sourceWordCount = sourceWords.size.toLong()
    val effectiveCount  = when {
        selectedWordIds.isNotEmpty() -> selectedWordIds.size.toLong()
        selectedWordCount == 0       -> sourceWordCount
        else                         -> minOf(selectedWordCount.toLong(), sourceWordCount)
    }

    val modeNames = buildList {
        if (multipleChoice) add("Тест")
        if (flashcard)      add("Карточки")
        if (typing)         add("Ввод")
        if (letterAssembly) add("Сборка")
    }

    val currentSourceLabel = when {
        selectedSource.isEmpty() -> "Все слова"
        else -> availablePacks.firstOrNull { it.sourceTag == selectedSource }
            ?.let { "${it.emoji} ${it.title}" } ?: "Все слова"
    }
    val currentSourceCount = when {
        selectedSource.isEmpty() -> totalDictionaryCount
        else -> packWordCounts[selectedSource] ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .safeContentPadding(),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Header ───────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val s = size.width / 24f
                    drawPath(
                        path = Path().apply {
                            moveTo(14f * s, 5f * s)
                            lineTo(8f * s, 12f * s)
                            lineTo(14f * s, 19f * s)
                        },
                        color = LightFg,
                        style = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "Настройки сессии",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Scrollable body ───────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {

            // ── РЕЖИМЫ ───────────────────────────────
            SectionLabel("РЕЖИМЫ")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
            ) {
                modeNames.forEach { name ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(LightFg)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── КОЛИЧЕСТВО СЛОВ ───────────────────────
            SectionLabel("КОЛИЧЕСТВО СЛОВ")
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightCard),
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Шаг: 1 если слов ≤20, иначе 5
                    val step = if (sourceWordCount <= 20L) 1 else 5

                    // Minus — не уходим ниже step (не сбрасываем в "Все")
                    val minusEnabled = selectedWordCount > step && selectedWordIds.isEmpty()
                    Text(
                        text     = "−",
                        fontSize = 28.sp,
                        color    = if (minusEnabled) LightFg else LightFgMuted,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = minusEnabled) {
                                selectedWordCount = maxOf(step, selectedWordCount - step)
                            }
                            .padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    // Count
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = when {
                                selectedWordIds.isNotEmpty() -> "${selectedWordIds.size}"
                                selectedWordCount == 0       -> "Все"
                                else                         -> "$selectedWordCount"
                            },
                            fontSize   = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color      = LightFg,
                        )
                        Text(
                            text     = "из $sourceWordCount доступных",
                            fontSize = 13.sp,
                            color    = LightFgSecondary,
                        )
                    }

                    // Plus — из "Все" → step; при переполнении → "Все"
                    val plusEnabled = selectedWordIds.isEmpty() && selectedWordCount < sourceWordCount
                    Text(
                        text     = "+",
                        fontSize = 28.sp,
                        color    = if (plusEnabled) LightFg else LightFgMuted,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = plusEnabled) {
                                val next = if (selectedWordCount == 0) step
                                           else selectedWordCount + step
                                selectedWordCount = if (next >= sourceWordCount) 0 else next
                            }
                            .padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                // Bottom separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LightFgMuted.copy(alpha = 0.25f)),
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── ИСТОЧНИК ─────────────────────────────
            SectionLabel("ИСТОЧНИК")
            Spacer(Modifier.height(8.dp))
            Column {
                // Main source row — нижние углы квадратные когда раскрыто
                val sourceHeaderShape = if (isSourceExpanded)
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                else
                    RoundedCornerShape(14.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(sourceHeaderShape)
                        .background(LightCard)
                        .clickable { isSourceExpanded = !isSourceExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier         = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(LightFgMuted.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { BookIcon() }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text     = "$currentSourceLabel  $currentSourceCount",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color    = LightFg,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text     = ">",
                        fontSize = 16.sp,
                        color    = LightFgSecondary,
                        modifier = Modifier.scale(scaleX = if (isSourceExpanded) -1f else 1f, scaleY = 1f),
                    )
                }
                // Expandable chips — верхние углы квадратные, нижние скруглённые
                AnimatedVisibility(
                    visible = isSourceExpanded,
                    enter   = expandVertically(expandFrom = androidx.compose.ui.Alignment.Top),
                    exit    = shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Top),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(LightCard)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(LightFgMuted.copy(alpha = 0.3f))
                        )
                        Spacer(Modifier.height(2.dp))
                        SourceRow(
                            label      = "Все слова ($totalDictionaryCount)",
                            isSelected = selectedSource == "",
                            onClick    = {
                                selectedSource = ""
                                selectedWordIds = emptySet()
                                isSourceExpanded = false
                            },
                        )
                        availablePacks.forEach { pack ->
                            val cnt = packWordCounts[pack.sourceTag] ?: 0
                            SourceRow(
                                label      = "${pack.emoji} ${pack.title} ($cnt)",
                                isSelected = selectedSource == pack.sourceTag,
                                onClick    = {
                                    selectedSource = pack.sourceTag
                                    selectedWordIds = emptySet()
                                    isSourceExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── ВЫБОР СЛОВ ───────────────────────────
            SectionLabel("ВЫБОР СЛОВ")
            Spacer(Modifier.height(8.dp))
            Column {
                val pickerHeaderShape = if (isWordPickerExpanded)
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                else
                    RoundedCornerShape(14.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pickerHeaderShape)
                        .background(LightCard)
                        .clickable {
                            isWordPickerExpanded = !isWordPickerExpanded
                            if (!isWordPickerExpanded) selectedWordIds = emptySet()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier         = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(LightFgMuted.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { ListIcon() }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text     = if (selectedWordIds.isNotEmpty())
                            "Выбрано: ${selectedWordIds.size} слов"
                        else
                            "Выбрать конкретные слова",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color      = LightFg,
                        modifier   = Modifier.weight(1f),
                    )
                    Text(
                        text     = ">",
                        fontSize = 16.sp,
                        color    = LightFgSecondary,
                        modifier = Modifier.scale(scaleX = if (isWordPickerExpanded) -1f else 1f, scaleY = 1f),
                    )
                }
                AnimatedVisibility(
                    visible = isWordPickerExpanded,
                    enter   = expandVertically(expandFrom = androidx.compose.ui.Alignment.Top),
                    exit    = shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Top),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(LightCard)
                            .heightIn(max = 260.dp),
                    ) {
                        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                            items(items = sourceWords, key = { it.id }) { word ->
                                WordPickerItem(
                                    word       = word,
                                    isSelected = word.id in selectedWordIds,
                                    onToggle   = {
                                        selectedWordIds = if (word.id in selectedWordIds)
                                            selectedWordIds - word.id
                                        else
                                            selectedWordIds + word.id
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // ── Bottom ────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text     = "${modeNames.size} ${modeText(modeNames.size)} · $effectiveCount слов",
                fontSize = 14.sp,
                color    = LightFgSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (effectiveCount > 0) LightFg else LightFgMuted.copy(alpha = 0.4f))
                    .clickable(enabled = effectiveCount > 0) {
                        val idsString = if (selectedWordIds.isNotEmpty())
                            selectedWordIds.joinToString(",") else ""
                        onStartStudy(selectedWordCount, selectedSource, idsString)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Начать обучение",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
        }
    }
}

private fun modeText(count: Int) = when {
    count % 10 == 1 && count % 100 != 11 -> "режим"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "режима"
    else -> "режимов"
}

// ─────────────────────────────────────────────
//  Small components
// ─────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = LightFgSecondary,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun SourceRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LightFg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color    = if (isSelected) Color.White else LightFg,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Text("✓", fontSize = 14.sp, color = Color.White)
        }
    }
}

@Composable
private fun WordPickerItem(word: Word, isSelected: Boolean, onToggle: () -> Unit) {
    val checkScale by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(150),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) LightProgressBar else Color.Transparent)
                .border(1.5.dp, if (isSelected) LightProgressBar else LightFgMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color    = Color.White,
                modifier = Modifier.scale(checkScale),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = word.original,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color    = LightFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text     = word.translation,
                fontSize = 13.sp,
                color    = LightFgSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Icon composables
// ─────────────────────────────────────────────
@Composable
private fun BookIcon() {
    val c = LightFgSecondary
    Canvas(Modifier.size(18.dp)) {
        val s  = size.width / 20f
        val sw = Stroke(1.5f)
        val sw2 = Stroke(1.2f, cap = StrokeCap.Round)
        svgPath20(SettingsIconPaths.BOOK_COVER, s) { drawPath(it, c, style = sw) }
        svgPath20(SettingsIconPaths.BOOK_SPINE, s) { drawPath(it, c, style = sw) }
        svgPath20(SettingsIconPaths.BOOK_L1,    s) { drawPath(it, c, style = sw2) }
        svgPath20(SettingsIconPaths.BOOK_L2,    s) { drawPath(it, c, style = sw2) }
    }
}

@Composable
private fun ListIcon() {
    val c = LightFgSecondary
    Canvas(Modifier.size(18.dp)) {
        val s  = size.width / 20f
        val sw = Stroke(1.5f, cap = StrokeCap.Round)
        val sw2 = Stroke(1.2f, cap = StrokeCap.Round)
        listOf(
            SettingsIconPaths.LIST_L1, SettingsIconPaths.LIST_L2, SettingsIconPaths.LIST_L3,
            SettingsIconPaths.LIST_C1A, SettingsIconPaths.LIST_C1B,
            SettingsIconPaths.LIST_C2A, SettingsIconPaths.LIST_C2B,
        ).forEach { d -> svgPath20(d, s) { drawPath(it, c, style = sw) } }
        svgPath20(SettingsIconPaths.LIST_CIRC, s) { drawPath(it, c, style = sw2) }
    }
}

private inline fun DrawScope.svgPath20(
    pathData: String,
    s: Float,
    crossinline draw: DrawScope.(androidx.compose.ui.graphics.Path) -> Unit,
) {
    val path = PathParser().parsePathString(pathData).toPath()
    scale(s, s, pivot = Offset.Zero) { draw(path) }
}
