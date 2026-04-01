package com.example.cardwords.ui.achievements

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.data.model.AchievementType
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.GreenDark
import com.example.cardwords.ui.theme.Indigo
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface0
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextHeading
import com.example.cardwords.ui.theme.TextSecondary
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.util.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { AchievementsViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Достижения",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "\u2190",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Header
                AchievementsHeader(
                    unlockedCount = uiState.unlockedCount,
                    totalCount = uiState.totalCount,
                )
            }

            items(uiState.achievements) { item ->
                AchievementCard(
                    item = item,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AchievementsHeader(
    unlockedCount: Int,
    totalCount: Int,
) {
    val bgColor = Surface2
    val textColor = Amber

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83C\uDFC6",
                fontSize = 48.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$unlockedCount / $totalCount",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (unlockedCount == totalCount) "Все достижения получены!"
                else "Продолжайте учиться, чтобы открыть все!",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun AchievementCard(
    item: AchievementItem,
) {
    val bgColor = if (item.isUnlocked) {
        Surface1
    } else {
        Surface0
    }

    val contentAlpha = if (item.isUnlocked) 1f else 0.65f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isUnlocked) 1f else 0.85f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUnlocked) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (item.isUnlocked) Surface2 else Surface1
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (item.isUnlocked) item.type.emoji else "\uD83D\uDD12",
                    fontSize = 26.sp,
                    modifier = Modifier.alpha(contentAlpha),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.type.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
                if (item.isUnlocked && item.unlockedAt > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\u2705 ${DateUtil.epochMillisToDateString(item.unlockedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green60,
                    )
                }
            }
        }
    }
}
