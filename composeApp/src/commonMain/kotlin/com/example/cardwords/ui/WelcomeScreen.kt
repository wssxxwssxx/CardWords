package com.example.cardwords.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF000000)
private val CardShadowColor = Color(0x1A000000)
private val BackCardColor = Color(0xFFE8E8E8)
private val SubtitleColor = Color(0xFF8A8A8A)
private val LoginTextColor = Color(0xFF8A8A8A)

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        FlashCardLogo()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "CardWords",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Учи слова легко и эффективно с\nпомощью карточек",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = SubtitleColor,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Black,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = "Начать",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "У меня уже есть аккаунт",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = LoginTextColor,
                modifier = Modifier
                    .clickable(onClick = onLoginClick)
                    .padding(8.dp),
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FlashCardLogo() {
    Box(
        modifier = Modifier.size(200.dp, 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Back card — rotated, gray
        Box(
            modifier = Modifier
                .size(130.dp, 160.dp)
                .rotate(8f)
                .offset(x = 20.dp, y = 6.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(BackCardColor)
        )

        // Front card — white with shadow, "Aa" inside
        Box(
            modifier = Modifier
                .size(130.dp, 160.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
            )
        }
    }
}
