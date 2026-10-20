package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.example.R
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Text in the app's font (Rubik SemiBold, as the currency names in the app's cards). Launchers inflate
 * widgets in a restricted context that ignores font resources, and Glance can only name system fonts,
 * so the text is drawn into a white picture and tinted like the texture (opaque tint, see [WidgetColors]).
 * It shrinks a little before it is cut short with "…", like the names in the app.
 */
@Composable
internal fun AppFontText(
    text: String,
    color: ColorProvider,
    sizeSp: Float,
    widthDp: Float,
    maxLines: Int,
    minSp: Float = sizeSp,
) {
    val context = LocalContext.current
    val label = remember(text, sizeSp, widthDp, maxLines, minSp) {
        renderAppFontText(context, text, sizeSp, widthDp, maxLines, minSp)
    }
    Image(
        provider = ImageProvider(label.bitmap),
        contentDescription = text,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color),
        modifier = GlanceModifier.size(label.widthDp.dp, label.heightDp.dp)
    )
}

internal class TextPicture(val bitmap: Bitmap, val widthDp: Float, val heightDp: Float)

/** ResourcesCompat keeps loaded fonts, so this is cheap after the first widget update. */
private fun appTypeface(context: Context): Typeface =
    ResourcesCompat.getFont(context, R.font.rubik_semibold) ?: Typeface.DEFAULT_BOLD

internal fun renderAppFontText(
    context: Context,
    text: String,
    sizeSp: Float,
    widthDp: Float,
    maxLines: Int,
    minSp: Float,
): TextPicture {
    val metrics = context.resources.displayMetrics
    val widthPx = (widthDp * metrics.density).roundToInt().coerceAtLeast(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = appTypeface(context)
    }
    fun layout(sp: Float): StaticLayout {
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }
    fun StaticLayout.cut() = lineCount > 0 && getEllipsisCount(lineCount - 1) > 0

    var sp = sizeSp
    var built = layout(sp)
    while (built.cut() && sp - 1f >= minSp) {
        sp -= 1f
        built = layout(sp)
    }
    val heightPx = ceil(built.height.toFloat()).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    built.draw(Canvas(bitmap))
    return TextPicture(bitmap, widthPx / metrics.density, heightPx / metrics.density)
}
