package com.example.cardwords.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.packs.WordPackRegistry
import com.example.cardwords.di.AppModule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyWordSettingsScreen(
    multipleChoice: Boolean,
    flashcard: Boolean,
    typing: Boolean,
    letterAssembly: Boolean,
    onStartStudy: (wordCount: Int, wordSource: String, wordIds: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedWordCount by remember { mutableStateOf(0) } // 0 = all
    var selectedSource by remember { mutableStateOf("") }
    var selectedWordIds by remember { mutableStateOf(setOf<Long>()) }
    var isWordPickerExpanded by remember { mutableStateOf(false) }

    val repository = AppModule.databaseRepository
    val totalDictionaryCount = remember {
        repository.getDictionaryWordCount()
    }
    val availablePacks = remember {
        WordPackRegistry.allPacks.filter { pack ->
            repository.getDictionaryWordCountBySource(pack.sourceTag) > 0
        }
    }
    val packWordCounts = remember(availablePacks) {
        availablePacks.associate { pack ->
            pack.sourceTag to repository.getDictionaryWordCountBySource(pack.sourceTag)
        }
    }

    // Words for the current source (for the picker)
    val sourceWords = remember(selectedSource) {
        if (selectedSource.isEmpty()) {
            repository.getDictionaryWords()
        } else {
            repository.getDictionaryWordsBySource(selectedSource)
        }
    }

    val sourceWordCount = sourceWords.size.toLong()

    val effectiveCount = when {
        selectedWordIds.isNotEmpty() -> selectedWordIds.size.toLong()
        selectedWordCount == 0 -> sourceWordCount
        else -> minOf(selectedWordCount.toLong(), sourceWordCount)
    }

    // Build selected modes summary text
    val modeNames = buildList {
        if (multipleChoice) add("Тест")
        if (flashcard) add("Карточки")
        if (typing) add("Ввод")
        if (letterAssembly) add("Сборка")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки сессии",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center,
                    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Summary of selected modes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "\uD83C\uDFAF",
                            fontSize = 20.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Режимы обучения",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            )
                            Text(
                                text = modeNames.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                // --- Word Source Section ---
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Источник слов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectableChip(
                        label = "Все слова ($totalDictionaryCount)",
                        isSelected = selectedSource == "",
                        onClick = {
                            selectedSource = ""
                            selectedWordIds = emptySet()
                            isWordPickerExpanded = false
                        },
                    )

                    availablePacks.forEach { pack ->
                        val packCount = packWordCounts[pack.sourceTag] ?: 0
                        SelectableChip(
                            label = "${pack.emoji} ${pack.title} ($packCount)",
                            isSelected = selectedSource == pack.sourceTag,
                            onClick = {
                                selectedSource = pack.sourceTag
                                selectedWordIds = emptySet()
                                isWordPickerExpanded = false
                            },
                        )
                    }
                }

                // --- Word Count Section ---
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Количество слов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Counter row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Minus button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedWordCount > 0 && selectedWordIds.isEmpty())
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable(enabled = selectedWordCount > 0 && selectedWordIds.isEmpty()) {
                                val newCount = selectedWordCount - 5
                                selectedWordCount = if (newCount < 5) 0 else newCount
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u2212",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedWordCount > 0 && selectedWordIds.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Count display
                    Text(
                        text = if (selectedWordIds.isNotEmpty()) "${selectedWordIds.size}"
                        else if (selectedWordCount == 0) "Все"
                        else "$selectedWordCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // Plus button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedWordIds.isEmpty())
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable(enabled = selectedWordIds.isEmpty()) {
                                if (selectedWordCount == 0) {
                                    selectedWordCount = 5
                                } else {
                                    val newCount = selectedWordCount + 5
                                    if (newCount >= sourceWordCount) {
                                        selectedWordCount = 0 // back to "All"
                                    } else {
                                        selectedWordCount = newCount
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedWordIds.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Доступно: $sourceWordCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                // --- Word Selection Section ---
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Выбор слов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle button for word picker
                OutlinedButton(
                    onClick = {
                        isWordPickerExpanded = !isWordPickerExpanded
                        if (!isWordPickerExpanded) {
                            selectedWordIds = emptySet()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selectedWordIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(
                        text = if (selectedWordIds.isNotEmpty())
                            "Выбрано: ${selectedWordIds.size} слов"
                        else if (isWordPickerExpanded)
                            "Случайные слова"
                        else
                            "Выбрать конкретные слова",
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Word picker list
                AnimatedVisibility(
                    visible = isWordPickerExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(max = 250.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            items(
                                items = sourceWords,
                                key = { it.id },
                            ) { word ->
                                WordPickerItem(
                                    word = word,
                                    isSelected = word.id in selectedWordIds,
                                    onToggle = {
                                        selectedWordIds = if (word.id in selectedWordIds) {
                                            selectedWordIds - word.id
                                        } else {
                                            selectedWordIds + word.id
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp, top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Слов в сессии: $effectiveCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val idsString = if (selectedWordIds.isNotEmpty()) {
                            selectedWordIds.joinToString(",")
                        } else ""
                        onStartStudy(selectedWordCount, selectedSource, idsString)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = effectiveCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "Начать обучение ($effectiveCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WordPickerItem(
    word: Word,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else Color.Transparent,
        animationSpec = tween(200),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox circle
        val checkBg by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = tween(200),
        )
        val checkBorder by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            animationSpec = tween(200),
        )
        val checkScale by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = tween(150),
        )

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color = checkBg, shape = CircleShape)
                .border(width = 1.5.dp, color = checkBorder, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u2713",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.scale(checkScale),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.original,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = word.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) primary else surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250),
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) onPrimary else onSurfaceVariant,
        animationSpec = tween(250),
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = animatedBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = animatedTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
