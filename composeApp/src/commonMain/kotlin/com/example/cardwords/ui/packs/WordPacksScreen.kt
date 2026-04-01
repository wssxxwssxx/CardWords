package com.example.cardwords.ui.packs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cardwords.ui.theme.Amber
import com.example.cardwords.ui.theme.AmberDark
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.GreenDark
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextHeading
import com.example.cardwords.ui.theme.TextMuted
import com.example.cardwords.ui.theme.TextPrimary
import com.example.cardwords.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPacksScreen(
    onNavigateToPackDetail: (String) -> Unit,
    onNavigateToWordSearch: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel = remember { WordPacksViewModel() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Наборы слов",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Search card
            SearchWordCard(onClick = onNavigateToWordSearch)

            Spacer(modifier = Modifier.height(16.dp))

            // By Level section
            SectionHeader(title = "По уровню")
            Spacer(modifier = Modifier.height(12.dp))
            uiState.byLevel.forEach { overview ->
                PackCard(
                    overview = overview,
                    onClick = { onNavigateToPackDetail(overview.pack.id) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // By Topic section
            SectionHeader(title = "По теме")
            Spacer(modifier = Modifier.height(12.dp))
            uiState.byTopic.forEach { overview ->
                PackCard(
                    overview = overview,
                    onClick = { onNavigateToPackDetail(overview.pack.id) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Special section
            SectionHeader(title = "Особые")
            Spacer(modifier = Modifier.height(12.dp))
            uiState.special.forEach { overview ->
                PackCard(
                    overview = overview,
                    onClick = { onNavigateToPackDetail(overview.pack.id) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextHeading,
    )
}

@Composable
private fun PackCard(
    overview: PackOverview,
    onClick: () -> Unit,
) {
    val pack = overview.pack
    val addedCount = overview.inDictionaryCount.toInt()
    val totalCount = pack.wordCount
    val isComplete = addedCount >= totalCount && totalCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface1,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pack.emoji,
                fontSize = 32.sp,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHeading,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = pack.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { overview.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isComplete) Green60 else Amber,
                    trackColor = Surface2,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isComplete) "Все слова добавлены"
                    else "$addedCount / $totalCount слов",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isComplete) Green60 else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SearchWordCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AmberDark,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDD0D",
                fontSize = 28.sp,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Найти слово",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHeading,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Поиск в базе или онлайн",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
