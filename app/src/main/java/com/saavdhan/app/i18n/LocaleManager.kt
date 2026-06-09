package com.saavdhan.app.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Remembers the user's chosen language and applies it to the app.
 *
 * We store the choice in SharedPreferences (a tiny on-device key/value file) because it can be
 * read INSTANTLY and synchronously — and we must know the language before the very first screen
 * is built, inside [com.saavdhan.app.MainActivity.attachBaseContext].
 */
object LocaleManager {
    private const val PREFS = "saavdhan_prefs"
    private const val KEY_LANG = "language"

    const val ENGLISH = "en"
    const val HINDI = "hi"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The saved language ("en"/"hi"), or null if the user has not chosen yet. */
    fun getLanguage(context: Context): String? = prefs(context).getString(KEY_LANG, null)

    /** True once the user has picked a language on the onboarding screen. */
    fun hasChosen(context: Context): Boolean = getLanguage(context) != null

    /** Save the user's language choice. */
    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANG, language).apply()
    }

    /**
     * Returns a copy of [context] whose resources use the chosen language. If no choice has been
     * made yet, the phone's own default language is used.
     */
    fun wrap(context: Context): Context {
        val language = getLanguage(context) ?: return context
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
