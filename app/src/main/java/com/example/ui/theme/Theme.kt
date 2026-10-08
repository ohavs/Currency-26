package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val StandardLight = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
private val StandardDark = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF006C52), onPrimary = Color.White, primaryContainer = Color(0xFF80F8D2), onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF006782), onSecondary = Color.White, secondaryContainer = Color(0xFFBBE9FF), onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFF4C626B), onTertiary = Color.White, tertiaryContainer = Color(0xFFCFE6F1), onTertiaryContainer = Color(0xFF071E26)
)
private val OceanDark = darkColorScheme(
    primary = Color(0xFF63DBB6), onPrimary = Color(0xFF003829), primaryContainer = Color(0xFF00513D), onPrimaryContainer = Color(0xFF80F8D2),
    secondary = Color(0xFF61D4FF), onSecondary = Color(0xFF003544), secondaryContainer = Color(0xFF004D62), onSecondaryContainer = Color(0xFFBBE9FF),
    tertiary = Color(0xFFB3CAD5), onTertiary = Color(0xFF1E333C), tertiaryContainer = Color(0xFF354A53), onTertiaryContainer = Color(0xFFCFE6F1)
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF376A20), onPrimary = Color.White, primaryContainer = Color(0xFFB7F397), onPrimaryContainer = Color(0xFF082100),
    secondary = Color(0xFF55624C), onSecondary = Color.White, secondaryContainer = Color(0xFFD8E7CB), onSecondaryContainer = Color(0xFF131F0E),
    tertiary = Color(0xFF386665), onTertiary = Color.White, tertiaryContainer = Color(0xFFBCEBEA), onTertiaryContainer = Color(0xFF00201F)
)
private val ForestDark = darkColorScheme(
    primary = Color(0xFF9CD67E), onPrimary = Color(0xFF113800), primaryContainer = Color(0xFF205106), onPrimaryContainer = Color(0xFFB7F397),
    secondary = Color(0xFFBCCBB0), onSecondary = Color(0xFF273420), secondaryContainer = Color(0xFF3E4A35), onSecondaryContainer = Color(0xFFD8E7CB),
    tertiary = Color(0xFFA0CFCF), onTertiary = Color(0xFF003736), tertiaryContainer = Color(0xFF1E4E4D), onTertiaryContainer = Color(0xFFBCEBEA)
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFF9E4200), onPrimary = Color.White, primaryContainer = Color(0xFFFFDBCB), onPrimaryContainer = Color(0xFF341100),
    secondary = Color(0xFF765848), onSecondary = Color.White, secondaryContainer = Color(0xFFFFDBCB), onSecondaryContainer = Color(0xFF2C160A),
    tertiary = Color(0xFF636032), onTertiary = Color.White, tertiaryContainer = Color(0xFFEAE5AB), onTertiaryContainer = Color(0xFF1E1D00)
)
private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB68F), onPrimary = Color(0xFF552000), primaryContainer = Color(0xFF793100), onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFE6BEA8), onSecondary = Color(0xFF432B1E), secondaryContainer = Color(0xFF5C4032), onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFCEC991), onTertiary = Color(0xFF343207), tertiaryContainer = Color(0xFF4B481D), onTertiaryContainer = Color(0xFFEAE5AB)
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFF904A4C), onPrimary = Color.White, primaryContainer = Color(0xFFFFDAD9), onPrimaryContainer = Color(0xFF3B080E),
    secondary = Color(0xFF775656), onSecondary = Color.White, secondaryContainer = Color(0xFFFFDAD9), onSecondaryContainer = Color(0xFF2C1516),
    tertiary = Color(0xFF755A2F), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFDDAF), onTertiaryContainer = Color(0xFF281800)
)
private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB3B4), onPrimary = Color(0xFF561D21), primaryContainer = Color(0xFF733336), onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDBE), onSecondary = Color(0xFF44292A), secondaryContainer = Color(0xFF5D3F40), onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFE5C18D), onTertiary = Color(0xFF422C05), tertiaryContainer = Color(0xFF5B421A), onTertiaryContainer = Color(0xFFFFDDAF)
)

private val LavenderLight = lightColorScheme(
    primary = Color(0xFF4758A9), onPrimary = Color.White, primaryContainer = Color(0xFFDDE1FF), onPrimaryContainer = Color(0xFF001257),
    secondary = Color(0xFF5B5D72), onSecondary = Color.White, secondaryContainer = Color(0xFFE0E1F9), onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD8EF), onTertiaryContainer = Color(0xFF2D1128)
)
private val LavenderDark = darkColorScheme(
    primary = Color(0xFFB9C3FF), onPrimary = Color(0xFF132778), primaryContainer = Color(0xFF2E4090), onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD), onSecondary = Color(0xFF2D2F42), secondaryContainer = Color(0xFF434559), onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE6B8D7), onTertiary = Color(0xFF45263E), tertiaryContainer = Color(0xFF5D3C55), onTertiaryContainer = Color(0xFFFFD8EF)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    colorThemeStr: String = "standard",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (colorThemeStr) {
        "ocean" -> if (darkTheme) OceanDark else OceanLight
        "forest" -> if (darkTheme) ForestDark else ForestLight
        "sunset" -> if (darkTheme) SunsetDark else SunsetLight
        "rose" -> if (darkTheme) RoseDark else RoseLight
        "lavender" -> if (darkTheme) LavenderDark else LavenderLight
        else -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) StandardDark else StandardLight
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
