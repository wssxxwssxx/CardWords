package com.example.cardwords.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.Amber40
import com.example.cardwords.ui.theme.Green40
import com.example.cardwords.ui.theme.Green60
import com.example.cardwords.ui.theme.Indigo
import com.example.cardwords.ui.theme.Orange40
import com.example.cardwords.ui.theme.Surface2

@Composable
fun OnboardingPage(page: Int, isCurrentPage: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        when (page) {
            0 -> SmartSessionPage(isCurrentPage)
            1 -> DailyGoalPage(isCurrentPage)
            2 -> MasterySystemPage(isCurrentPage)
            3 -> DictionaryDotsPage(isCurrentPage)
        }

        Spacer(modifier = Modifier.weight(0.4f))
    }
}

// ═══════════════════════════════════════════════════════════════
// Page 0: Smart Session
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SmartSessionPage(isCurrentPage: Boolean) {
    val pulseTransition = rememberInfiniteTransition()
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val orbitTransition = rememberInfiniteTransition()
    val orbitAngle by orbitTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    // Visual: green gradient card with lightning and orbiting cards
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Green40, Green60))),
        contentAlignment = Alignment.Center,
    ) {
        // Orbiting word cards
        val radius = 70f
        listOf(0f, 120f, 240f).forEachIndexed { index, baseAngle ->
            val angle = Math.toRadians((orbitAngle + baseAngle).toDouble())
            val offsetX = (radius * kotlin.math.cos(angle)).toFloat()
            val offsetY = (radius * kotlin.math.sin(angle)).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(40.dp, 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f + index * 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = listOf("Aa", "Bb", "Cc")[index],
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Central lightning icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\u26A1", fontSize = 36.sp)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "\u0423\u043C\u043D\u0430\u044F \u0441\u0435\u0441\u0441\u0438\u044F",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "\u0410\u043B\u0433\u043E\u0440\u0438\u0442\u043C \u0441\u0430\u043C \u0432\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u0441\u043B\u043E\u0432\u0430 \u0434\u043B\u044F \u043F\u043E\u0432\u0442\u043E\u0440\u0435\u043D\u0438\u044F.\n" +
                "\n\u041E\u043D \u0443\u0447\u0438\u0442\u044B\u0432\u0430\u0435\u0442:" +
                "\n\u2022 \u041A\u0430\u043A\u0438\u0435 \u0441\u043B\u043E\u0432\u0430 \u043F\u043E\u0440\u0430 \u043F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C" +
                "\n\u2022 \u041A\u0430\u043A\u0438\u0435 \u0441\u043B\u043E\u0432\u0430 \u0432\u044B \u0437\u043D\u0430\u0435\u0442\u0435 \u0445\u0443\u0436\u0435 \u0432\u0441\u0435\u0433\u043E" +
                "\n\u2022 \u041D\u043E\u0432\u044B\u0435 \u0441\u043B\u043E\u0432\u0430 \u0438\u0437 \u0432\u0430\u0448\u0435\u0433\u043E \u0441\u043B\u043E\u0432\u0430\u0440\u044F" +
                "\n\n\u041F\u0440\u043E\u0441\u0442\u043E \u043D\u0430\u0436\u043C\u0438\u0442\u0435 \u26A1 \u043D\u0430 \u0433\u043B\u0430\u0432\u043D\u043E\u043C \u044D\u043A\u0440\u0430\u043D\u0435 \u2014\n\u043E\u0441\u0442\u0430\u043B\u044C\u043D\u043E\u0435 \u0441\u0434\u0435\u043B\u0430\u0435\u0442 \u0430\u043B\u0433\u043E\u0440\u0438\u0442\u043C.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Start,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ═══════════════════════════════════════════════════════════════
// Page 1: Daily Goal Ring
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DailyGoalPage(isCurrentPage: Boolean) {
    val ringProgress = remember { Animatable(0f) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            ringProgress.snapTo(0f)
            ringProgress.animateTo(
                targetValue = 0.7f,
                animationSpec = tween(1500, easing = EaseInOut),
            )
        }
    }

    val ringColor = when {
        ringProgress.value > 0.5f -> Amber40
        else -> Indigo
    }
    val trackColor = Surface2

    // Visual: Large animated ring
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = ringProgress.value * 360f,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(ringProgress.value * 10).toInt()}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
            Text(
                text = "/ 10",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Streak and XP badges
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uD83D\uDD25 7 \u0434\u043D\u0435\u0439 \u043F\u043E\u0434\u0440\u044F\u0434",
            style = MaterialTheme.typography.labelLarge,
            color = Orange40,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "\u26A1 \u0423\u0440. 3",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFF8F00),
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "\u0414\u043D\u0435\u0432\u043D\u0430\u044F \u0446\u0435\u043B\u044C",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "\u041A\u043E\u043B\u044C\u0446\u043E \u043D\u0430 \u0433\u043B\u0430\u0432\u043D\u043E\u043C \u044D\u043A\u0440\u0430\u043D\u0435 \u043F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442\n\u0432\u0430\u0448 \u043F\u0440\u043E\u0433\u0440\u0435\u0441\u0441 \u0437\u0430 \u0434\u0435\u043D\u044C." +
                "\n\n\u0426\u0435\u043B\u044C \u043F\u043E \u0443\u043C\u043E\u043B\u0447\u0430\u043D\u0438\u044E \u2014 10 \u0441\u043B\u043E\u0432 \u0432 \u0434\u0435\u043D\u044C." +
                "\n\u041A\u043E\u0433\u0434\u0430 \u043A\u043E\u043B\u044C\u0446\u043E \u0437\u0430\u043F\u043E\u043B\u043D\u0438\u0442\u0441\u044F \u2014 \u0446\u0435\u043B\u044C \u0434\u043E\u0441\u0442\u0438\u0433\u043D\u0443\u0442\u0430! \u2713" +
                "\n\n\uD83D\uDD25 \u0417\u0430\u043D\u0438\u043C\u0430\u0439\u0442\u0435\u0441\u044C \u043A\u0430\u0436\u0434\u044B\u0439 \u0434\u0435\u043D\u044C, \u0447\u0442\u043E\u0431\u044B \u043D\u0435 \u043F\u0440\u0435\u0440\u044B\u0432\u0430\u0442\u044C \u0441\u0435\u0440\u0438\u044E." +
                "\n\u26A1 \u0417\u0430 \u043A\u0430\u0436\u0434\u043E\u0435 \u0441\u043B\u043E\u0432\u043E \u0432\u044B \u043F\u043E\u043B\u0443\u0447\u0430\u0435\u0442\u0435 XP \u0438 \u0440\u0430\u0441\u0442\u0451\u0442\u0435 \u0432 \u0443\u0440\u043E\u0432\u043D\u0435.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Start,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ═══════════════════════════════════════════════════════════════
// Page 2: Mastery System
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MasterySystemPage(isCurrentPage: Boolean) {
    data class MasteryStep(
        val label: String,
        val color: Color,
        val icon: String,
    )

    val steps = listOf(
        MasteryStep("\u041D\u043E\u0432\u043E\u0435", Color(0xFF9E9E9E), "\u25CB"),
        MasteryStep("\u0412\u0438\u0434\u0435\u043B", Color(0xFF90CAF9), "\u25D4"),
        MasteryStep("\u0417\u043D\u0430\u043A\u043E\u043C\u043E\u0435", Color(0xFF64B5F6), "\u25D1"),
        MasteryStep("\u0423\u0447\u0443", Color(0xFFFFB74D), "\u25D5"),
        MasteryStep("\u0417\u043D\u0430\u044E", Color(0xFF81C784), "\u25D5"),
        MasteryStep("\u041E\u0441\u0432\u043E\u0435\u043D\u043E", Color(0xFFFFD700), "\u2B50"),
    )

    val climbProgress = remember { Animatable(0f) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            climbProgress.snapTo(0f)
            climbProgress.animateTo(
                targetValue = 5f,
                animationSpec = tween(3000, easing = EaseInOut),
            )
        }
    }

    // Visual: Mastery ladder
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        steps.reversed().forEachIndexed { reverseIndex, step ->
            val stepIndex = steps.size - 1 - reverseIndex
            val isReached = climbProgress.value >= stepIndex.toFloat()
            val alpha = if (isReached) 1f else 0.35f

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Step dot
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(step.color.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = step.icon,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = alpha),
                    )
                }

                // Step bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(step.color.copy(alpha = alpha * 0.25f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal,
                        color = if (isReached) step.color else step.color.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4 study mode icons
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "\uD83C\uDFAF" to "\u0422\u0435\u0441\u0442",
            "\uD83D\uDCCB" to "\u041A\u0430\u0440\u0442\u043E\u0447\u043A\u0438",
            "\u270D\uFE0F" to "\u0412\u0432\u043E\u0434",
            "\uD83D\uDD24" to "\u0421\u0431\u043E\u0440\u043A\u0430",
        ).forEach { (emoji, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "\u0421\u0438\u0441\u0442\u0435\u043C\u0430 \u043E\u0441\u0432\u043E\u0435\u043D\u0438\u044F",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "\u041A\u0430\u0436\u0434\u043E\u0435 \u0441\u043B\u043E\u0432\u043E \u043F\u0440\u043E\u0445\u043E\u0434\u0438\u0442 6 \u0443\u0440\u043E\u0432\u043D\u0435\u0439." +
                "\n\u0414\u043B\u044F \u043F\u0440\u043E\u0434\u0432\u0438\u0436\u0435\u043D\u0438\u044F \u043D\u0443\u0436\u043D\u044B 4 \u0440\u0435\u0436\u0438\u043C\u0430 \u0437\u0430\u043D\u044F\u0442\u0438\u0439." +
                "\n\n\u0418\u043D\u0442\u0435\u0440\u0432\u0430\u043B\u044B \u043C\u0435\u0436\u0434\u0443 \u043F\u043E\u0432\u0442\u043E\u0440\u0435\u043D\u0438\u044F\u043C\u0438 \u0440\u0430\u0441\u0442\u0443\u0442:" +
                "\n4\u0447 \u2192 1 \u0434\u0435\u043D\u044C \u2192 3 \u0434\u043D\u044F \u2192 7 \u0434\u043D\u0435\u0439 \u2192 30 \u0434\u043D\u0435\u0439" +
                "\n\n\u0427\u0435\u043C \u043B\u0443\u0447\u0448\u0435 \u0432\u044B \u0437\u043D\u0430\u0435\u0442\u0435 \u0441\u043B\u043E\u0432\u043E,\n\u0442\u0435\u043C \u0440\u0435\u0436\u0435 \u043E\u043D\u043E \u043F\u043E\u0432\u0442\u043E\u0440\u044F\u0435\u0442\u0441\u044F.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Start,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ═══════════════════════════════════════════════════════════════
// Page 3: Dictionary Dots
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DictionaryDotsPage(isCurrentPage: Boolean) {
    // Visual: Mock dictionary card with actual badge styles
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            data class MockWord(
                val original: String,
                val translation: String,
                val tempLabel: String,
                val tempBgColor: Color,
                val tempTextColor: Color,
                val masteryLabel: String,
                val masteryBgColor: Color,
                val masteryTextColor: Color,
            )

            val mockWords = listOf(
                MockWord(
                    "apple", "\u044F\u0431\u043B\u043E\u043A\u043E",
                    "\uD83D\uDD34 \u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C",
                    Color(0xFFEF5350).copy(alpha = 0.15f), Color(0xFFEF5350),
                    "\u0423\u0447\u0443",
                    Color(0xFF1565C0).copy(alpha = 0.15f), Color(0xFF64B5F6),
                ),
                MockWord(
                    "book", "\u043A\u043D\u0438\u0433\u0430",
                    "\uD83D\uDFE1 \u0421\u043A\u043E\u0440\u043E",
                    Color(0xFFFFA726).copy(alpha = 0.15f), Color(0xFFFFA726),
                    "\u0417\u043D\u0430\u044E",
                    Color(0xFF2E7D32).copy(alpha = 0.15f), Color(0xFF4CAF50),
                ),
                MockWord(
                    "cat", "\u043A\u043E\u0442",
                    "\uD83D\uDFE2 \u041E\u043A",
                    Color(0xFF66BB6A).copy(alpha = 0.15f), Color(0xFF66BB6A),
                    "\u0412\u044B\u0443\u0447\u0435\u043D\u043E",
                    Color(0xFFFFD700).copy(alpha = 0.15f), Color(0xFFFFD700),
                ),
            )

            mockWords.forEach { word ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = word.original,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = word.translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }

                    // Temperature badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(word.tempBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = word.tempLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = word.tempTextColor,
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mastery badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(word.masteryBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = word.masteryLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = word.masteryTextColor,
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "\u0421\u043B\u043E\u0432\u0430\u0440\u044C \u043F\u043E\u0434\u0441\u043A\u0430\u0436\u0435\u0442",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "\u0412 \u0441\u043B\u043E\u0432\u0430\u0440\u0435 \u0440\u044F\u0434\u043E\u043C \u0441 \u043A\u0430\u0436\u0434\u044B\u043C \u0441\u043B\u043E\u0432\u043E\u043C \u0432\u044B \u0443\u0432\u0438\u0434\u0438\u0442\u0435:" +
                "\n\n\u041A\u043E\u0433\u0434\u0430 \u043F\u043E\u0432\u0442\u043E\u0440\u044F\u0442\u044C:" +
                "\n\uD83D\uDD34 \u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C \u2014 \u0441\u043B\u043E\u0432\u043E \u043F\u0440\u043E\u0441\u0440\u043E\u0447\u0435\u043D\u043E" +
                "\n\uD83D\uDFE1 \u0421\u043A\u043E\u0440\u043E \u2014 \u043F\u043E\u0432\u0442\u043E\u0440\u0435\u043D\u0438\u0435 \u0432 \u0431\u043B\u0438\u0436\u0430\u0439\u0448\u0438\u0435 24\u0447" +
                "\n\uD83D\uDFE2 \u041E\u043A \u2014 \u0441\u043B\u043E\u0432\u043E \u0441\u0432\u0435\u0436\u0435\u0435 \u0432 \u043F\u0430\u043C\u044F\u0442\u0438" +
                "\n\n\u0423\u0440\u043E\u0432\u0435\u043D\u044C \u043E\u0441\u0432\u043E\u0435\u043D\u0438\u044F:" +
                "\n\u041D\u043E\u0432\u043E\u0435 \u2192 \u0423\u0447\u0443 \u2192 \u0417\u043D\u0430\u044E \u2192 \u0412\u044B\u0443\u0447\u0435\u043D\u043E" +
                "\n\n\u041D\u0430\u0436\u043C\u0438\u0442\u0435 \u043D\u0430 \u0441\u043B\u043E\u0432\u043E, \u0447\u0442\u043E\u0431\u044B \u0443\u0432\u0438\u0434\u0435\u0442\u044C\n\u043F\u043E\u0434\u0440\u043E\u0431\u043D\u0443\u044E \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043A\u0443 \u043F\u043E \u043A\u0430\u0436\u0434\u043E\u043C\u0443 \u0440\u0435\u0436\u0438\u043C\u0443.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Start,
        lineHeight = 24.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}
