package com.example.cardwords.ui.game.dungeon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.Indigo
import com.example.cardwords.ui.theme.Red60
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextSecondary
import kotlinx.coroutines.delay

// Dungeon-specific colors
private val DungeonPurple = Color(0xFF8B5CF6)
private val DungeonPurpleDark = Color(0xFF1A0D2E)
private val DungeonBgTop = Color(0xFF0A0D14)
private val DungeonBgBottom = Color(0xFF15101F)
private val MonsterHpColor = Color(0xFFEF4444)
private val MonsterHpTrack = Color(0xFF3F1515)
private val CardBorderActive = Amber
private val CardBorderDefault = Color.White.copy(alpha = 0.15f)

@Composable
fun DungeonScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { DungeonViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val bgGradient = Brush.verticalGradient(
        colors = listOf(DungeonBgTop, DungeonBgBottom, DungeonPurpleDark),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .safeContentPadding(),
    ) {
        when {
            state.isEmpty -> EmptyState(onNavigateBack)
            state.phase == DungeonPhase.NOT_STARTED -> StartScreen(
                state = state,
                onStart = { viewModel.startRun() },
                onBack = onNavigateBack,
            )
            state.phase == DungeonPhase.DECK_PREVIEW -> DeckPreviewScreen(
                state = state,
                onBegin = { viewModel.beginCombat() },
            )
            state.phase == DungeonPhase.COMBAT || state.phase == DungeonPhase.COMBAT_RESULT -> CombatScreen(
                state = state,
                onInputChanged = { viewModel.onInputChanged(it) },
                onSubmit = { viewModel.submitAnswer() },
                onSelectCard = { viewModel.selectCard(it) },
                onClearResult = { viewModel.clearCombatResult() },
            )
            state.phase == DungeonPhase.FLOOR_CLEARED -> FloorClearedScreen(
                state = state,
                onContinue = { viewModel.generateRewards() },
            )
            state.phase == DungeonPhase.REWARD_SELECTION -> RewardSelectionScreen(
                state = state,
                onSelectReward = { viewModel.selectReward(it) },
                onSkip = { viewModel.skipReward() },
            )
            state.phase == DungeonPhase.REMOVE_CARD_SELECTION -> RemoveCardScreen(
                state = state,
                onRemove = { viewModel.removeCard(it) },
            )
            state.phase == DungeonPhase.GAME_OVER -> RunEndScreen(
                state = state,
                isVictory = false,
                onPlayAgain = { viewModel.startRun() },
                onBack = onNavigateBack,
                onRunEnd = { viewModel.onRunEnd() },
            )
            state.phase == DungeonPhase.VICTORY -> RunEndScreen(
                state = state,
                isVictory = true,
                onPlayAgain = { viewModel.startRun() },
                onBack = onNavigateBack,
                onRunEnd = { viewModel.onRunEnd() },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Start Screen
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StartScreen(
    state: DungeonUiState,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "\uD83C\uDFF0", fontSize = 64.sp)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "\u0421\u043B\u043E\u0432\u0430\u0440\u043D\u044B\u0439 \u0434\u0430\u043D\u0436\u0435\u043D",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "\u0412\u0432\u043E\u0434\u0438 \u043F\u0435\u0440\u0435\u0432\u043E\u0434\u044B, \u0447\u0442\u043E\u0431\u044B \u0430\u0442\u0430\u043A\u043E\u0432\u0430\u0442\u044C \u043C\u043E\u043D\u0441\u0442\u0440\u043E\u0432!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        if (state.highestFloor > 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "\uD83C\uDFC6 \u041B\u0443\u0447\u0448\u0438\u0439 \u044D\u0442\u0430\u0436: ${state.highestFloor}/${state.totalFloors}",
                style = MaterialTheme.typography.titleMedium,
                color = DungeonPurple,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.totalRuns > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "\u0417\u0430\u0431\u0435\u0433\u043E\u0432: ${state.totalRuns}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DungeonPurple),
        ) {
            Text(
                text = "\u041D\u0430\u0447\u0430\u0442\u044C \u0437\u0430\u0431\u0435\u0433",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("\u041D\u0430\u0437\u0430\u0434", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Deck Preview
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckPreviewScreen(
    state: DungeonUiState,
    onBegin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "\u0422\u0432\u043E\u044F \u043A\u043E\u043B\u043E\u0434\u0430",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Text(
            text = "${state.deck.size} \u043A\u0430\u0440\u0442",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.deck) { _, card ->
                MiniCardView(card = card, selected = false, onClick = {})
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onBegin,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DungeonPurple),
        ) {
            Text(
                text = "\u0412 \u0431\u043E\u0439!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// Combat Screen
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CombatScreen(
    state: DungeonUiState,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelectCard: (Int) -> Unit,
    onClearResult: () -> Unit,
) {
    val combat = state.combat ?: return
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-clear combat result after delay
    LaunchedEffect(combat.lastResult) {
        if (combat.lastResult != null) {
            delay(800)
            onClearResult()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Top bar: Floor | HP | Relics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDFF0 ${state.currentFloor + 1}/${state.totalFloors}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DungeonPurple,
            )

            Row {
                repeat(state.playerHp) {
                    Text("\u2764\uFE0F", fontSize = 18.sp)
                }
                repeat((state.maxPlayerHp - state.playerHp).coerceAtLeast(0)) {
                    Text("\uD83E\uDE76", fontSize = 18.sp)
                }
            }
        }

        // Relics row
        if (state.relics.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.relics.forEach { relic ->
                    Text(text = relic.emoji, fontSize = 16.sp)
                }
            }
        }

        // Combo indicator
        if (state.currentCombo >= 2) {
            Text(
                text = "x${state.currentCombo} COMBO!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    state.currentCombo >= 5 -> Color(0xFFFFD700)
                    state.currentCombo >= 3 -> Color(0xFFFFA726)
                    else -> Green60
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Monster area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = combat.monster.emoji,
                fontSize = 72.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = combat.monster.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(12.dp))

            // Monster HP bar
            val hpFraction by animateFloatAsState(
                targetValue = combat.monsterHp.toFloat() / combat.monster.maxHp,
                animationSpec = tween(300),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LinearProgressIndicator(
                    progress = { hpFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MonsterHpColor,
                    trackColor = MonsterHpTrack,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${combat.monsterHp.coerceAtLeast(0)} / ${combat.monster.maxHp}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            // Damage / result feedback
            AnimatedVisibility(
                visible = combat.lastResult != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val (text, color) = when (combat.lastResult) {
                    CombatResult.CORRECT -> "-${combat.lastDamage}" to Green60
                    CombatResult.INCORRECT -> {
                        if (combat.lastDamage > 0) "+${combat.lastDamage} \u0443\u0440\u043E\u043D" to Red60
                        else "\u041F\u0440\u043E\u043C\u0430\u0445!" to Color(0xFFFFA726)
                    }
                    null -> "" to Color.White
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Hand — cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            combat.hand.forEachIndexed { index, card ->
                val isActive = index == combat.activeCardIndex
                HandCardView(
                    card = card,
                    isActive = isActive,
                    onClick = { onSelectCard(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = combat.input,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
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
                    cursorColor = DungeonPurple,
                    focusedBorderColor = DungeonPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DungeonPurple),
            ) {
                Text("\u2694\uFE0F", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// Floor Cleared
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FloorClearedScreen(
    state: DungeonUiState,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "\u2694\uFE0F", fontSize = 64.sp)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "\u042D\u0442\u0430\u0436 ${state.currentFloor + 1} \u043F\u0440\u043E\u0439\u0434\u0435\u043D!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(Modifier.height(8.dp))

        val accuracy = if (state.totalAttempts > 0) {
            (state.totalCorrect * 100 / state.totalAttempts)
        } else 0

        Text(
            text = "\u0422\u043E\u0447\u043D\u043E\u0441\u0442\u044C: $accuracy%  |  HP: ${state.playerHp}/${state.maxPlayerHp}",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DungeonPurple),
        ) {
            Text(
                text = "\u041D\u0430\u0433\u0440\u0430\u0434\u0430",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Reward Selection
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RewardSelectionScreen(
    state: DungeonUiState,
    onSelectReward: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "\u0412\u044B\u0431\u0435\u0440\u0438 \u043D\u0430\u0433\u0440\u0430\u0434\u0443",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(Modifier.height(24.dp))

        state.rewards.forEachIndexed { index, reward ->
            RewardCardView(
                reward = reward,
                onClick = { onSelectReward(index) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onSkip) {
            Text("\u041F\u0440\u043E\u043F\u0443\u0441\u0442\u0438\u0442\u044C", color = Color.White.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// Remove Card Selection
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RemoveCardScreen(
    state: DungeonUiState,
    onRemove: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            text = "\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u043A\u0430\u0440\u0442\u0443 \u0438\u0437 \u043A\u043E\u043B\u043E\u0434\u044B",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.deck) { index, card ->
                MiniCardView(
                    card = card,
                    selected = false,
                    onClick = { onRemove(index) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// Run End (Game Over / Victory)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RunEndScreen(
    state: DungeonUiState,
    isVictory: Boolean,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    onRunEnd: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRunEnd()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isVictory) "\uD83C\uDFC6" else "\uD83D\uDCA5",
            fontSize = 64.sp,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isVictory) "\u041F\u043E\u0431\u0435\u0434\u0430!" else "\u041F\u043E\u0440\u0430\u0436\u0435\u043D\u0438\u0435",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isVictory) Color(0xFFFFD700) else Color.White,
        )

        if (isVictory) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\u0414\u0430\u043D\u0436\u0435\u043D \u043F\u0440\u043E\u0439\u0434\u0435\u043D!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatRow("\u042D\u0442\u0430\u0436\u0438", "${state.floorsCleared}/${state.totalFloors}")
                StatRow("\u041F\u0440\u0430\u0432\u0438\u043B\u044C\u043D\u043E", "${state.totalCorrect}/${state.totalAttempts}")
                if (state.totalAttempts > 0) {
                    val acc = state.totalCorrect * 100 / state.totalAttempts
                    StatRow("\u0422\u043E\u0447\u043D\u043E\u0441\u0442\u044C", "$acc%")
                }
                StatRow("\u041B\u0443\u0447\u0448\u0438\u0439 \u043A\u043E\u043C\u0431\u043E", "x${state.bestCombo}")
                if (state.xpEarned > 0) {
                    StatRow("\u041E\u043F\u044B\u0442", "+${state.xpEarned} XP")
                }
            }
        }

        // Relics collected
        if (state.relics.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.relics.forEach { relic ->
                    Text(text = relic.emoji, fontSize = 24.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DungeonPurple),
        ) {
            Text(
                text = "\u0415\u0449\u0451 \u0440\u0430\u0437",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("\u0414\u043E\u043C\u043E\u0439", color = Color.White.copy(alpha = 0.6f))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// Reusable Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HandCardView(
    card: DungeonCard,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) CardBorderActive else CardBorderDefault,
                shape = RoundedCornerShape(12.dp),
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Surface2 else Surface1,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = card.word.original,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "\u2694\uFE0F",
                    fontSize = 12.sp,
                )
                Text(
                    text = "${card.power}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (card.upgraded) Color(0xFFFFD700) else Amber,
                )
                if (card.word.category.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = card.word.category.take(6),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCardView(
    card: DungeonCard,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Amber else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.word.original,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.word.category.isNotEmpty()) {
                    Text(
                        text = card.word.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = "\u2694\uFE0F${card.power}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (card.upgraded) Color(0xFFFFD700) else Amber,
            )
        }
    }
}

@Composable
private fun RewardCardView(
    reward: FloorReward,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DungeonPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = reward.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
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
        Text(text = "\uD83C\uDFF0", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "\u041D\u0443\u0436\u043D\u043E \u043C\u0438\u043D\u0438\u043C\u0443\u043C 3 \u0441\u043B\u043E\u0432\u0430",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "\u0414\u043E\u0431\u0430\u0432\u044C\u0442\u0435 \u0441\u043B\u043E\u0432\u0430 \u0432 \u0441\u043B\u043E\u0432\u0430\u0440\u044C,\n\u0447\u0442\u043E\u0431\u044B \u0438\u0433\u0440\u0430\u0442\u044C",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateBack) {
            Text("\u041D\u0430\u0437\u0430\u0434", color = Color.White.copy(alpha = 0.6f))
        }
    }
}
