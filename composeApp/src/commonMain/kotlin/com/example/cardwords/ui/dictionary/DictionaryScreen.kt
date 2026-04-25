package com.example.cardwords.ui.dictionary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.study.StudyMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────
//  Design tokens — match welcome/auth screens
// ─────────────────────────────────────────────
private val Black        = Color(0xFF000000)
private val White        = Color(0xFFFFFFFF)
private val PageBg       = Color(0xFFF5F5F7)
private val CardBg       = Color(0xFFFFFFFF)
private val CardBgExp    = Color(0xFFFAFAFC)
private val FieldBg      = Color(0xFFFFFFFF)
private val SubtitleGray = Color(0xFF8A8A8A)
private val LightGray    = Color(0xFFBBBBBB)
private val DividerColor = Color(0xFFEEEEEE)
private val CountBadgeBg = Color(0xFFEFEFEF)

// Status colors — 6-step mastery progression (New → Seen → Familiar → Learning → Known → Mastered)
private val StatusNew      = Color(0xFFBDBDBD) // 0 — gray — newly added
private val StatusSeen     = Color(0xFFCE93D8) // 1 — soft violet — first exposure
private val StatusFamiliar = Color(0xFFFFCC80) // 2 — light amber — starting to stick
private val StatusLearning = Color(0xFFFFA726) // 3 — amber — being learned
private val StatusKnown    = Color(0xFF66BB6A) // 4 — green — known
private val StatusMastered = Color(0xFF42A5F5) // 5 — blue — mastered
private val StatusReview   = Color(0xFFEF5350) // red — needs review (overrides)

// ─────────────────────────────────────────────
//  Filter state — 6 mastery levels + ALL + REVIEW
// ─────────────────────────────────────────────
private enum class WordFilter(val label: String) {
    ALL("Все"),
    NEW("Новые"),
    SEEN("Видел"),
    FAMILIAR("Знакомые"),
    LEARNING("Учу"),
    KNOWN("Знаю"),
    MASTERED("Выучено"),
    REVIEW("Повторить"),
}

private fun classifyWord(
    progressMap: Map<StudyMode, WordProgress>,
    now: Long,
): WordFilter {
    if (progressMap.isEmpty()) return WordFilter.NEW
    val hasOverdue = progressMap.values.any { it.nextReviewAt in 1..now }
    if (hasOverdue) return WordFilter.REVIEW
    val minLevel = progressMap.values.minOf { it.masteryLevel }
    return when {
        minLevel >= 5 -> WordFilter.MASTERED
        minLevel >= 4 -> WordFilter.KNOWN
        minLevel >= 3 -> WordFilter.LEARNING
        minLevel >= 2 -> WordFilter.FAMILIAR
        minLevel >= 1 -> WordFilter.SEEN
        else -> WordFilter.NEW
    }
}

private fun statusColor(f: WordFilter): Color = when (f) {
    WordFilter.NEW -> StatusNew
    WordFilter.SEEN -> StatusSeen
    WordFilter.FAMILIAR -> StatusFamiliar
    WordFilter.LEARNING -> StatusLearning
    WordFilter.KNOWN -> StatusKnown
    WordFilter.MASTERED -> StatusMastered
    WordFilter.REVIEW -> StatusReview
    WordFilter.ALL -> Black
}

// Pre-computed row data — calculated once per snapshot, reused across scroll
@androidx.compose.runtime.Immutable
private data class WordItemData(
    val word: Word,
    val status: WordFilter,
    val accuracyPct: Int,      // -1 = no progress
    val nearestReview: Long,   // 0 = no review
    val isOverdue: Boolean,
    val reviewText: String,    // pre-formatted
    val progressMap: Map<StudyMode, WordProgress>, // for expanded view
)

