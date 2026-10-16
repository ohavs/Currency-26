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

    /** Shown in Settings, each in its own language (strings come from res/values-<code>). */
    val OPTIONS = listOf(
        HEBREW to "עברית",
        ENGLISH to "English",
        "ar" to "العربية",
        "ru" to "Русский",
        SPANISH to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "pt" to "Português",
        "it" to "Italiano",
        "tr" to "Türkçe",
        "zh" to "中文",
        "ja" to "日本語",
        "hi" to "हिन्दी",
    )

    /** Follow the device: its language when the app has it, English (res/values) otherwise. */
    const val DEFAULT = SYSTEM

    private const val PREFS = "currency_prefs"
    const val KEY = "app_language"
    /** Set on a fresh install: offer the language of the user's country once, on the first launch. */
    const val SUGGEST_KEY = "language_suggestion_pending"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Runs before anything reads the language. A fresh install follows the device language; an install from
     * before this setting existed keeps Hebrew, which was the app's only language back then.
     */
    fun initialize(context: Context) {
        val prefs = prefs(context)
        if (prefs.contains(KEY)) return
        val existingInstall = prefs.all.isNotEmpty()
        prefs.edit()
            .putString(KEY, if (existingInstall) HEBREW else SYSTEM)
            .putBoolean(SUGGEST_KEY, !existingInstall)
            .commit()
    }

    fun stored(context: Context): String = prefs(context).getString(KEY, DEFAULT) ?: DEFAULT

    /** The language the app actually shows for the [language] setting. */
    fun effective(language: String): String {
        val code = if (language == SYSTEM) CountryLanguage.normalize(systemLocale().language) else language
        return if (OPTIONS.any { it.first == code }) code else ENGLISH
    }

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
