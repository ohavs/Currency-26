package render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppPalette
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo

/** Desktop approximation of LargeCurrencyWidget/SmallCurrencyWidget (same tokens, sizes and structure). */
@Composable
fun LargeWidgetMock(p: AppPalette, amount: String, src: String, tgt: String, rate: Double, updated: String, extra: String? = null, extraRate: Double = 1.0) {
    // Mirrors LargeCurrencyWidget on an LTR launcher: amounts left, pills and swap on the right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(p.background).padding(12.dp)) {
            Card(p, src, formatAmountInput(amount), p.card, p.cardSoft, 24.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(p.card).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("\u200E1 $src = ${formatRate(rate)} $tgt", style = TextStyle(color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    Text("עודכן $updated", style = TextStyle(color = p.inkMuted, fontSize = 10.sp))
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(p.accent), contentAlignment = Alignment.Center) {
                    Text("⇅", style = TextStyle(color = p.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.height(6.dp))
            Card(p, tgt, formatAmount((amount.toDoubleOrNull() ?: 0.0) * rate), p.highlight, p.highlightSoft, 24.sp)
            if (extra != null) {
                Spacer(Modifier.height(6.dp))
                Card(p, extra, formatAmount((amount.toDoubleOrNull() ?: 0.0) * extraRate), p.highlightSoft, p.highlight, 24.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf("C", "0", "⌫")).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            val bg = when (key) { "C" -> p.accent; "⌫" -> p.highlight; else -> p.cardSoft }
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                                Text(key, style = TextStyle(color = if (key == "C") p.onAccent else p.ink, fontSize = 24.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(p: AppPalette, code: String, amount: String, bg: Color, pill: Color, amountSize: TextUnit) {
    val info = getCurrencyInfo(code)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(amount, style = TextStyle(color = p.ink, fontSize = amountSize, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(pill).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(info.flag, fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(code, style = TextStyle(color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun SmallWidgetMock(p: AppPalette, amount: String, src: String, tgt: String, rate: Double) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(p.background).padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SmallLine(p, src, formatAmountInput(amount), p.card, p.cardSoft)
            Spacer(Modifier.height(6.dp))
            SmallLine(p, tgt, formatAmount((amount.toDoubleOrNull() ?: 0.0) * rate), p.highlight, p.highlightSoft)
        }
    }
}

@Composable
private fun SmallLine(p: AppPalette, code: String, amount: String, bg: Color, pill: Color) {
    val info = getCurrencyInfo(code)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(pill).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(info.flag, fontSize = 15.sp)
            Spacer(Modifier.width(4.dp))
            Text(code, style = TextStyle(color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.weight(1f))
        Text(amount, style = TextStyle(color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 6.dp))
    }
}