// ─────────────────────────────────────────────
//  Main screen
// ─────────────────────────────────────────────
@Composable
fun DictionaryScreen(
    onNavigateToAddWords: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToWordSearch: () -> Unit = {},
    showTopBar: Boolean = true,
    viewModel: DictionaryViewModel = viewModel { DictionaryViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeFilter by remember { mutableStateOf(WordFilter.ALL) }

    LaunchedEffect(Unit) { viewModel.loadWords() }

    // Pre-compute everything per-word ONCE per data snapshot
    val itemDataList: List<WordItemData> = remember(
        uiState.words, uiState.wordProgressMap, uiState.now,
    ) {
        uiState.words.map { word ->
            val pm = uiState.wordProgressMap[word.id] ?: emptyMap()
            val status = classifyWord(pm, uiState.now)

            val totalAttempts = pm.values.sumOf { it.totalCount }
            val acc = if (totalAttempts > 0)
                (pm.values.sumOf { it.correctCount } * 100 / totalAttempts) else -1

            val nearest = pm.values
                .filter { it.nextReviewAt > 0 }
                .minOfOrNull { it.nextReviewAt } ?: 0L
            val overdue = nearest in 1..uiState.now
            val reviewText = when {
                nearest <= 0 -> ""
                overdue -> "⏰ Повторить"
                else -> "⏱ ${formatRelative(nearest, uiState.now)}"
            }

            WordItemData(word, status, acc, nearest, overdue, reviewText, pm)
        }
    }

    // Counts per filter
    val counts: Map<WordFilter, Int> = remember(itemDataList) {
        val map = HashMap<WordFilter, Int>(6)
        WordFilter.values().forEach { map[it] = 0 }
        map[WordFilter.ALL] = itemDataList.size
        itemDataList.forEach { map[it.status] = (map[it.status] ?: 0) + 1 }
        map
    }

    // Memoized filtered list — recomputed only when dependencies change
    val query = uiState.searchQuery
    val filteredItems: List<WordItemData> = remember(itemDataList, query, activeFilter) {
        val byQuery = if (query.isBlank()) itemDataList
        else {
            val q = query.lowercase()
            itemDataList.filter {
                it.word.original.lowercase().contains(q) ||
                    it.word.translation.lowercase().contains(q)
            }
        }
        if (activeFilter == WordFilter.ALL) byQuery
        else byQuery.filter { it.status == activeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        // ── Toolbar: back button + title
        Toolbar(
            title = "Слова",
            onNavigateBack = onNavigateBack,
        )

        PullRefresh(
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.isEmpty) {
                EmptyDictionaryContent(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    onNavigateToAddWords = onNavigateToAddWords,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Search + Add button
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            SearchField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                            )
                            Spacer(Modifier.height(10.dp))
                            AddWordsButton(onClick = onNavigateToAddWords)
                        }
                    }

                    // Filter chips row
                    item {
                        Spacer(Modifier.height(14.dp))
                        FilterChipsRow(
                            activeFilter = activeFilter,
                            counts = counts,
                            onSelect = { activeFilter = it },
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // Filtered/searched word list (memoized above).
                    // `onRemove` is hoisted as a stable (Long) -> Unit so each
                    // item doesn't recreate its own lambda on every recomposition.
                    val onRemove: (Long) -> Unit = viewModel::removeWord
                    items(
                        items = filteredItems,
                        key = { it.word.id },
                        contentType = { "word_row" },
                    ) { data ->
                        WordRow(
                            data = data,
                            now = uiState.now,
                            onRemove = onRemove,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Spacer(Modifier.height(40.dp))
                            Text(
                                text = when {
                                    uiState.searchQuery.isNotBlank() -> "Ничего не найдено"
                                    activeFilter != WordFilter.ALL -> "Нет слов в этой категории"
                                    else -> ""
                                },
                                fontSize = 15.sp,
                                color = SubtitleGray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Pieces
// ─────────────────────────────────────────────
@Composable
private fun Toolbar(
    title: String,
    onNavigateBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBg)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = Black,
            )
        }

        Spacer(Modifier.width(4.dp))

        // Title — bold, left-aligned
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
        )
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CountBadgeBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        placeholder = {
            Text("Поиск…", color = LightGray, fontSize = 15.sp)
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Black,
            unfocusedTextColor = Black,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Black,
        ),
    )
}

@Composable
private fun AddWordsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(Black)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Добавить слова",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = White,
        )
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: WordFilter,
    counts: Map<WordFilter, Int>,
    onSelect: (WordFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WordFilter.values().forEach { f ->
            val selected = f == activeFilter
            val count = counts[f] ?: 0
            if (f != WordFilter.ALL && count == 0) return@forEach

            FilterChip(
                label = f.label,
                count = count,
                selected = selected,
                accent = if (f == WordFilter.ALL) Black else statusColor(f),
                onClick = { onSelect(f) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Black else FieldBg,
        animationSpec = tween(180),
    )
    val fg by animateColorAsState(
        targetValue = if (selected) White else Black,
        animationSpec = tween(180),
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dot (not for ALL)
        if (label != "Все") {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = if (selected) White.copy(alpha = 0.6f) else SubtitleGray,
        )
    }
}

// ─────────────────────────────────────────────
//  Word row — expandable CardView (optimized)
// ─────────────────────────────────────────────
@Composable
private fun WordRow(
    data: WordItemData,
    now: Long,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressMap = data.progressMap
    // Plain `remember` instead of `rememberSaveable` — hoisting per-item state into
    // saveable registry causes Bundle serialization on scroll recycling (~0.2-1ms/item).
    // Expand state for a dictionary card is transient and not worth persisting.
    var expanded by remember(data.word.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Single drawWithCache pass instead of clip + background + border stack.
            // Cuts per-item draw cost ~20-40% on long lists.
            .cardBg(
                color = if (expanded) CardBgExp else CardBg,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 16.dp,
                clipContent = true, // rounded ripple bounds
            )
            .clickable { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.word.original,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = data.word.translation,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    color = SubtitleGray,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusBadge(status = data.status)
                    if (data.reviewText.isNotEmpty()) {
                        ReviewBadge(text = data.reviewText, overdue = data.isOverdue)
                    }
                }
            }

            // Accuracy pill — Text with background modifier (no Box wrapper)
            if (data.accuracyPct >= 0) {
                Text(
                    text = "${data.accuracyPct}%",
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    modifier = Modifier
                        .cardBg(color = CountBadgeBg, cornerRadius = 50.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
                Spacer(Modifier.width(6.dp))
            }

            // Chevron — Text directly, no wrapping Box
            Text(
                text = if (expanded) "⌃" else "⌄",
                fontSize = 18.sp,
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                color = LightGray,
                modifier = Modifier.size(24.dp).padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )
        }

        if (expanded) {
            AnimatedVisibility(
                visible = true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ExpandedStats(
                    progressMap = progressMap,
                    now = now,
                    onRemove = { onRemove(data.word.id) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Status + Review badges
// ─────────────────────────────────────────────
@Composable
private fun StatusBadge(status: WordFilter) {
    val color = statusColor(status)
    val bg = color.copy(alpha = 0.12f)
    val label = when (status) {
        WordFilter.NEW -> "Новое"
        WordFilter.SEEN -> "Видел"
        WordFilter.FAMILIAR -> "Знакомое"
        WordFilter.LEARNING -> "Учу"
        WordFilter.KNOWN -> "Знаю"
        WordFilter.MASTERED -> "Выучено"
        WordFilter.REVIEW -> "Повторить"
        WordFilter.ALL -> ""
    }
    if (label.isEmpty()) return

    Row(
        modifier = Modifier
            .cardBg(color = bg, cornerRadius = 50.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .cardBg(color = color, cornerRadius = 50.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun ReviewBadge(text: String, overdue: Boolean) {
    val color = if (overdue) StatusReview else SubtitleGray
    val bg = if (overdue) color.copy(alpha = 0.12f) else CountBadgeBg
    Text(
        text = text,
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false,
        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Medium,
        color = color,
        modifier = Modifier
            .cardBg(color = bg, cornerRadius = 50.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ExpandedStats(
    progressMap: Map<StudyMode, WordProgress>,
    now: Long,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor),
        )

        Spacer(Modifier.height(12.dp))

        if (progressMap.isEmpty()) {
            Text(
                text = "Ещё не изучалось",
                fontSize = 13.sp,
                color = SubtitleGray,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            val modes = listOf(
                StudyMode.MULTIPLE_CHOICE to "Тест",
                StudyMode.FLASHCARD to "Карточки",
                StudyMode.TYPING to "Ввод",
                StudyMode.LETTER_ASSEMBLY to "Сборка",
            )
            modes.forEach { (mode, label) ->
                val p = progressMap[mode]
                ModeRow(label = label, progress = p, now = now)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Delete action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRemove)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "×",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StatusReview,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Удалить из словаря",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = StatusReview,
            )
        }
    }
}

@Composable
private fun ModeRow(
    label: String,
    progress: WordProgress?,
    now: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Black,
            modifier = Modifier.width(66.dp),
        )

        // Level dots (0..5)
        val level = progress?.masteryLevel ?: 0
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(5) { i ->
                val filled = i < level
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) when {
                                level >= 4 -> StatusKnown
                                level >= 2 -> StatusLearning
                                else -> StatusNew
                            } else DividerColor
                        ),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Review status
        Text(
            text = when {
                progress == null -> "—"
                progress.nextReviewAt in 1..now -> "Повторить"
                progress.nextReviewAt > 0 -> formatRelative(progress.nextReviewAt, now)
                else -> "—"
            },
            fontSize = 11.sp,
            color = if (progress != null && progress.nextReviewAt in 1..now) StatusReview
                else SubtitleGray,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
        )
    }
}

// ─────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────
@Composable
private fun EmptyDictionaryContent(
    modifier: Modifier = Modifier,
    onNavigateToAddWords: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📚", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Словарь пуст",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Добавьте слова, чтобы начать изучение",
                fontSize = 15.sp,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Black)
                    .clickable(onClick = onNavigateToAddWords)
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Добавить слова",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────
private val MONTH_NAMES_SHORT = listOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

private fun formatShortDate(millis: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val date = kotlin.time.Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
    return "${date.day} ${MONTH_NAMES_SHORT[date.month.ordinal]}"
}

private fun formatRelative(millis: Long, now: Long): String {
    val diffMs = millis - now
    val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
    return when {
        diffDays < 0 -> {
            val ago = -diffDays
            if (ago == 1) "вчера" else "$ago дн. назад"
        }
        diffDays == 0 -> "сегодня"
        diffDays == 1 -> "завтра"
        diffDays <= 7 -> "через $diffDays дн."
        else -> formatShortDate(millis)
    }
}
