package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * A quiet texture behind the screens, in the colour set's accent and matching its icon: banknote guilloche
 * (classic), waves (sea), dune contour lines (sand), a glow and horizon lines (sunset) and stars (night).
 * It sits under everything, so it shows only between the cards; fainter in dark mode.
 */
fun Modifier.paletteTexture(themeKey: String, palette: AppPalette): Modifier = drawWithCache {
    val ink = palette.accent
    val dim = if (palette.isDark) 0.6f else 1f
    val dp = density
    when (Palettes.normalize(themeKey)) {
        "ocean" -> {
            val waves = Path()
            var y = 11f * dp
            while (y < size.height + 22f * dp) {
                waves.moveTo(0f, y)
                var x = 0f
                var up = true
                while (x < size.width) {
                    waves.quadraticTo(x + 6f * dp, y + (if (up) -7f else 7f) * dp, x + 12f * dp, y)
                    x += 12f * dp
                    up = !up
                }
                y += 22f * dp
            }
            onDrawBehind {
                drawPath(waves, ink.copy(alpha = 0.14f * dim), style = Stroke(width = 1.6f * dp, cap = StrokeCap.Round))
            }
        }
        "sunset" -> {
            val lines = Path()
            val random = SeededRandom(11)
            var y = 10f * dp
            while (y < size.height + 30f * dp) {
                val amplitude = (3f + random.next() * 6f) * dp
                val length = (120f + random.next() * 110f) * dp
                val phase = random.next() * 2f * PI.toFloat()
                addWave(lines, y, amplitude, length, phase, size.width, dp)
                y += (18f + random.next() * 8f) * dp
            }
            onDrawBehind {
                drawPath(lines, ink.copy(alpha = 0.2f * dim), style = Stroke(width = 1.1f * dp))
            }
        }
        "rose" -> {
            val glow = Brush.radialGradient(
                colors = listOf(ink.copy(alpha = 0.3f * dim), Color.Transparent),
                center = Offset(size.width / 2f, -0.08f * size.height),
                radius = size.maxDimension * 0.62f
            )
            onDrawBehind {
                drawRect(glow)
                // Horizon lines across the lowest third, thickening downwards.
                val top = size.height * 0.7f
                var y = top
                var line = 0
                while (y < size.height) {
                    drawRect(ink.copy(alpha = 0.08f * dim), topLeft = Offset(0f, y), size = Size(size.width, (2.5f + line * 0.5f) * dp))
                    y += 25f * dp
                    line++
                }
            }
        }
        "lavender" -> {
            val tile = 140f * dp
            val random = SeededRandom(5)
            val stars = List(14) { Triple(random.next() * tile, random.next() * tile, (0.8f + random.next() * 0.8f) * dp) }
            val sparkles = listOf(Offset(100f * dp, 30f * dp), Offset(38f * dp, 104f * dp))
            onDrawBehind {
                val color = ink.copy(alpha = 0.4f * dim)
                var ty = 0f
                while (ty < size.height) {
                    var tx = 0f
                    while (tx < size.width) {
                        stars.forEach { (x, y, r) -> drawCircle(color, r, Offset(tx + x, ty + y)) }
                        sparkles.forEach { drawSparkle(color, Offset(tx + it.x, ty + it.y), 4f * dp) }
                        tx += tile
                    }
                    ty += tile
                }
            }
        }
        else -> {
            // Classic: the crossing waves of a banknote's guilloche.
            val lattice = Path()
            var y = 13f * dp
            while (y < size.height + 26f * dp) {
                addWave(lattice, y, 5f * dp, 72f * dp, 0f, size.width, dp)
                addWave(lattice, y, 5f * dp, 72f * dp, PI.toFloat(), size.width, dp)
                y += 26f * dp
            }
            onDrawBehind {
                drawPath(lattice, ink.copy(alpha = 0.1f * dim), style = Stroke(width = 1f * dp))
            }
        }
    }
}

private fun addWave(path: Path, y0: Float, amplitude: Float, length: Float, phase: Float, width: Float, dp: Float) {
    val step = 4f * dp
    var x = 0f
    path.moveTo(0f, y0 + amplitude * sin(phase))
    while (x <= width + step) {
        x += step
        path.lineTo(x, y0 + amplitude * sin(x / length * 2f * PI.toFloat() + phase))
    }
}

/** A four-pointed twinkle. */
private fun DrawScope.drawSparkle(color: Color, center: Offset, radius: Float) {
    val (x, y) = center
    val sparkle = Path().apply {
        moveTo(x, y - radius)
        quadraticTo(x, y, x + radius, y)
        quadraticTo(x, y, x, y + radius)
        quadraticTo(x, y, x - radius, y)
        quadraticTo(x, y, x, y - radius)
        close()
    }
    drawPath(sparkle, color)
}

/** Deterministic, so the stars and contour lines stay put between frames and launches. */
private class SeededRandom(seed: Int) {
    private var state = seed.toLong()
    fun next(): Float {
        state = (state * 16807) % 2147483647
        return (state - 1).toFloat() / 2147483646f
    }
}
