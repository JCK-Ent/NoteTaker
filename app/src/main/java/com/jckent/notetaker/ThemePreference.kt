package com.jckent.notetaker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreference {
    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "night_mode"

    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getSavedMode(context))
    }

    fun cycle(context: Context) {
        val next = when (getSavedMode(context)) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_YES  // FOLLOW_SYSTEM → dark
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MODE, next).apply()
        AppCompatDelegate.setDefaultNightMode(next)
    }

    fun getSavedMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}