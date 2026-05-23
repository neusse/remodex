package com.remodex.mobile.data

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import com.remodex.mobile.R
import com.remodex.mobile.core.model.AppLanguagePreference
import java.util.Locale

object LanguagePreferences {
    private const val PREFS_NAME = "remodex_ui"
    private val englishLocale: Locale = Locale.forLanguageTag("en")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): AppLanguagePreference {
        val raw = prefs(context).getString(AppLanguagePreference.storageKey, null)
        return AppLanguagePreference.fromStorage(raw)
    }

    fun write(
        context: Context,
        preference: AppLanguagePreference,
    ) {
        prefs(context).edit().putString(AppLanguagePreference.storageKey, preference.name).apply()
    }

    fun wrapContext(context: Context): Context {
        val preference = read(context)
        return wrapContext(context, preference)
    }

    fun wrapContext(
        context: Context,
        preference: AppLanguagePreference,
    ): Context {
        val locale =
            when (preference) {
                AppLanguagePreference.english -> englishLocale
                AppLanguagePreference.system -> currentSystemLocale(context)
            }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return ContextThemeWrapper(context, R.style.Theme_RemodexMobile).apply {
            applyOverrideConfiguration(config)
        }
    }

    fun currentSystemLocaleDisplayName(context: Context): String {
        val locale = currentSystemLocale(context)
        return locale.getDisplayName(locale).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    fun currentSystemLocaleTag(context: Context): String = currentSystemLocale(context).toLanguageTag()

    private fun currentSystemLocale(context: Context): Locale {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val systemLocales = context.getSystemService(LocaleManager::class.java)?.systemLocales
            if (systemLocales != null && systemLocales.size() > 0) {
                return systemLocales[0]
            }
        }

        val systemConfiguration = Resources.getSystem().configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            systemConfiguration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            systemConfiguration.locale
        } ?: Locale.getDefault()
    }
}
