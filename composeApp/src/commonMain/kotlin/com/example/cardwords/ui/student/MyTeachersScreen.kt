package com.example.cardwords.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.AssignedCollection
import com.example.cardwords.data.model.AssignmentStatus
import com.example.cardwords.data.model.TeacherSummary
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

private val DividerColor = Color(0xFFE5E5EA)
private val SectionLabel = Color(0xFF8A8A8A)

@Composable
fun MyTeachersScreen() {
    val viewModel = remember { MyTeachersViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Учителя",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LightFg,
            )
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                MyTeachersPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                MyTeachersPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = LightFgSecondary)
                }
                MyTeachersPhase.CONTENT -> ContentList(state, onAccept = viewModel::acceptCollection)
            }
        }
    }
}

@Composable
private fun ContentList(
    state: MyTeachersUiState,
    onAccept: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Teachers section
        item {
            SectionHeader("УЧИТЕЛЯ · ${state.teachers.size}")
        }
        if (state.teachers.isEmpty()) {
            item { EmptyHint("У вас пока нет учителей. Учитель пригласит вас по email.") }
        } else {
            items(state.teachers, key = { it.id }) { TeacherRow(it) }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Collections section
        item {
            SectionHeader("НАЗНАЧЕННЫЕ КОЛЛЕКЦИИ · ${state.collections.size}")
        }
        if (state.collections.isEmpty()) {
            item { EmptyHint("Учитель пока ничего не назначил.") }
        } else {
            items(state.collections, key = { it.collection.id }) { coll ->
                CollectionRow(
                    item = coll,
                    accepting = coll.collection.id in state.acceptingIds,
                    onAccept = { onAccept(coll.collection.id) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionLabel,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(20.dp),
    ) {
        Text(text = text, fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun TeacherRow(teacher: TeacherSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(teacher.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text(teacher.email, fontSize = 12.sp, color = LightFgSecondary)
        }
    }
}

@Composable
private fun CollectionRow(
    item: AssignedCollection,
    accepting: Boolean,
    onAccept: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.collection.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            if (!item.collection.description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(item.collection.description, fontSize = 12.sp, color = LightFgSecondary)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(item.status)
                Spacer(Modifier.width(8.dp))
                Text("${item.collection.cardsCount} карточек · от ${item.teacherName}", fontSize = 11.sp, color = LightFgSecondary)
                if (item.hasNewCards) {
                    Spacer(Modifier.width(8.dp))
                    NewBadge()
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        AcceptButton(item, accepting, onAccept)
    }
}

@Composable
private fun StatusChip(status: AssignmentStatus) {
    val (bg, fg, label) = when (status) {
        AssignmentStatus.ASSIGNED -> Triple(LightProgressBar.copy(alpha = 0.12f), LightProgressBar, "Выдана")
        AssignmentStatus.ADDED    -> Triple(Green40.copy(alpha = 0.12f), Green40, "Добавлена")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LightProgressBar)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("Новые", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun AcceptButton(
    item: AssignedCollection,
    accepting: Boolean,
    onAccept: () -> Unit,
) {
    val isAdded = item.status == AssignmentStatus.ADDED && !item.hasNewCards
    val label = when {
        accepting -> "..."
        item.hasNewCards -> "Обновить"
        isAdded -> "OK"
        else -> "Принять"
    }
    val enabled = !accepting && (!isAdded || item.hasNewCards)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) LightFg else Color(0xFFE0E0E0))
            .let { if (enabled) it.clickable(onClick = onAccept) else it }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
    }
}
