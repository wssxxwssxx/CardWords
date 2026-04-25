package com.example.cardwords.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardwords.data.model.OnboardingManager
import com.example.cardwords.di.AppModule
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val repository    = AppModule.databaseRepository
    val pagerState    = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val isLast        = pagerState.currentPage == PAGE_COUNT - 1

    fun finish() {
        OnboardingManager.setOnboardingCompleted(repository)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .safeContentPadding(),
    ) {
        // Skip row
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (!isLast) {
                TextButton(onClick = { finish() }) {
                    Text(
                        text       = "Пропустить",
                        fontSize   = 14.sp,
                        color      = LightFgSecondary,
                    )
                }
            }
        }

        // Pages
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            OnboardingPage(page = page, isCurrentPage = pagerState.currentPage == page)
        }

        // Dots + button
        Column(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.spacedBy(24.dp),
        ) {
            // Pill-style dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGE_COUNT) { i ->
                    val selected = i == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue   = if (selected) 24.dp else 8.dp,
                        animationSpec = tween(250),
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (selected) LightFg else LightFgMuted.copy(alpha = 0.4f)),
                    )
                }
            }

            // Action button — full black
            Button(
                onClick = {
                    if (!isLast) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        finish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightFg,
                    contentColor   = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text(
                    text       = if (!isLast) "Далее" else "Поехали!",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
