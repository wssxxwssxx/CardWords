package com.example.cardwords.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary

@Composable
fun OnboardingPage(page: Int, isCurrentPage: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.1f))

        when (page) {
            0 -> SmartSessionPage(isCurrentPage)
            1 -> DailyGoalPage(isCurrentPage)
            2 -> DictionaryPage()
            3 -> GamesPage(isCurrentPage)
        }

        Spacer(Modifier.weight(0.35f))
    }
}

// ─────────────────────────────────────────────
//  Page 0 — Умная сессия
// ─────────────────────────────────────────────
@Composable
private fun SmartSessionPage(isCurrentPage: Boolean) {
    val orbit = rememberInfiniteTransition()
    val angle by orbit.animateFloat(
        initialValue   = 0f,
        targetValue    = 360f,
        animationSpec  = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
    )

    // Illustration: orbiting word chips around a central card
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Orbiting chips
        listOf(0f, 120f, 240f).forEachIndexed { i, base ->
            val rad  = Math.toRadians((angle + base).toDouble())
            val r    = 68f
            val ox   = (r * kotlin.math.cos(rad)).toFloat()
            val oy   = (r * kotlin.math.sin(rad)).toFloat()
            val words = listOf("apple", "book", "run")
            Box(
                modifier = Modifier
                    .offset(ox.dp, oy.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightCard)
                    .border(1.dp, LightFgMuted.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(words[i], fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LightFg)
            }
        }

        // Central black card — mimics the Smart Session card
        Box(
            modifier = Modifier
                .size(width = 130.dp, height = 50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LightFg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "Умная сессия",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }
    }

    Spacer(Modifier.height(28.dp))

    PageTitle("Умная сессия")
    Spacer(Modifier.height(14.dp))
    PageBody(
        "Алгоритм сам выбирает, какие слова повторить. " +
        "Он учитывает:\n" +
        "• Какие слова пора повторить\n" +
        "• Какие слова вы знаете хуже всего\n" +
        "• Новые слова из вашего словаря\n\n" +
        "Просто нажмите «Умная сессия» — остальное сделает алгоритм."
    )
}

// ─────────────────────────────────────────────
//  Page 1 — Дневная цель
// ─────────────────────────────────────────────
@Composable
private fun DailyGoalPage(isCurrentPage: Boolean) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            progress.snapTo(0f)
            progress.animateTo(0.6f, animationSpec = tween(1400, easing = EaseInOut))
        }
    }

    // Illustration: progress ring (same style as HomeScreen)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(Modifier.size(140.dp)) {
            val stroke = 9.dp.toPx()
            val r      = (size.minDimension - stroke) / 2f
            val c      = Offset(size.width / 2f, size.height / 2f)
            // track
            drawCircle(
                color  = LightFgMuted.copy(alpha = 0.3f),
                radius = r, center = c,
                style  = Stroke(stroke),
            )
            // fill
            if (progress.value > 0f) {
                drawArc(
                    color      = LightFg,
                    startAngle = -90f,
                    sweepAngle = progress.value * 360f,
                    useCenter  = false,
                    topLeft    = Offset(c.x - r, c.y - r),
                    size       = Size(r * 2f, r * 2f),
                    style      = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${(progress.value * 10).toInt()}/10",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = LightFg,
            )
            Text("слов", fontSize = 12.sp, color = LightFgSecondary)
        }
    }

    Spacer(Modifier.height(28.dp))

    PageTitle("Дневная цель")
    Spacer(Modifier.height(14.dp))
    PageBody(
        "Кольцо на главном экране показывает ваш прогресс за день.\n\n" +
        "Цель по умолчанию — 10 слов в день.\n" +
        "Когда кольцо заполнится — цель достигнута ✓\n\n" +
        "Занимайтесь каждый день, чтобы не прерывать серию. " +
        "За каждое слово вы получаете XP и растёте в уровне."
    )
}

// ─────────────────────────────────────────────
//  Page 2 — Словарь
// ─────────────────────────────────────────────
@Composable
private fun DictionaryPage() {
    // Illustration: mock word cards
    val words = listOf(
        Triple("apple", "яблоко", "Учу"),
        Triple("book",  "книга",  "Знаю"),
        Triple("run",   "бежать", "Выучено"),
    )

    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        words.forEach { (word, tr, mastery) ->
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(word, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                    Text(tr, fontSize = 12.sp, color = LightFgSecondary)
                }
                // Mastery chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LightFgMuted.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(mastery, fontSize = 11.sp, color = LightFg)
                }
            }
            if (word != words.last().first) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(LightFgMuted.copy(alpha = 0.3f)))
            }
        }
    }

    Spacer(Modifier.height(28.dp))

    PageTitle("Твой словарь")
    Spacer(Modifier.height(14.dp))
    PageBody(
        "Добавляйте слова из готовых паков или вводите свои.\n\n" +
        "Каждое слово проходит 6 уровней освоения:\n" +
        "Новое → Видел → Знакомое → Учу → Знаю → Выучено\n\n" +
        "Чем лучше знаете слово — тем реже оно повторяется. " +
        "Умная сессия сама подберёт нужные слова."
    )
}

// ─────────────────────────────────────────────
//  Page 3 — Игры и тесты
// ─────────────────────────────────────────────
@Composable
private fun GamesPage(isCurrentPage: Boolean) {
    // Illustration: 3 tabs (как на HomeScreen)
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Учёба" to true, "Тест" to false, "Игры" to false).forEach { (label, active) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) LightFg else LightCard)
                    .border(
                        width = if (active) 0.dp else 1.dp,
                        color = LightFgMuted.copy(0.35f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (active) Color.White else LightFg,
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Mode cards
    val modes = listOf(
        "Учить новые" to "Карточки, тест, набор букв",
        "Словопад"    to "Угадывай падающие слова",
        "Подземелье"  to "Рогалик с карточками слов",
        "AI Тест"     to "Вопросы от нейросети",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { (title, sub) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LightFg)
                    Text(sub, fontSize = 12.sp, color = LightFgSecondary)
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LightFgMuted.copy(alpha = 0.5f)),
                )
            }
        }
    }

    Spacer(Modifier.height(28.dp))

    PageTitle("Учись играя")
    Spacer(Modifier.height(14.dp))
    PageBody(
        "Несколько режимов — один словарь.\n\n" +
        "Учёба: карточки, тест, набор букв и сборка слова.\n" +
        "Словопад: угадывай перевод падающего слова.\n" +
        "Подземелье: прокачивай героя, повторяя слова.\n" +
        "AI Тест: нейросеть составляет предложения — вставь нужное слово."
    )
}

// ─────────────────────────────────────────────
//  Common text helpers
// ─────────────────────────────────────────────
@Composable
private fun PageTitle(text: String) {
    Text(
        text       = text,
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        color      = LightFg,
        textAlign  = TextAlign.Center,
    )
}

@Composable
private fun PageBody(text: String) {
    Text(
        text       = text,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
        color      = LightFgSecondary,
        textAlign  = TextAlign.Start,
        modifier   = Modifier.fillMaxWidth(),
    )
}
