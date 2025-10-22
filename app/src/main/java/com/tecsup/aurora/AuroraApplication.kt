package com.tecsup.aurora

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class AuroraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Leer la preferencia del tema guardada al iniciar la app
        val sharedPrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false) // false por defecto

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
    