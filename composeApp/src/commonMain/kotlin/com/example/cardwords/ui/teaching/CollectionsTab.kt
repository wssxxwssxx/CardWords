package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.cardwords.data.model.Collection
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.CreateCollectionDialog
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun CollectionsTab(
    viewModel: MyCollectionsViewModel,
    onOpenCollection: (id: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.collections.size} коллекций", fontSize = 13.sp, color = LightFgSecondary, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(LightFg)
                    .clickable { showCreate = true }.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("+ Создать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
            }
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                CollectionsPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                CollectionsPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
                }
                CollectionsPhase.CONTENT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.collections.isEmpty()) {
                            item { EmptyHint() }
                        } else {
                            items(state.collections, key = { it.id }) { c ->
                                CollectionRow(c, onClick = { onOpenCollection(c.id) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateCollectionDialog(
            inFlight = state.createInFlight,
            error = state.createError,
            onSubmit = { name, desc -> viewModel.create(name, desc) { showCreate = false } },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(20.dp),
    ) {
        Text("Создайте первую коллекцию.", fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun CollectionRow(c: Collection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp, clipContent = true)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text("${c.cardsCount} карточек", fontSize = 12.sp, color = LightFgSecondary)
        }
        Text("›", fontSize = 22.sp, color = LightFgSecondary, modifier = Modifier.padding(start = 8.dp))
    }
}
