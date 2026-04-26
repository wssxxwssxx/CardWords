package com.example.cardwords.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.ui.components.PullRefresh
import com.example.cardwords.ui.components.cardBg
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary

private val DividerColor = Color(0xFFE5E5EA)

// ─────────────────────────────────────────────
//  Avatar data
// ─────────────────────────────────────────────
val AVATARS = listOf(
    "🦊", "🐺", "🦁", "🐯",
    "🐻", "🐼", "🦋", "🦝",
    "🐸", "🐉", "🌟", "⚡",
    "🎯", "🎸", "🚀", "🌊",
)

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() },
) {
    val state by viewModel.uiState.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarId = state.selectedAvatarId,
            onSelect = { id ->
                viewModel.setAvatar(id)
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false },
        )
    }

    if (showRoleDialog) {
        RolePickDialog(
            current = state.currentRole,
            onPick = {
                viewModel.switchRole(it)
                showRoleDialog = false
            },
            onDismiss = { showRoleDialog = false },
        )
    }

    PullRefresh(
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize().background(LightBg),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar — tap to change
        AvatarCircle(
            avatarId = state.selectedAvatarId,
            size = 90,
            onClick = { showAvatarPicker = true },
        )

        Spacer(Modifier.height(12.dp))

        // Name
        val displayName = state.userName ?: "Профиль"
        Text(
            text       = displayName,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = LightFg,
        )

        // Email
        if (state.userEmail != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text     = state.userEmail!!,
                fontSize = 14.sp,
                color    = LightFgSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))

        // XP level card
        XpLevelCard(state)

        Spacer(Modifier.height(12.dp))

        // Stats grid — 2-column × 3-row
        val stats = listOf(
            state.totalWords.toString()    to "Слов",
            state.wordsMastered.toString() to "Выучено",
            state.currentStreak.toString() to "Стрик",
            state.totalXp.toString()       to "Опыт",
            state.totalReviews.toString()  to "Повторений",
            state.streakFreezes.toString() to "Заморозки",
        )
        stats.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { (value, label) ->
                    StatCell(value = value, label = label, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(2.dp))

        // About card
        AboutCard()

        Spacer(Modifier.height(10.dp))

        // Switch-role action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cardBg(LightCard, DividerColor, 0.5.dp, 14.dp)
                .clickable(enabled = !state.roleSwitchInFlight) { showRoleDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Сменить роль",
                    fontSize = 15.sp,
                    color = LightFg,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (state.currentRole) {
                        UserRole.STUDENT -> "Сейчас: ученик"
                        UserRole.TEACHER -> "Сейчас: преподаватель"
                        null             -> "Не задана"
                    },
                    fontSize = 12.sp,
                    color = LightFgSecondary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Logout button
        var showLogoutConfirm by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFEBEB))
                .clickable { showLogoutConfirm = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Выйти из аккаунта",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFFDC2626),
            )
            Text(
                text  = "→",
                fontSize = 16.sp,
                color = Color(0xFFDC2626),
            )
        }

        if (showLogoutConfirm) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showLogoutConfirm = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(24.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "Выйти из аккаунта?",
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = LightFg,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text     = "Весь прогресс сохранён на сервере",
                            fontSize = 14.sp,
                            color    = LightFgSecondary,
                        )
                        Spacer(Modifier.height(24.dp))
                        // Confirm
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDC2626))
                                .clickable {
                                    viewModel.logout()
                                    onLogout()
                                }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = "Выйти",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = androidx.compose.ui.graphics.Color.White,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        // Cancel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightFgMuted.copy(alpha = 0.25f))
                                .clickable { showLogoutConfirm = false }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text     = "Отмена",
                                fontSize = 15.sp,
                                color    = LightFgSecondary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
    } // end PullRefresh
}

// ─────────────────────────────────────────────
//  Avatar circle (shared: profile + picker)
// ─────────────────────────────────────────────
@Composable
fun AvatarCircle(
    avatarId: Int,
    size: Int,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val emoji = AVATARS.getOrNull(avatarId)
    val borderMod = if (selected)
        Modifier.border(2.dp, LightFg, CircleShape)
    else
        Modifier

    Box(
        modifier = borderMod
            .size(size.dp)
            .clip(CircleShape)
            .background(LightFgMuted.copy(alpha = 0.35f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(
                text     = emoji,
                fontSize = (size * 0.45).sp,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Avatar picker dialog
// ─────────────────────────────────────────────
@Composable
private fun AvatarPickerDialog(
    currentAvatarId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "Выбери аватар",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = LightFg,
                )

                Spacer(Modifier.height(18.dp))

                LazyVerticalGrid(
                    columns             = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.fillMaxWidth(),
                ) {
                    items(AVATARS.size) { id ->
                        AvatarCircle(
                            avatarId = id,
                            size     = 58,
                            selected = id == currentAvatarId,
                            onClick  = { onSelect(id) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightFgMuted.copy(alpha = 0.25f))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text     = "Отмена",
                        fontSize = 15.sp,
                        color    = LightFgSecondary,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Role picker dialog
// ─────────────────────────────────────────────
@Composable
private fun RolePickDialog(
    current: UserRole?,
    onPick: (UserRole) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cardBg(LightCard, DividerColor, 0.5.dp, 18.dp)
                .padding(20.dp),
        ) {
            Text(
                text       = "Выберите роль",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
            Spacer(Modifier.height(12.dp))
            UserRole.values().forEach { role ->
                val label = when (role) {
                    UserRole.STUDENT -> "Я — ученик"
                    UserRole.TEACHER -> "Я — преподаватель"
                }
                val isCurrent = role == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCurrent) { onPick(role) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        color = if (isCurrent) LightFgSecondary else LightFg,
                    )
                    Spacer(Modifier.weight(1f))
                    if (isCurrent) {
                        Text(
                            text = "текущая",
                            fontSize = 11.sp,
                            color = LightFgSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  XP level card
// ─────────────────────────────────────────────
@Composable
private fun XpLevelCard(state: ProfileUiState) {
    val (xpCurrent, xpNeeded) = state.xpProgress
    val fraction = if (xpNeeded > 0) (xpCurrent.toFloat() / xpNeeded).coerceIn(0f, 1f) else 0f
    val pct      = (fraction * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Уровень ${state.currentLevel}",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = LightFg,
                )
                Text(
                    text     = "$xpCurrent / $xpNeeded XP",
                    fontSize = 13.sp,
                    color    = LightFgSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LightFgMuted.copy(alpha = 0.3f)),
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(LightFg),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Percentage pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LightFgMuted.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text     = "$pct%",
                    fontSize = 10.sp,
                    color    = LightFgSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Stat cell (2-column grid)
// ─────────────────────────────────────────────
@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LightCard)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = value,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = label,
                fontSize = 13.sp,
                color    = LightFgSecondary,
            )
        }
    }
}

// ─────────────────────────────────────────────
//  About card
// ─────────────────────────────────────────────
@Composable
private fun AboutCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "О приложении",
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = LightFg,
        )
        Text(
            text     = "Версия 1.0",
            fontSize = 13.sp,
            color    = LightFgSecondary,
        )
    }
}
