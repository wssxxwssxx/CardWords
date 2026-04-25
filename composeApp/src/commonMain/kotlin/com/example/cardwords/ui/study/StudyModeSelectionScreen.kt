package com.example.cardwords.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.LightProgressBar

// ─────────────────────────────────────────────
//  SVG path data (viewBox 24×24, stroke-width 1.8)
// ─────────────────────────────────────────────
private object IconPaths {
    // Тест — concentric circles (Frame 8)
    val TEST_OUTER  = "M12 21C16.9706 21 21 16.9706 21 12C21 7.02944 16.9706 3 12 3C7.02944 3 3 7.02944 3 12C3 16.9706 7.02944 21 12 21Z"
    val TEST_MIDDLE = "M12 17C14.7614 17 17 14.7614 17 12C17 9.23858 14.7614 7 12 7C9.23858 7 7 9.23858 7 12C7 14.7614 9.23858 17 12 17Z"
    val TEST_DOT    = "M12 13.5C12.8284 13.5 13.5 12.8284 13.5 12C13.5 11.1716 12.8284 10.5 12 10.5C11.1716 10.5 10.5 11.1716 10.5 12C10.5 12.8284 11.1716 13.5 12 13.5Z"

    // Ввод — keyboard (Frame 9)
    val KB_OUTER = "M19 5H5C3.34315 5 2 6.34315 2 8V16C2 17.6569 3.34315 19 5 19H19C20.6569 19 22 17.6569 22 16V8C22 6.34315 20.6569 5 19 5Z"
    val KB_K1    = "M7.5 8H5.5C5.22386 8 5 8.22386 5 8.5V9.5C5 9.77614 5.22386 10 5.5 10H7.5C7.77614 10 8 9.77614 8 9.5V8.5C8 8.22386 7.77614 8 7.5 8Z"
    val KB_K2    = "M12.5 8H10.5C10.2239 8 10 8.22386 10 8.5V9.5C10 9.77614 10.2239 10 10.5 10H12.5C12.7761 10 13 9.77614 13 9.5V8.5C13 8.22386 12.7761 8 12.5 8Z"
    val KB_K3    = "M17.5 8H15.5C15.2239 8 15 8.22386 15 8.5V9.5C15 9.77614 15.2239 10 15.5 10H17.5C17.7761 10 18 9.77614 18 9.5V8.5C18 8.22386 17.7761 8 17.5 8Z"
    val KB_K4    = "M7.5 12H5.5C5.22386 12 5 12.2239 5 12.5V13.5C5 13.7761 5.22386 14 5.5 14H7.5C7.77614 14 8 13.7761 8 13.5V12.5C8 12.2239 7.77614 12 7.5 12Z"
    val KB_K5    = "M12.5 12H10.5C10.2239 12 10 12.2239 10 12.5V13.5C10 13.7761 10.2239 14 10.5 14H12.5C12.7761 14 13 13.7761 13 13.5V12.5C13 12.2239 12.7761 12 12.5 12Z"
    val KB_K6    = "M17.5 12H15.5C15.2239 12 15 12.2239 15 12.5V13.5C15 13.7761 15.2239 14 15.5 14H17.5C17.7761 14 18 13.7761 18 13.5V12.5C18 12.2239 17.7761 12 17.5 12Z"
    val KB_SPACE = "M15.5 16H8.5C8.22386 16 8 16.2239 8 16.5V17.5C8 17.7761 8.22386 18 8.5 18H15.5C15.7761 18 16 17.7761 16 17.5V16.5C16 16.2239 15.7761 16 15.5 16Z"

    // Сборка — 4 squares grid (Frame 10)
    val GRID_TL = "M9 3H5C3.89543 3 3 3.89543 3 5V9C3 10.1046 3.89543 11 5 11H9C10.1046 11 11 10.1046 11 9V5C11 3.89543 10.1046 3 9 3Z"
    val GRID_TR = "M19 3H15C13.8954 3 13 3.89543 13 5V9C13 10.1046 13.8954 11 15 11H19C20.1046 11 21 10.1046 21 9V5C21 3.89543 20.1046 3 19 3Z"
    val GRID_BL = "M9 13H5C3.89543 13 3 13.8954 3 15V19C3 20.1046 3.89543 21 5 21H9C10.1046 21 11 20.1046 11 19V15C11 13.8954 10.1046 13 9 13Z"
    val GRID_BR = "M19 13H15C13.8954 13 13 13.8954 13 15V19C13 20.1046 13.8954 21 15 21H19C20.1046 21 21 20.1046 21 19V15C21 13.8954 20.1046 13 19 13Z"
}

private data class ModeItem(
    val mode: StudyMode,
    val title: String,
    val subtitle: String,
)

private val modeItems = listOf(
    ModeItem(StudyMode.MULTIPLE_CHOICE, "Тест",     "Выберите правильный перевод\nиз 4 вариантов"),
    ModeItem(StudyMode.FLASHCARD,       "Карточки", "Переворачивайте карточки и\nоценивайте себя"),
    ModeItem(StudyMode.TYPING,          "Ввод",     "Напишите перевод слова\nсамостоятельно"),
    ModeItem(StudyMode.LETTER_ASSEMBLY, "Сборка",   "Соберите перевод слова\nиз букв"),
)

