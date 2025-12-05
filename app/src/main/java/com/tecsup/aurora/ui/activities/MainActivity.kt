package com.tecsup.aurora.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityMainBinding
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
import com.tecsup.aurora.utils.DeviceHelper
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.SessionState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var authCheckStarted = false

    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val prefs = getSharedPreferences("AuroraSettings", Context.MODE_PRIVATE) // Uso hardcoded para evitar error si la clase no existe
            val optimizationSeen = prefs.getBoolean("optimizationSeen", false)

            // Si es un dispositivo problemático (Xiaomi, etc) y no ha visto la optimización
            if (!optimizationSeen && DeviceHelper.isProblematicManufacturer()) {

                startActivity(Intent(this, BatteryOptimizationActivity::class.java))
                return
            }
        } catch (e: Exception) {

        }

        startAuthCheck()
    }

    override fun onResume() {
        super.onResume()
        if (!authCheckStarted) {
            startAuthCheck()
        }
    }

    private fun startAuthCheck() {
        if (authCheckStarted) return
        authCheckStarted = true

        lifecycleScope.launch {
            authViewModel.sessionState.collect { state ->
                if (isFinishing) return@collect

                when (state) {
                    is SessionState.Authenticated -> navigateToHome()
                    is SessionState.Unauthenticated -> navigateToLogin()
                    is SessionState.Loading -> {
                        ProgressDialogFragment.show(supportFragmentManager)
                    }
                }
            }
        }
        authViewModel.checkSessionStatus()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}