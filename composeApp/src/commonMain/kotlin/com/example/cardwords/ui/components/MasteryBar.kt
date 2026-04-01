package com.example.cardwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.data.model.WordProgress
import com.example.cardwords.ui.study.StudyMode

/**
 * Compact mastery summary: shows overall mastery level as a labeled badge.
 * E.g. "★3/5" with color indicating progress.
 */
@Composable
fun MiniMasteryBar(
    progressMap: Map<StudyMode, WordProgress>,
    modifier: Modifier = Modifier,
) {
    if (progressMap.isEmpty()) return

    val level = progressMap.values.minOf { it.masteryLevel }

    val label = masteryLabel(level)
    val bgColor = masteryBgColor(level)
    val textColor = masteryTextColor(level)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = textColor,
        )
    }
}

private fun masteryLabel(level: Int): String = when {
    level >= 5 -> "\u0412\u044B\u0443\u0447\u0435\u043D\u043E"
    level >= 4 -> "\u0417\u043D\u0430\u044E"
    level >= 1 -> "\u0423\u0447\u0443"
    else -> "\u041D\u043E\u0432\u043E\u0435"
}

fun masteryLabelForLevel(level: Int): String = when {
    level >= 5 -> "\u0412\u044B\u0443\u0447\u0435\u043D\u043E"
    level >= 4 -> "\u0417\u043D\u0430\u044E"
    level >= 1 -> "\u0423\u0447\u0443"
    else -> "\u041D\u043E\u0432\u043E\u0435"
}

private fun masteryBgColor(level: Int): Color = when {
    level >= 5 -> Color(0xFFFFD700).copy(alpha = 0.15f)
    level >= 4 -> Color(0xFF2E7D32).copy(alpha = 0.15f)
    level >= 1 -> Color(0xFF1565C0).copy(alpha = 0.15f)
    else -> Color(0xFFE65100).copy(alpha = 0.15f)
}

private fun masteryTextColor(level: Int): Color = when {
    level >= 5 -> Color(0xFFFFD700)
    level >= 4 -> Color(0xFF4CAF50)
    level >= 1 -> Color(0xFF64B5F6)
    else -> Color(0xFFFF9800)
}

// --- Review Temperature (Forgetting Curve) ---

enum class ReviewTemperature { OVERDUE, DUE_SOON, COOL, NO_DATA }

private const val MILLIS_24H = 24L * 60 * 60 * 1000

fun computeReviewTemperature(
    progressMap: Map<StudyMode, WordProgress>,
    now: Long,
): ReviewTemperature {
    if (progressMap.isEmpty()) return ReviewTemperature.NO_DATA

    val validEntries = progressMap.values.filter { it.nextReviewAt > 0 }
    if (validEntries.isEmpty()) return ReviewTemperature.NO_DATA

    val earliestReview = validEntries.minOf { it.nextReviewAt }

    return when {
        earliestReview <= now -> ReviewTemperature.OVERDUE
        earliestReview <= now + MILLIS_24H -> ReviewTemperature.DUE_SOON
        else -> ReviewTemperature.COOL
    }
}

private fun temperatureLabel(temp: ReviewTemperature): String = when (temp) {
    ReviewTemperature.OVERDUE -> "\uD83D\uDD34 \u041F\u043E\u0432\u0442\u043E\u0440\u0438\u0442\u044C"
    ReviewTemperature.DUE_SOON -> "\uD83D\uDFE1 \u0421\u043A\u043E\u0440\u043E"
    ReviewTemperature.COOL -> "\uD83D\uDFE2 \u041E\u043A"
    ReviewTemperature.NO_DATA -> ""
}

private fun temperatureBgColor(temp: ReviewTemperature): Color = when (temp) {
    ReviewTemperature.OVERDUE -> Color(0xFFEF5350).copy(alpha = 0.15f)
    ReviewTemperature.DUE_SOON -> Color(0xFFFFA726).copy(alpha = 0.15f)
    ReviewTemperature.COOL -> Color(0xFF66BB6A).copy(alpha = 0.15f)
    ReviewTemperature.NO_DATA -> Color.Transparent
}

private fun temperatureTextColor(temp: ReviewTemperature): Color = when (temp) {
    ReviewTemperature.OVERDUE -> Color(0xFFEF5350)
    ReviewTemperature.DUE_SOON -> Color(0xFFFFA726)
    ReviewTemperature.COOL -> Color(0xFF66BB6A)
    ReviewTemperature.NO_DATA -> Color.Transparent
}

@Composable
fun ReviewTemperatureIndicator(
    temperature: ReviewTemperature,
    modifier: Modifier = Modifier,
) {
    if (temperature == ReviewTemperature.NO_DATA) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(temperatureBgColor(temperature))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = temperatureLabel(temperature),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = temperatureTextColor(temperature),
        )
    }
}
