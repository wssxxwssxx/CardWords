package com.example.cardwords.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a rounded card background + optional border in a single draw pass.
 * Replaces `.clip(shape) + .background(color) + .border(w, c, shape)` which installs
 * multiple draw/layer modifiers and allocates Stroke/Path on every frame.
 *
 * Pass [clipContent] = true to also clip children/ripple to the rounded shape.
 *
 * Use this for every card-shaped item in scrolling lists.
 */
fun Modifier.cardBg(
    color: Color,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp,
    cornerRadius: Dp = 14.dp,
    clipContent: Boolean = false,
): Modifier = this
    .drawWithCache {
    val radiusPx = cornerRadius.toPx()
    val radius = CornerRadius(radiusPx, radiusPx)
    val strokePx = borderWidth.toPx()
    val half = strokePx / 2f
    val hasBorder = borderWidth > 0.dp && borderColor.alpha > 0f
    val stroke = if (hasBorder) Stroke(width = strokePx) else null
    val strokeSize = if (hasBorder) Size(size.width - strokePx, size.height - strokePx) else Size.Zero
    val strokeOffset = if (hasBorder) Offset(half, half) else Offset.Zero

    onDrawBehind {
        drawRoundRect(color = color, cornerRadius = radius)
        if (stroke != null) {
            drawRoundRect(
                color = borderColor,
                topLeft = strokeOffset,
                size = strokeSize,
                cornerRadius = radius,
                style = stroke,
            )
        }
    }
}
    .then(if (clipContent) Modifier.clip(RoundedCornerShape(cornerRadius)) else Modifier)
