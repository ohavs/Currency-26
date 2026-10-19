package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File

/** Desktop stand-in for Android drawable resources: loads the PNG from res/drawable-nodpi. */
fun painterResource(id: Int): Painter {
    val path = com.example.R.drawable.files[id] ?: error("unknown drawable $id")
    return BitmapPainter(File(path).inputStream().use { loadImageBitmap(it) })
}

/** Like Android's decodeResource: an xxhdpi PNG is scaled to the screen density. */
@Composable
fun ImageBitmap.Companion.imageResource(id: Int): ImageBitmap {
    val density = LocalDensity.current.density
    return remember(id, density) {
        val path = com.example.R.drawable.files[id] ?: error("unknown drawable $id")
        val image = Image.makeFromEncoded(File(path).readBytes())
        val scale = if (path.contains("xxhdpi")) density / 3f else 1f
        val w = Math.round(image.width * scale).coerceAtLeast(1)
        val h = Math.round(image.height * scale).coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(image, Rect.makeWH(image.width.toFloat(), image.height.toFloat()), Rect.makeWH(w.toFloat(), h.toFloat()), SamplingMode.LINEAR, Paint(), true)
        surface.makeImageSnapshot().toComposeImageBitmap()
    }
}
