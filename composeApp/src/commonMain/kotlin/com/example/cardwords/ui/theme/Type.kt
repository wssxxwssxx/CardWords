package com.example.cardwords.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cardwords.composeapp.generated.resources.Res
import cardwords.composeapp.generated.resources.inter_bold
import cardwords.composeapp.generated.resources.inter_medium
import cardwords.composeapp.generated.resources.inter_regular
import cardwords.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun interFontFamily() = FontFamily(
    Font(Res.font.inter_regular,  FontWeight.Normal),
    Font(Res.font.inter_medium,   FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold,     FontWeight.Bold),
)
