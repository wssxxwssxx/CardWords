package com.example.cardwords.ui.teaching.dialogs

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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.teaching.MyStudentsViewModel
import com.example.cardwords.ui.teaching.StudentsPhase
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun AssignStudentSheet(
    onPick: (studentId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val vm = remember { MyStudentsViewModel() }
    val state by vm.uiState.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 18.dp).padding(20.dp),
        ) {
            Text("Назначить ученику", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            when {
                state.phase == StudentsPhase.LOADING -> CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LightFg, strokeWidth = 2.dp)
                state.students.isEmpty() -> Text("Сначала добавьте ученика во вкладке «Ученики»", fontSize = 13.sp, color = LightFgSecondary)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.students, key = { it.id }) { st ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F8FA)).clickable { onPick(st.id) }.padding(12.dp),
                        ) {
                            Column {
                                Text(st.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                                Text(st.email, fontSize = 11.sp, color = LightFgSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
