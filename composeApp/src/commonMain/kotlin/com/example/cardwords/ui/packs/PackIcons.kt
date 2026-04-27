package com.example.cardwords.ui.packs

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

// ─────────────────────────────────────────────
//  SVG path data for pack icons (24×24)
// ─────────────────────────────────────────────
private object PackIconPaths {
    // Sprout / plant (basic_a1)
    const val SPROUT =
        "M12 22V10 " +
        "M12 10C12 10 7 8 7 4C7 4 12 4 13 8 " +
        "M12 10C12 10 17 7 17 3C17 3 12 3 12 7"
    // Flame (popular_b1)
    const val FLAME =
        "M12 2.5C12 2.5 7 7 7 12C7 15.3 9.3 17.5 12 17.5C14.7 17.5 17 15.3 17 12C17 10 16 9 15 9C15 9 16 7 13 4C13 4 13.5 6 11.5 7C10 8 10 10 12 11C10 11 9 10 9 8C9 7 10 6 10 5C10 4 10 3 12 2.5Z"
    // Diamond (advanced_b2)
    const val DIAMOND = "M12 2L22 9L12 22L2 9L12 2Z M2 9H22 M12 2L8 9L12 22 M12 2L16 9L12 22"
    // Laptop (it_tech)
    const val LAPTOP_SCREEN = "M5 5H19V15H5Z"
    const val LAPTOP_BASE = "M2 18H22"
    // Plane (travel)
    const val PLANE = "M2 16L22 8L14 20L12 14L4 12L2 16Z M14 10L10 14"
    // Briefcase (business)
    const val BRIEFCASE_BOX = "M4 8H20V20H4Z"
    const val BRIEFCASE_HANDLE = "M9 8V6C9 5 10 4 11 4H13C14 4 15 5 15 6V8"
    // House / home (everyday_life)
    const val HOUSE = "M3 10L12 3L21 10V20H15V14H9V20H3V10Z"
    // Speech bubble (phrases_idioms)
    const val BUBBLE = "M4 4H20V16H12L8 20V16H4V4Z"
    const val BUBBLE_DOTS = "M8 10H8.01 M12 10H12.01 M16 10H16.01"
    // Letter "V" with three forms — arrows showing transformation (irregular verbs)
    const val VERB_ARROW_1 = "M3 7H15"
    const val VERB_ARROW_1_HEAD = "M13 5L15 7L13 9"
    const val VERB_ARROW_2 = "M3 12H19"
    const val VERB_ARROW_2_HEAD = "M17 10L19 12L17 14"
    const val VERB_ARROW_3 = "M3 17H21"
    const val VERB_ARROW_3_HEAD = "M19 15L21 17L19 19"
    // Search magnifier (search card)
    const val SEARCH_CIRCLE = "M11 11M11 11C13.76 11 16 8.76 16 6C16 3.24 13.76 1 11 1C8.24 1 6 3.24 6 6C6 8.76 8.24 11 11 11Z"
    const val SEARCH_HANDLE = "M14.5 9.5L20 15"
    // Generic book
    const val BOOK = "M6 4H18C19 4 20 5 20 6V20H6C5 20 4 19 4 18V6C4 5 5 4 6 4Z"
    const val BOOK_LINES = "M8 8H16 M8 12H14"
    // Chevron
    const val CHEVRON_L = "M15 5L8 12L15 19"
    const val CHEVRON_R = "M9 5L16 12L9 19"
    // Check + Plus
    const val CHECK = "M4 12L9 17L20 6"
    const val PLUS = "M12 5V19 M5 12H19"
}

@Composable
private fun DrawSvg(
    paths: List<String>,
    color: Color,
    modifier: Modifier,
    strokeWidth: Float = 1.8f,
    filled: Boolean = false,
) {
    val parsed = remember(paths) { paths.map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier) {
        val s = size.width / 24f
        scale(s, s, Offset.Zero) {
            parsed.forEach { p ->
                if (filled) drawPath(p, color)
                else drawPath(
                    p, color,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
internal fun PackIconFor(packId: String, color: Color, modifier: Modifier) {
    when (packId) {
        "basic_a1" -> DrawSvg(listOf(PackIconPaths.SPROUT), color, modifier, strokeWidth = 2f)
        "popular_b1" -> DrawSvg(listOf(PackIconPaths.FLAME), color, modifier, filled = true)
        "advanced_b2" -> DrawSvg(listOf(PackIconPaths.DIAMOND), color, modifier)
        "it_tech" -> DrawSvg(
            listOf(PackIconPaths.LAPTOP_SCREEN, PackIconPaths.LAPTOP_BASE),
            color, modifier, strokeWidth = 1.8f,
        )
        "travel" -> DrawSvg(listOf(PackIconPaths.PLANE), color, modifier)
        "business" -> DrawSvg(
            listOf(PackIconPaths.BRIEFCASE_BOX, PackIconPaths.BRIEFCASE_HANDLE),
            color, modifier,
        )
        "everyday_life" -> DrawSvg(listOf(PackIconPaths.HOUSE), color, modifier)
        "phrases_idioms" -> DrawSvg(
            listOf(PackIconPaths.BUBBLE, PackIconPaths.BUBBLE_DOTS),
            color, modifier, strokeWidth = 1.6f,
        )
        "irregular_basic", "irregular_extended" -> DrawSvg(
            listOf(
                PackIconPaths.VERB_ARROW_1, PackIconPaths.VERB_ARROW_1_HEAD,
                PackIconPaths.VERB_ARROW_2, PackIconPaths.VERB_ARROW_2_HEAD,
                PackIconPaths.VERB_ARROW_3, PackIconPaths.VERB_ARROW_3_HEAD,
            ),
            color, modifier, strokeWidth = 1.8f,
        )
        else -> DrawSvg(listOf(PackIconPaths.BOOK, PackIconPaths.BOOK_LINES), color, modifier)
    }
}

@Composable
internal fun SearchIcon(color: Color, modifier: Modifier) = DrawSvg(
    listOf(PackIconPaths.SEARCH_CIRCLE, PackIconPaths.SEARCH_HANDLE),
    color, modifier, strokeWidth = 2f,
)

@Composable
internal fun ChevronLeftIcon(color: Color, modifier: Modifier) = DrawSvg(
    listOf(PackIconPaths.CHEVRON_L), color, modifier, strokeWidth = 2f,
)

@Composable
internal fun ChevronRightIcon(color: Color, modifier: Modifier) = DrawSvg(
    listOf(PackIconPaths.CHEVRON_R), color, modifier, strokeWidth = 2f,
)

@Composable
internal fun CheckIcon(color: Color, modifier: Modifier) = DrawSvg(
    listOf(PackIconPaths.CHECK), color, modifier, strokeWidth = 2.2f,
)

@Composable
internal fun PlusIcon(color: Color, modifier: Modifier) = DrawSvg(
    listOf(PackIconPaths.PLUS), color, modifier, strokeWidth = 2.2f,
)
