package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.R
import java.io.File

/** Desktop stand-in for Android string resources: reads the app's strings.xml for [HarnessStrings.language]. */
object HarnessStrings {
    var language: String = "iw"
    private val cache = mutableMapOf<String, Map<String, String>>()

    private fun load(lang: String): Map<String, String> = cache.getOrPut(lang) {
        val folder = if (lang == "en") "values" else "values-$lang"
        val xml = File("../../app/src/main/res/$folder/strings.xml").readText()
        Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).associate { m ->
            m.groupValues[1] to m.groupValues[2]
                .replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        }
    }

    fun get(id: Int): String {
        val name = R.string.names[id - 0x7f100000]
        return load(language)[name] ?: error("missing string $name for $language")
    }
}

@Composable
@ReadOnlyComposable
fun stringResource(id: Int): String = HarnessStrings.get(id)

@Composable
@ReadOnlyComposable
fun stringResource(id: Int, vararg formatArgs: Any): String = String.format(HarnessStrings.get(id), *formatArgs)
