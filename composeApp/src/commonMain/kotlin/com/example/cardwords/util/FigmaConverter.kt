package com.example.cardwords.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Converts Figma design values to Compose units.
 *
 * Figma uses a 1× canvas (e.g. iPhone 390×844) where 1 Figma px = 1 Compose dp.
 * Font sizes in Figma are in points, which also map directly to Compose sp.
 *
 * Usage:
 *   FigmaConverter.px(16)   → 16.dp
 *   FigmaConverter.sp(14)   → 14.sp
 */
object FigmaConverter {
    /** Converts a Figma pixel dimension to Compose Dp (1 Figma px = 1 dp at 1× scale). */
    fun px(value: Number): Dp = value.toFloat().dp

    /** Converts a Figma font-size (pt) to Compose TextUnit (1 Figma pt = 1 sp). */
    fun sp(value: Number): TextUnit = value.toFloat().sp
}
