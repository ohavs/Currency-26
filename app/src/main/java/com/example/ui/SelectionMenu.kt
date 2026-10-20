package com.example.ui

import android.content.Intent
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.LocalPalette

/**
 * The phone's text-selection menu as other apps show it: in the phone's language, not the app's (the desktop
 * preview has a stand-in for this file).
 */
data class SelectionMenuInfo(
    /** Our item in the menu, "Convert currency". */
    val menuLabel: String,
    /** The app's name as the phone lists it (Samsung's "Manage apps" list). */
    val appLabel: String,
    /** Samsung hides other apps' items until they are turned on under ⋮ → "Manage apps". */
    val samsung: Boolean,
)

@Composable
fun rememberSelectionMenuInfo(): SelectionMenuInfo {
    val context = LocalContext.current
    return remember(context) {
        // Labels loaded through the package manager use the phone's language, like the menus that show them.
        val pm = context.packageManager
        val ours = pm.queryIntentActivities(Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain"), 0)
            .firstOrNull { it.activityInfo.packageName == context.packageName }
        SelectionMenuInfo(
            menuLabel = ours?.loadLabel(pm)?.toString() ?: context.getString(R.string.quick_convert_label),
            appLabel = context.applicationInfo.loadLabel(pm).toString(),
            samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        )
    }
}

/**
 * A price to long-press: it opens the phone's own selection menu, with "Convert currency" (and Samsung's
 * "Manage apps"). Compose text has no such menu, so this is a classic TextView.
 */
@Composable
fun SelectablePrice(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val ink = palette.ink.toArgb()
    val selection = palette.accent.copy(alpha = 0.35f).toArgb()
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                // Apps' items (PROCESS_TEXT) only join the menu of a TextView with an id.
                id = View.generateViewId()
                setTextIsSelectable(true)
                gravity = Gravity.CENTER
                textDirection = View.TEXT_DIRECTION_LTR
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                typeface = ResourcesCompat.getFont(context, R.font.rubik_bold)
                setPadding(0, 12, 0, 12)
            }
        },
        update = { view ->
            // A long press selects the word under the finger, which would leave "€" out of "€120"; a link span is
            // selected whole instead. This one draws nothing and opens nothing.
            view.text = SpannableString(text).apply {
                setSpan(WholePrice(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            view.setTextColor(ink)
            view.highlightColor = selection
        },
        modifier = modifier
    )
}

private class WholePrice : URLSpan("") {
    override fun updateDrawState(ds: TextPaint) {}
    override fun onClick(widget: View) {}
}
