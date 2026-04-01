package com.example.cardwords.ui.addwords

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardwords.data.model.Word
import com.example.cardwords.data.remote.FetchedWordResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSelectionScreen(
    onWordsAdded: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: WordSelectionViewModel = viewModel { WordSelectionViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Добавить слова",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.hasSelection,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 }),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.addSelectedWords()
                        onWordsAdded()
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = "Добавить ${uiState.selectedCount}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                            "Поиск слов...",
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
            }

            // Custom word entry
            item {
                FilledTonalButton(
                    onClick = { viewModel.toggleCustomForm() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        if (uiState.showCustomForm) "Скрыть форму" else "Добавить своё слово",
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (uiState.showCustomForm) {
                item {
                    CustomWordForm(
                        original = uiState.customOriginal,
                        translation = uiState.customTranslation,
                        transcription = uiState.customTranscription,
                        canAdd = uiState.canAddCustomWord,
                        wasAdded = uiState.customWordAdded,
                        onOriginalChange = { viewModel.onCustomOriginalChange(it) },
                        onTranslationChange = { viewModel.onCustomTranslationChange(it) },
                        onTranscriptionChange = { viewModel.onCustomTranscriptionChange(it) },
                        onAdd = { viewModel.addCustomWord() },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // API search result
            val apiResult = uiState.apiSearchResult
            if (apiResult is ApiSearchResult.Found) {
                item {
                    ApiWordCard(
                        word = apiResult.word,
                        onAdd = { viewModel.addApiWord(apiResult.word) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (apiResult is ApiSearchResult.NotFound) {
                item {
                    Text(
                        text = "Слово не найдено онлайн",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (uiState.isApiSearching) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text("Поиск онлайн...")
                    }
                }
            }

            val wordsToShow = if (uiState.searchQuery.isBlank()) uiState.words else uiState.filteredWords

            items(wordsToShow, key = { it.id }) { word ->
                val isAlreadyAdded = word.id in uiState.alreadyAddedIds
                val isSelected = word.id in uiState.selectedWordIds

                WordItem(
                    word = word,
                    isSelected = isSelected,
                    isAlreadyAdded = isAlreadyAdded,
                    onToggle = {
                        if (!isAlreadyAdded) {
                            viewModel.toggleWordSelection(word.id)
                        }
                    },
                )
            }

            // Show "Search online" button when local search has few results
            if (uiState.searchQuery.isNotBlank() && !uiState.isApiSearching && apiResult == null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.searchApi() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Искать \"${uiState.searchQuery}\" онлайн")
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiWordCard(
    word: FetchedWordResult,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.original,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                )
                if (word.transcription.isNotBlank()) {
                    Text(
                        text = word.transcription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    )
                }
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomWordForm(
    original: String,
    translation: String,
    transcription: String,
    canAdd: Boolean,
    wasAdded: Boolean,
    onOriginalChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onTranscriptionChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = original,
                onValueChange = onOriginalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Слово") },
                placeholder = { Text("Например: apple") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                ),
            )
            OutlinedTextField(
                value = translation,
                onValueChange = onTranslationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Перевод") },
                placeholder = { Text("Например: яблоко") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                ),
            )
            OutlinedTextField(
                value = transcription,
                onValueChange = onTranscriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Транскрипция (необязательно)") },
                placeholder = { Text("Например: [ˈæp.əl]") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (wasAdded) {
                    Text(
                        text = "Добавлено!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = onAdd,
                    enabled = canAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("Добавить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WordItem(
    word: Word,
    isSelected: Boolean,
    isAlreadyAdded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAlreadyAdded, onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAlreadyAdded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.original,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAlreadyAdded)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAlreadyAdded) "${word.translation} (добавлено)" else word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAlreadyAdded)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isAlreadyAdded) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}
