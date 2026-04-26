package com.example.cardwords.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Red40

private val DividerColor = Color(0xFFE5E5EA)

@Composable
fun RoleSelectionScreen(
    onPicked: () -> Unit,
) {
    val viewModel = remember { RoleSelectionViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "Кто вы?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LightFg,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Выберите роль для продолжения. Её можно сменить в любой момент в Профиле.",
            fontSize = 14.sp,
            color = LightFgSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        RoleCard(
            title = "Я — ученик",
            subtitle = "Учу слова, прохожу тесты",
            enabled = state.phase != RolePickPhase.SUBMITTING,
            onClick = { viewModel.pickRole(UserRole.STUDENT, onPicked) },
        )
        Spacer(Modifier.height(12.dp))
        RoleCard(
            title = "Я — преподаватель",
            subtitle = "Создаю коллекции, выдаю их ученикам",
            enabled = state.phase != RolePickPhase.SUBMITTING,
            onClick = { viewModel.pickRole(UserRole.TEACHER, onPicked) },
        )

        Spacer(Modifier.height(20.dp))

        when (state.phase) {
            RolePickPhase.SUBMITTING -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = LightFg,
                strokeWidth = 2.dp,
            )
            RolePickPhase.ERROR -> Text(
                text = state.error ?: "Ошибка",
                fontSize = 13.sp,
                color = Red40,
                textAlign = TextAlign.Center,
            )
            RolePickPhase.IDLE -> Spacer(Modifier.height(28.dp))
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBg(
                color = LightCard,
                borderColor = DividerColor,
                borderWidth = 0.5.dp,
                cornerRadius = 16.dp,
                clipContent = true,
            )
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightFg,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = LightFgSecondary,
        )
    }
}
