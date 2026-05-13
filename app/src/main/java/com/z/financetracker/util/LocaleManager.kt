package com.z.financetracker.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {

    private const val PREF_NAME = "locale_pref"
    private const val KEY_LANGUAGE = "language"

    const val LANG_ENGLISH = "en"
    const val LANG_CHINESE = "zh"

    fun setLocale(context: Context, language: String) {
        // Save preference
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()

        // Apply via AppCompatDelegate — works on all Android versions
        val localeList = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLanguage(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun applyOnLaunch(context: Context) {
        val language = getLanguage(context)
        val localeList = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}