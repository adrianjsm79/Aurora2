package com.tecsup.aurora.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.databinding.ActivityBatteryOptimizationBinding

class BatteryOptimizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryOptimizationBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "OptimizationPrefs"
        const val KEY_OPTIMIZATION_SEEN = "optimization_seen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryOptimizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.btnOpenSettings.setOnClickListener {
            prefs.edit().putBoolean(KEY_OPTIMIZATION_SEEN, true).apply()

            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)

            finish()
        }

        binding.btnContinueAnyway.setOnClickListener {
            prefs.edit().putBoolean(KEY_OPTIMIZATION_SEEN, true).apply()
            finish()
        }
    }
}