// ─────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────
@Composable
fun StudyModeSelectionScreen(
    onNext: (Set<StudyMode>) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var selectedModes by remember { mutableStateOf(StudyMode.entries.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .safeContentPadding(),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Header row: back chevron + title ─────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back chevron
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val s  = size.width / 24f
                    val sw = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(
                        path = Path().apply {
                            moveTo(14f * s, 5f * s)
                            lineTo(8f * s, 12f * s)
                            lineTo(14f * s, 19f * s)
                        },
                        color = LightFg,
                        style = sw,
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text       = "Режим обучения",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text     = "Выберите режимы",
            fontSize = 15.sp,
            color    = LightFgSecondary,
            modifier = Modifier.padding(horizontal = 22.dp),
        )

        Spacer(Modifier.height(20.dp))

        // ── Mode cards ───────────────────────────────────
        Column(
            modifier            = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            modeItems.forEach { item ->
                ModeCard(
                    item       = item,
                    isSelected = item.mode in selectedModes,
                    onToggle   = { selectedModes = toggleMode(selectedModes, item.mode) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Bottom ───────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text     = "Выбрано режимов: ${selectedModes.size}",
                fontSize = 14.sp,
                color    = LightFgSecondary,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightFg)
                    .clickable { onNext(selectedModes) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Далее",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Mode card
// ─────────────────────────────────────────────
@Composable
private fun ModeCard(
    item: ModeItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val checkScale by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LightCard)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon box — gray rounded square
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LightFgMuted.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            ModeIconCanvas(mode = item.mode)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = item.title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text       = item.subtitle,
                fontSize   = 13.sp,
                color      = LightFgSecondary,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Blue checkmark — animates in/out
        Text(
            text       = "✓",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = LightProgressBar,
            modifier   = Modifier.scale(checkScale),
        )
    }
}

// ─────────────────────────────────────────────
//  Mode icon canvas
// ─────────────────────────────────────────────
@Composable
private fun ModeIconCanvas(mode: StudyMode) {
    val color = LightFgSecondary
    when (mode) {

        StudyMode.MULTIPLE_CHOICE -> Canvas(Modifier.size(24.dp)) {
            val s  = size.width / 24f
            // stroke width in SVG units — scale() will convert to pixels
            val sw = Stroke(1.8f)
            svgPath(IconPaths.TEST_OUTER,  s) { drawPath(it, color, style = sw) }
            svgPath(IconPaths.TEST_MIDDLE, s) { drawPath(it, color, style = sw) }
            svgPath(IconPaths.TEST_DOT,    s) { drawPath(it, color) }
        }

        StudyMode.FLASHCARD -> Canvas(Modifier.size(24.dp)) {
            val s  = size.width / 24f
            val sw = Stroke(1.8f * s, cap = StrokeCap.Round)
            val iconBg = LightFgMuted.copy(alpha = 0.18f)
            // Back card (no fill — partly hidden by front card bg)
            drawRoundRect(
                color        = color,
                topLeft      = Offset(2f * s, 2f * s),
                size         = Size(14f * s, 14f * s),
                cornerRadius = CornerRadius(2f * s),
                style        = sw,
            )
            // Front card — bg to erase overlap, then stroke
            drawRoundRect(
                color        = iconBg,
                topLeft      = Offset(8f * s, 8f * s),
                size         = Size(14f * s, 14f * s),
                cornerRadius = CornerRadius(2f * s),
            )
            drawRoundRect(
                color        = color,
                topLeft      = Offset(8f * s, 8f * s),
                size         = Size(14f * s, 14f * s),
                cornerRadius = CornerRadius(2f * s),
                style        = sw,
            )
        }

        StudyMode.TYPING -> Canvas(Modifier.size(24.dp)) {
            val s  = size.width / 24f
            val sw = Stroke(1.8f)
            svgPath(IconPaths.KB_OUTER, s) { drawPath(it, color, style = sw) }
            listOf(
                IconPaths.KB_K1, IconPaths.KB_K2, IconPaths.KB_K3,
                IconPaths.KB_K4, IconPaths.KB_K5, IconPaths.KB_K6,
                IconPaths.KB_SPACE,
            ).forEach { d -> svgPath(d, s) { drawPath(it, color) } }
        }

        StudyMode.LETTER_ASSEMBLY -> Canvas(Modifier.size(24.dp)) {
            val s  = size.width / 24f
            val sw = Stroke(1.8f)
            listOf(IconPaths.GRID_TL, IconPaths.GRID_TR, IconPaths.GRID_BL, IconPaths.GRID_BR)
                .forEach { d -> svgPath(d, s) { drawPath(it, color, style = sw) } }
        }
    }
}

// ─────────────────────────────────────────────
//  DrawScope helper
// ─────────────────────────────────────────────
private inline fun DrawScope.svgPath(
    pathData: String,
    s: Float,
    crossinline draw: DrawScope.(Path) -> Unit,
) {
    val path = PathParser().parsePathString(pathData).toPath()
    // NOTE: do NOT pre-multiply stroke widths by s — scale() will handle that
    scale(s, s, pivot = Offset.Zero) { draw(path) }
}

private fun toggleMode(current: Set<StudyMode>, mode: StudyMode): Set<StudyMode> =
    if (mode in current) {
        if (current.size > 1) current - mode else current
    } else {
        current + mode
    }
