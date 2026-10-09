package com.example.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.view.View
import java.util.Locale

/** The app's own language choice (Settings → Language), independent of the device language. */
object AppLanguage {
    const val SYSTEM = "system"
    const val HEBREW = "he"
    const val ENGLISH = "en"
    const val SPANISH = "es"

    /** Shown in Settings, each in its own language. */
    val OPTIONS = listOf(HEBREW to "עברית", ENGLISH to "English", SPANISH to "Español")

    /** The app was Hebrew-only, so Hebrew stays the default until the user picks something else. */
    const val DEFAULT = HEBREW

    private const val PREFS = "currency_prefs"
    const val KEY = "app_language"

    fun stored(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, DEFAULT) ?: DEFAULT

    fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]

    fun locale(language: String): Locale = if (language == SYSTEM) systemLocale() else Locale.forLanguageTag(language)

    fun isRtl(language: String): Boolean =
        TextUtils.getLayoutDirectionFromLocale(locale(language)) == View.LAYOUT_DIRECTION_RTL

    /** A context whose strings and layout direction follow [language]. */
    fun wrap(context: Context, language: String = stored(context)): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale(language))
        return context.createConfigurationContext(config)
    }

    /** Layout direction of the launcher hosting the widgets (it follows the device, not the app). */
    fun isLauncherRtl(): Boolean = Resources.getSystem().configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}
