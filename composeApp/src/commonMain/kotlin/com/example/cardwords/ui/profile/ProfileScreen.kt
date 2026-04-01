package com.example.cardwords.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.AmberLight
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.TextHeading
import com.example.cardwords.ui.theme.TextPrimary
import com.example.cardwords.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() },
) {
    val state by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AmberDark),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDC64",
                fontSize = 36.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "\u0423\u0440\u043E\u0432\u0435\u043D\u044C ${state.currentLevel}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Amber,
        )

        val (xpCurrent, xpNeeded) = state.xpProgress
        val xpFraction = if (xpNeeded > 0) xpCurrent.toFloat() / xpNeeded else 0f

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { xpFraction },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Amber,
            trackColor = Surface1,
            strokeCap = StrokeCap.Round,
        )

        Text(
            text = "$xpCurrent / $xpNeeded XP",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )

        Spacer(Modifier.height(24.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                emoji = "\uD83D\uDCDA",
                value = "${state.totalWords}",
                label = "\u0421\u043B\u043E\u0432",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                emoji = "\u2B50",
                value = "${state.wordsMastered}",
                label = "\u0412\u044B\u0443\u0447\u0435\u043D\u043E",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                emoji = "\uD83D\uDD25",
                value = "${state.currentStreak}",
                label = "\u0421\u0442\u0440\u0438\u043A",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                emoji = "\u26A1",
                value = "${state.totalXp}",
                label = "\u041E\u043F\u044B\u0442",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                emoji = "\uD83D\uDDD3\uFE0F",
                value = "${state.totalReviews}",
                label = "\u041F\u043E\u0432\u0442\u043E\u0440\u0435\u043D\u0438\u0439",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                emoji = "\u2744\uFE0F",
                value = "${state.streakFreezes}",
                label = "\u0417\u0430\u043C\u043E\u0440\u043E\u0437\u043A\u0438",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface1),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "\u041E \u043F\u0440\u0438\u043B\u043E\u0436\u0435\u043D\u0438\u0438",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextHeading,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "CardWords \u2014 \u0438\u0437\u0443\u0447\u0430\u0439\u0442\u0435 \u0441\u043B\u043E\u0432\u0430 \u0441 \u043F\u043E\u043C\u043E\u0449\u044C\u044E \u043A\u0430\u0440\u0442\u043E\u0447\u0435\u043A \u0438 \u0438\u0433\u0440.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    text = "\u0412\u0435\u0440\u0441\u0438\u044F 1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface1),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextHeading,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}
