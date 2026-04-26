package com.example.cardwords.ui.teaching.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)
private val FieldBg = Color(0xFFF2F2F7)

@Composable
fun CreateCollectionDialog(
    inFlight: Boolean,
    error: String?,
    onSubmit: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().cardBg(LightCard, DividerColor, 0.5.dp, 18.dp).padding(20.dp),
        ) {
            Text("Новая коллекция", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LightFg)
            Spacer(Modifier.height(12.dp))
            Field(name, { name = it }, "Название", singleLine = true, max = 100)
            Spacer(Modifier.height(8.dp))
            Field(description, { description = it }, "Описание (необязательно)", singleLine = false, max = 500)
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 12.sp, color = Red40)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                        .background(FieldBg).clickable(enabled = !inFlight, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text("Отмена", fontSize = 14.sp, color = LightFg) }
                Spacer(Modifier.width(10.dp))
                val canSubmit = name.isNotBlank() && !inFlight
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(50))
                        .background(if (canSubmit) LightFg else Color(0xFFCCCCCC))
                        .clickable(enabled = canSubmit) { onSubmit(name, description) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (inFlight) "..." else "Создать", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightCard)
                }
            }
        }
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, placeholder: String, singleLine: Boolean, max: Int) {
    TextField(
        value = value,
        onValueChange = { if (it.length <= max) onValue(it) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(placeholder, color = LightFgSecondary, fontSize = 15.sp) },
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedTextColor = LightFg,
            unfocusedTextColor = LightFg,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = LightFg,
        ),
    )
}
