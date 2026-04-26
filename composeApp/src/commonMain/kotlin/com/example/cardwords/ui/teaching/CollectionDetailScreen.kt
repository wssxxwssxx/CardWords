package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.AddCardDialog
import com.example.cardwords.ui.teaching.dialogs.AssignStudentSheet
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun CollectionDetailScreen(
    collectionId: String,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember(collectionId) { CollectionDetailViewModel(collectionId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCard by remember { mutableStateOf(false) }
    var showAssignSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 28.sp, color = LightFg) }
            Spacer(Modifier.width(4.dp))
            Text(
                text = state.detail?.collection?.name ?: "Коллекция",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LightFg,
                maxLines = 1,
            )
        }

        when (state.phase) {
            DetailPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
            }
            DetailPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
            }
            DetailPhase.CONTENT -> {
                val detail = state.detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        if (!detail.collection.description.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(16.dp)) {
                                Text(detail.collection.description, fontSize = 13.sp, color = LightFgSecondary)
                            }
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                                    .background(LightFg).clickable { showAddCard = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+ Карточка", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                            }
                            Box(
                                modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                                    .background(Color(0xFFF2F2F7)).clickable { showAssignSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("→ Назначить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                            }
                        }
                    }
                    item { Text("${detail.cards.size} карточек", fontSize = 11.sp, color = LightFgSecondary) }
                    items(detail.cards, key = { it.id }) { card ->
                        Row(
                            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(card.wordOriginal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                                Text(card.wordTranslation, fontSize = 12.sp, color = LightFgSecondary)
                            }
                            val deleting = card.id in state.deletingCardIds
                            if (!deleting) {
                                Text("×", fontSize = 18.sp, color = Red40,
                                    modifier = Modifier.clickable { viewModel.deleteCard(card.id) }.padding(horizontal = 8.dp))
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = LightFgSecondary)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showAddCard) {
        AddCardDialog(
            inFlight = state.addingCard,
            onSubmit = { o, t -> viewModel.addCard(o, t) },
            onDismiss = { showAddCard = false },
        )
    }
    if (showAssignSheet) {
        AssignStudentSheet(
            onPick = { studentId -> viewModel.assignTo(studentId) { showAssignSheet = false } },
            onDismiss = { showAssignSheet = false },
        )
    }
}
