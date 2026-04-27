package com.example.cardwords.ui.teaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary

private enum class TeachingTab { STUDENTS, COLLECTIONS }

@Composable
fun TeachingScreen(onOpenCollection: (id: String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(TeachingTab.STUDENTS) }
    val studentsVm = remember { MyStudentsViewModel() }
    val collectionsVm = remember { MyCollectionsViewModel() }

    Column(Modifier.fillMaxSize().background(LightBg)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Преподавание", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LightFg)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Ученики", selected == TeachingTab.STUDENTS) { selected = TeachingTab.STUDENTS }
            Pill("Коллекции", selected == TeachingTab.COLLECTIONS) { selected = TeachingTab.COLLECTIONS }
        }

        when (selected) {
            TeachingTab.STUDENTS    -> StudentsTab(studentsVm)
            TeachingTab.COLLECTIONS -> CollectionsTab(collectionsVm, onOpenCollection)
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LightFg else Color(0xFFF2F2F7)
    val fg = if (selected) Color.White else LightFgSecondary
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
