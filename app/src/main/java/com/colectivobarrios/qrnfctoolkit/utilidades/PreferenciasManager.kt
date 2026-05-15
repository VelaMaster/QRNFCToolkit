package com.colectivobarrios.qrnfctoolkit.utilidades

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class PreferenciasManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            return try { ThemeMode.valueOf(name!!) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var language: String
        get() = prefs.getString("app_language", "es") ?: "es"
        set(value) = prefs.edit().putString("app_language", value).apply()
}
