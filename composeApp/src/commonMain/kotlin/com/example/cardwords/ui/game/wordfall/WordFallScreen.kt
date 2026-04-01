package com.example.cardwords.ui.game.wordfall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val LANE_COUNT = 3

@Composable
fun WordFallScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { WordFallViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1117),
            Color(0xFF161B22),
            Color(0xFF1A1F2E),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .safeContentPadding(),
    ) {
        when {
            state.isEmpty -> EmptyState(onNavigateBack = onNavigateBack)
            state.gamePhase == GamePhase.NOT_STARTED -> StartScreen(
                highScore = state.highScore,
                onStart = { viewModel.startGame() },
                onBack = onNavigateBack,
            )
            state.gamePhase == GamePhase.GAME_OVER -> GameOverScreen(
                state = state,
                onPlayAgain = { viewModel.restartGame() },
                onBack = onNavigateBack,
            )
            else -> GamePlayScreen(
                state = state,
                onInputChanged = { viewModel.onInputChanged(it) },
                onFrameTick = { viewModel.onFrameTick(it) },
            )
        }
    }
}

@Composable
private fun StartScreen(
    highScore: Int,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "\uD83C\uDF0A",
            fontSize = 64.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "\u0421\u043B\u043E\u0432\u043E\u043F\u0430\u0434",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u0412\u0432\u043E\u0434\u0438 \u043F\u0435\u0440\u0435\u0432\u043E\u0434, \u043F\u043E\u043A\u0430 \u0441\u043B\u043E\u0432\u0430 \u043F\u0430\u0434\u0430\u044E\u0442!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        if (highScore > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "\uD83C\uDFC6 \u0420\u0435\u043A\u043E\u0440\u0434: $highScore",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "\u0421\u0442\u0430\u0440\u0442",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text(
                text = "\u041D\u0430\u0437\u0430\u0434",
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun GamePlayScreen(
    state: WordFallUiState,
    onInputChanged: (String) -> Unit,
    onFrameTick: (Long) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Game loop — drives both physics and rendering
    var frameTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { ms ->
                frameTimeMs = ms
                onFrameTick(ms)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Top bar: Score | Level | Lives
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u2B50 ${state.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
            )

            Text(
                text = "Lv.${state.level + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
            )

            Text(
                text = "\u2764\uFE0F".repeat(state.lives) + "\uD83E\uDE76".repeat((3 - state.lives).coerceAtLeast(0)),
                fontSize = 20.sp,
            )
        }

        // Combo indicator
        if (state.combo >= 2) {
            Text(
                text = "x${state.combo} COMBO!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    state.combo >= 5 -> Color(0xFFFFD700)
                    state.combo >= 3 -> Color(0xFFFFA726)
                    else -> Color(0xFF66BB6A)
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        // Game field
        var fieldWidthPx by remember { mutableIntStateOf(0) }
        var fieldHeightPx by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged {
                    fieldWidthPx = it.width
                    fieldHeightPx = it.height
                },
        ) {
            if (fieldWidthPx > 0 && fieldHeightPx > 0) {
                for (fw in state.activeWords) {
                    val progress = if (fw.fallDurationMs > 0 && frameTimeMs > 0) {
                        ((frameTimeMs - fw.startTimeMs).toFloat() / fw.fallDurationMs)
                            .coerceIn(0f, 1f)
                    } else 0f

                    val laneWidth = fieldWidthPx / LANE_COUNT
                    val xOffset = fw.lane * laneWidth
                    val yOffset = (progress * fieldHeightPx).toInt()

                    FallingWordCard(
                        word = fw.word.original,
                        xOffset = xOffset,
                        yOffset = yOffset,
                        laneWidth = laneWidth,
                    )
                }

                // Destroyed word effects
                for (effect in state.destroyedEffects) {
                    val laneWidth = fieldWidthPx / LANE_COUNT
                    val xOffset = effect.lane * laneWidth + laneWidth / 2
                    val yOffset = (effect.yProgress * fieldHeightPx).toInt()

                    DestroyedWordEffect(
                        text = effect.text,
                        xOffset = xOffset,
                        yOffset = yOffset,
                    )
                }
            }
        }

        // Input field
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    "\u0412\u0432\u0435\u0434\u0438 \u043F\u0435\u0440\u0435\u0432\u043E\u0434...",
                    color = Color.White.copy(alpha = 0.4f),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {}),
        )
    }
}

@Composable
private fun FallingWordCard(
    word: String,
    xOffset: Int,
    yOffset: Int,
    laneWidth: Int,
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(x = xOffset, y = yOffset) }
            .width(with(androidx.compose.ui.platform.LocalDensity.current) { laneWidth.toDp() })
            .padding(horizontal = 4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E3A5F).copy(alpha = 0.9f),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DestroyedWordEffect(
    text: String,
    xOffset: Int,
    yOffset: Int,
) {
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(500),
    )

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF66BB6A).copy(alpha = 1f - alpha),
        modifier = Modifier.offset { IntOffset(x = xOffset, y = yOffset - 30) },
    )
}

@Composable
private fun GameOverScreen(
    state: WordFallUiState,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "\uD83D\uDCA5",
            fontSize = 64.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "\u0418\u0433\u0440\u0430 \u043E\u043A\u043E\u043D\u0447\u0435\u043D\u0430",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${state.score}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD700),
        )
        Text(
            text = "\u043E\u0447\u043A\u043E\u0432",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
        )

        if (state.isNewRecord) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
            ) {
                Text(
                    text = "\uD83C\uDFC6 \u041D\u043E\u0432\u044B\u0439 \u0440\u0435\u043A\u043E\u0440\u0434!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatRow("\u0421\u043B\u043E\u0432", "${state.wordsDestroyed}")
                StatRow("\u041C\u0430\u043A\u0441. combo", "x${state.maxCombo}")
                StatRow("\u0423\u0440\u043E\u0432\u0435\u043D\u044C", "${state.level + 1}")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "\u0415\u0449\u0451 \u0440\u0430\u0437",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text(
                text = "\u0414\u043E\u043C\u043E\u0439",
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun EmptyState(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "\uD83D\uDCDA", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\u041D\u0443\u0436\u043D\u043E \u043C\u0438\u043D\u0438\u043C\u0443\u043C 5 \u0441\u043B\u043E\u0432",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\u0414\u043E\u0431\u0430\u0432\u044C\u0442\u0435 \u0441\u043B\u043E\u0432\u0430 \u0432 \u0441\u043B\u043E\u0432\u0430\u0440\u044C,\n\u0447\u0442\u043E\u0431\u044B \u0438\u0433\u0440\u0430\u0442\u044C",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text("\u041D\u0430\u0437\u0430\u0434", color = Color.White.copy(alpha = 0.6f))
        }
    }
}
