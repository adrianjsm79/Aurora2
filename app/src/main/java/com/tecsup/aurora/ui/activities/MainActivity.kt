package com.tecsup.aurora.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityMainBinding
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
        AuthViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(BatteryOptimizationActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val optimizationSeen = prefs.getBoolean(BatteryOptimizationActivity.KEY_OPTIMIZATION_SEEN, false)

        // If it's a problematic device and the user hasn't seen the optimization screen
        if (!optimizationSeen && DeviceHelper.isProblematicManufacturer()) {
            // Start the optimization activity and wait for the user to return.
            // The auth flow will be triggered in onResume.
            startActivity(Intent(this, BatteryOptimizationActivity::class.java))
            return // Stop further execution in onCreate
        }

        // If not a problematic device, or if the screen has been seen, start the auth check immediately.
        startAuthCheck()
    }

    override fun onResume() {
        super.onResume()
        // This will be called when returning from BatteryOptimizationActivity.
        // We start the check here to continue the flow.
        startAuthCheck()
    }

    private fun startAuthCheck() {
        // Ensure this block runs only once.
        if (authCheckStarted) return
        authCheckStarted = true

        lifecycleScope.launch {
            authViewModel.sessionState.collect { state ->
                // Prevent navigation if the activity is already finishing.
                if (isFinishing) return@collect

                when (state) {
                    is SessionState.Authenticated -> navigateToHome()
                    is SessionState.Unauthenticated -> navigateToLogin()
                    is SessionState.Loading -> { /* The splash screen handles the loading state */ }
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
