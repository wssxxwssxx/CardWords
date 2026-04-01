package com.example.cardwords.ui.dictionary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.components.MiniMasteryBar
import com.example.cardwords.ui.components.ReviewTemperatureIndicator
import com.example.cardwords.ui.components.computeReviewTemperature
import com.example.cardwords.ui.components.masteryLabelForLevel
import com.example.cardwords.ui.study.StudyMode
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onNavigateToAddWords: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToWordSearch: () -> Unit = {},
    showTopBar: Boolean = true,
    viewModel: DictionaryViewModel = viewModel { DictionaryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadWords()
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        if (uiState.isEmpty) {
            EmptyDictionaryContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                onNavigateToAddWords = onNavigateToAddWords,
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "\u041F\u043E\u0438\u0441\u043A \u0441\u043B\u043E\u0432...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToAddWords,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = "\u2795 \u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C \u0441\u043B\u043E\u0432\u0430",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val wordsToShow = if (uiState.searchQuery.isBlank()) uiState.words else uiState.filteredWords

                items(wordsToShow, key = { it.id }) { word ->
                    DictionaryWordItem(
                        word = word,
                        progressMap = uiState.wordProgressMap[word.id] ?: emptyMap(),
                        now = uiState.now,
                        onRemove = { viewModel.removeWord(word.id) },
                    )
                }

                if (uiState.searchQuery.isNotBlank() && uiState.filteredWords.isEmpty()) {
                    item {
                        Text(
                            text = "\u041D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "\u0421\u043B\u043E\u0432\u0430\u0440\u044C",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        ) { paddingValues ->
            content(Modifier.padding(paddingValues))
        }
    } else {
        content(Modifier)
    }
}

@Composable
private fun DictionaryWordItem(
    word: Word,
    progressMap: Map<StudyMode, WordProgress>,
    now: Long,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = word.original,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (progressMap.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(6.dp))
                        ReviewTemperatureIndicator(
                            temperature = computeReviewTemperature(progressMap, now),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        MiniMasteryBar(progressMap = progressMap)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp),
            ) {
                Text(
                    text = "\u2715",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            WordStatsSection(progressMap = progressMap, now = now)
        }
    }
}

private val MONTH_NAMES_SHORT = listOf(
    "\u044F\u043D\u0432", "\u0444\u0435\u0432", "\u043C\u0430\u0440",
    "\u0430\u043F\u0440", "\u043C\u0430\u0439", "\u0438\u044E\u043D",
    "\u0438\u044E\u043B", "\u0430\u0432\u0433", "\u0441\u0435\u043D",
    "\u043E\u043A\u0442", "\u043D\u043E\u044F", "\u0434\u0435\u043A",
)

private fun formatShortDate(millis: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
    return "${date.dayOfMonth} ${MONTH_NAMES_SHORT[date.monthNumber - 1]}"
}

private fun formatRelative(millis: Long, now: Long): String {
    val diffMs = millis - now
    val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
    return when {
        diffDays < 0 -> {
            val ago = -diffDays
            if (ago == 1) "\u0432\u0447\u0435\u0440\u0430"
            else "$ago \u0434\u043D. \u043D\u0430\u0437\u0430\u0434"
        }
        diffDays == 0 -> "\u0441\u0435\u0433\u043E\u0434\u043D\u044F"
        diffDays == 1 -> "\u0437\u0430\u0432\u0442\u0440\u0430"
        diffDays <= 7 -> "\u0447\u0435\u0440\u0435\u0437 $diffDays \u0434\u043D."
        else -> formatShortDate(millis)
    }
}

@Composable
private fun WordStatsSection(
    progressMap: Map<StudyMode, WordProgress>,
    now: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        if (progressMap.isEmpty()) {
            Text(
                text = "\u0415\u0449\u0451 \u043D\u0435 \u0438\u0437\u0443\u0447\u0430\u043B\u043E\u0441\u044C",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val totalCorrect = progressMap.values.sumOf { it.correctCount }
            val totalAttempts = progressMap.values.sumOf { it.totalCount }
            val overallAccuracy = if (totalAttempts > 0) (totalCorrect * 100 / totalAttempts) else 0

            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "\u0422\u043E\u0447\u043D\u043E\u0441\u0442\u044C: $totalCorrect/$totalAttempts ($overallAccuracy%)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode rows
            val modes = listOf(
                StudyMode.MULTIPLE_CHOICE to "\u0422\u0435\u0441\u0442",
                StudyMode.FLASHCARD to "\u041A\u0430\u0440\u0442\u043E\u0447\u043A\u0438",
                StudyMode.TYPING to "\u0412\u0432\u043E\u0434",
                StudyMode.LETTER_ASSEMBLY to "\u0421\u0431\u043E\u0440\u043A\u0430",
            )

            for ((mode, label) in modes) {
                val progress = progressMap[mode]
                ModeStatRow(label = label, progress = progress, now = now)
            }
        }
    }
}

@Composable
private fun ModeStatRow(
    label: String,
    progress: WordProgress?,
    now: Long,
) {
    if (progress == null) return

    val accuracy = if (progress.totalCount > 0)
        (progress.correctCount * 100 / progress.totalCount) else 0
    val needsReview = progress.nextReviewAt in 1..now

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mode name
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(72.dp),
        )

        // Progress bar
        LinearProgressIndicator(
            progress = { progress.masteryLevel / 5f },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                progress.masteryLevel >= 4 -> MaterialTheme.colorScheme.primary
                progress.masteryLevel >= 2 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            },
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Accuracy
        Text(
            text = "$accuracy%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Review status
        Text(
            text = if (needsReview) "\u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C"
            else if (progress.nextReviewAt > 0) formatRelative(progress.nextReviewAt, now)
            else "\u2014",
            style = MaterialTheme.typography.labelSmall,
            color = if (needsReview) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun EmptyDictionaryContent(
    modifier: Modifier = Modifier,
    onNavigateToAddWords: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83D\uDCDA",
                fontSize = 64.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Словарь пуст",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Добавьте слова, чтобы начать изучение",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToAddWords,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Добавить слова",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
