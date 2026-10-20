package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.HarnessStrings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalPalette

/** Desktop stand-in for the app's SelectionMenu.kt: the phone's language and maker are set by the render. */
data class SelectionMenuInfo(
    val menuLabel: String,
    val appLabel: String,
    val samsung: Boolean,
    val copyLabel: String,
    val shareLabel: String?,
    val selectAllLabel: String,
    val rtl: Boolean,
    val manageLabel: String,
    val doneLabel: String,
)

/** The system's Copy / Share / Select all, as Samsung words them (the app has no strings for these). */
private val systemLabels = mapOf(
    "en" to listOf("Copy", "Share", "Select all"),
    "iw" to listOf("העתק", "שתף", "בחר הכל"),
    "ar" to listOf("نسخ", "مشاركة", "تحديد الكل"),
    "ru" to listOf("Копировать", "Поделиться", "Выбрать все"),
)

object SelectionMenuHarness {
    /** The phone's language (the selection menu's), which can differ from the app's. */
    var deviceLanguage: String = "en"
    var samsung: Boolean = true
    /** Draws the price as if it had just been long-pressed. */
    var selected: Boolean = false
    /** Where the price was last drawn (window coordinates), for the gesture checks. */
    var priceBounds: Rect? = null
    var longPresses: Int = 0
}

@Composable
fun rememberSelectionMenuInfo(): SelectionMenuInfo {
    val device = SelectionMenuHarness.deviceLanguage
    val (copy, share, selectAll) = systemLabels[device] ?: systemLabels.getValue("en")
    return SelectionMenuInfo(
        menuLabel = HarnessStrings.get(device, "quick_convert_label"),
        appLabel = HarnessStrings.get(device, "app_name"),
        samsung = SelectionMenuHarness.samsung,
        copyLabel = copy,
        shareLabel = share,
        selectAllLabel = selectAll,
        rtl = device == "iw" || device == "ar",
        manageLabel = HarnessStrings.get(device, "samsung_manage_apps"),
        doneLabel = HarnessStrings.get(device, "samsung_done")
    )
}

@Composable
fun SelectablePrice(text: String, modifier: Modifier = Modifier, onLongPress: () -> Unit = {}) {
    val palette = LocalPalette.current
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier
            .onGloballyPositioned { SelectionMenuHarness.priceBounds = it.boundsInWindow() }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    SelectionMenuHarness.longPresses++
                    currentOnLongPress()
                })
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = palette.ink,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr),
            modifier = if (SelectionMenuHarness.selected) {
                Modifier.clip(RoundedCornerShape(2.dp)).background(palette.accent.copy(alpha = 0.35f))
            } else {
                Modifier
            }
        )
    }
}
