package com.example.ui

import androidx.compose.foundation.background
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
data class SelectionMenuInfo(val menuLabel: String, val appLabel: String, val samsung: Boolean)

object SelectionMenuHarness {
    /** The phone's language (the selection menu's), which can differ from the app's. */
    var deviceLanguage: String = "en"
    var samsung: Boolean = true
    /** Draws the price as if it had just been long-pressed. */
    var selected: Boolean = false
}

@Composable
fun rememberSelectionMenuInfo(): SelectionMenuInfo = SelectionMenuInfo(
    menuLabel = HarnessStrings.get(SelectionMenuHarness.deviceLanguage, "quick_convert_label"),
    appLabel = HarnessStrings.get(SelectionMenuHarness.deviceLanguage, "app_name"),
    samsung = SelectionMenuHarness.samsung
)

@Composable
fun SelectablePrice(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
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
