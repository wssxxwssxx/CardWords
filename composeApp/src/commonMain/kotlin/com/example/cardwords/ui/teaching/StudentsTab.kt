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
import com.example.cardwords.data.model.StudentSummary
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.dialogs.AddStudentDialog
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun StudentsTab(viewModel: MyStudentsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInvite by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.students.size} учеников", fontSize = 13.sp, color = LightFgSecondary, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LightFg)
                    .clickable { showInvite = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("+ Пригласить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
            }
        }

        PullRefresh(onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when (state.phase) {
                StudentsPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = LightFg, strokeWidth = 2.dp)
                }
                StudentsPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Ошибка", fontSize = 14.sp, color = Red40)
                }
                StudentsPhase.CONTENT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.students.isEmpty()) {
                            item { EmptyHint() }
                        } else {
                            items(state.students, key = { it.id }) { student ->
                                StudentRow(student, removing = student.id in state.removingIds, onRemove = { viewModel.removeStudent(student.id) })
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (showInvite) {
        AddStudentDialog(
            inFlight = state.inviteInFlight,
            error = state.inviteError,
            onSubmit = { email -> viewModel.invite(email) { showInvite = false } },
            onDismiss = { showInvite = false },
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(20.dp),
    ) {
        Text("Пригласите первого ученика по email.", fontSize = 13.sp, color = LightFgSecondary)
    }
}

@Composable
private fun StudentRow(student: StudentSummary, removing: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 14.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
            Spacer(Modifier.height(2.dp))
            Text("${student.email} · ${student.cardsCount} карточек", fontSize = 12.sp, color = LightFgSecondary)
        }
        if (!removing) {
            Box(
                modifier = Modifier.clickable(onClick = onRemove).padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Удалить", fontSize = 12.sp, color = Red40)
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = LightFgSecondary)
        }
    }
}
