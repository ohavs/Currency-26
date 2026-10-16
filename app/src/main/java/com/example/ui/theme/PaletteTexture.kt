package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quiet texture behind the screens, in the colour set's accent: a banknote's guilloche (classic), seigaiha
 * waves (sea), wind ripples (sand), sun rays (sunset), constellations (night) and currency symbols (gold).
 * The tiles come from tools/textures/make_textures.mjs, which bakes each one's strength into its alpha; the
 * widgets draw the same ones. It sits under everything, so it shows only between the cards.
 */
@Composable
fun Modifier.paletteTexture(themeKey: String, palette: AppPalette): Modifier {
    val option = Palettes.option(themeKey)
    val filter = ColorFilter.tint(palette.textureInk)
    val tile = option.textureTile
    if (tile == null) {
        return drawWithCache {
            val rays = sunRays(size)
            onDrawBehind { drawPath(rays, palette.textureInk, alpha = SunRays.ALPHA) }
        }
    }
    val bitmap = ImageBitmap.imageResource(tile)
    return drawWithCache {
        val brush = ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind { drawRect(brush, colorFilter = filter) }
    }
}

/** The numbers of RAYS in tools/textures/make_textures.mjs, which draws the widgets' copy. */
private object SunRays {
    const val COUNT = 18
    const val CENTER_Y = 1.04f
    const val ALPHA = 0.07f
}

/**
 * Every other wedge of [SunRays.COUNT] over the upper half, from a sun just below the bottom edge; laid out
 * in a 100 x 100 box stretched over [size], like the widgets' vector drawable.
 */
private fun sunRays(size: Size): Path {
    val sx = size.width / 100f
    val sy = size.height / 100f
    val cx = 50f
    val cy = 100f * SunRays.CENTER_Y
    val reach = 160f
    val step = PI / SunRays.COUNT
    return Path().apply {
        for (i in 0 until SunRays.COUNT step 2) {
            val from = PI + i * step
            val to = from + step
            moveTo(cx * sx, cy * sy)
            lineTo((cx + reach * cos(from).toFloat()) * sx, (cy + reach * sin(from).toFloat()) * sy)
            lineTo((cx + reach * cos(to).toFloat()) * sx, (cy + reach * sin(to).toFloat()) * sy)
            close()
        }
    }
}
