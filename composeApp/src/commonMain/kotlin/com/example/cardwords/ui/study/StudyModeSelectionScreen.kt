package com.example.cardwords.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.AmberLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyModeSelectionScreen(
    onNext: (Set<StudyMode>) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedModes by remember {
        mutableStateOf(StudyMode.entries.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Режим обучения",
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
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Выберите режимы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                StudyModeCard(
                    emoji = "\uD83C\uDFAF",
                    title = "Тест",
                    subtitle = "Выберите правильный перевод из 4 вариантов",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isSelected = StudyMode.MULTIPLE_CHOICE in selectedModes,
                    onToggle = {
                        selectedModes = toggleMode(selectedModes, StudyMode.MULTIPLE_CHOICE)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                StudyModeCard(
                    emoji = "\uD83D\uDCCB",
                    title = "Карточки",
                    subtitle = "Переворачивайте карточки и оценивайте себя",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    isSelected = StudyMode.FLASHCARD in selectedModes,
                    onToggle = {
                        selectedModes = toggleMode(selectedModes, StudyMode.FLASHCARD)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                StudyModeCard(
                    emoji = "\u270D\uFE0F",
                    title = "Ввод",
                    subtitle = "Напишите перевод слова самостоятельно",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    isSelected = StudyMode.TYPING in selectedModes,
                    onToggle = {
                        selectedModes = toggleMode(selectedModes, StudyMode.TYPING)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                StudyModeCard(
                    emoji = "\uD83D\uDD24",
                    title = "Сборка",
                    subtitle = "Соберите перевод слова из букв",
                    containerColor = AmberDark,
                    contentColor = AmberLight,
                    isSelected = StudyMode.LETTER_ASSEMBLY in selectedModes,
                    onToggle = {
                        selectedModes = toggleMode(selectedModes, StudyMode.LETTER_ASSEMBLY)
                    },
                )

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
                    text = "Выбрано режимов: ${selectedModes.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onNext(selectedModes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "Далее",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun toggleMode(current: Set<StudyMode>, mode: StudyMode): Set<StudyMode> {
    return if (mode in current) {
        if (current.size > 1) current - mode else current
    } else {
        current + mode
    }
}

@Composable
private fun StudyModeCard(
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) containerColor
        else surfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(300),
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) contentColor
        else onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(300),
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) primary.copy(alpha = 0.45f)
        else onSurfaceVariant.copy(alpha = 0.12f),
        animationSpec = tween(300),
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 1.dp,
        animationSpec = tween(300),
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
    )
    val animatedCheckBg by animateColorAsState(
        targetValue = if (isSelected) primary else Color.Transparent,
        animationSpec = tween(250),
    )
    val animatedCheckBorder by animateColorAsState(
        targetValue = if (isSelected) primary else onSurfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(250),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(animatedBorderWidth, animatedBorderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = animatedContentColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = animatedContentColor.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = animatedCheckBg,
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = animatedCheckBorder,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2713",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                    modifier = Modifier.scale(checkScale),
                )
            }
        }
    }
}
