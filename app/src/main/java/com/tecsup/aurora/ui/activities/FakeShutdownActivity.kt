package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tecsup.aurora.databinding.ActivityFakeShutdownBinding

class FakeShutdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeShutdownBinding
    private var secretTapCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración para pantalla completa total (ocultar barras)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Mantener pantalla encendida (para que el proceso no muera)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityFakeShutdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        // 2. Bloquear botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada. El ladrón creerá que está apagado.
            }
        })

        // 3. Simular animación de apagado
        simulateShutdownProcess()

        // 4. Puerta trasera (Salir con 4 toques rápidos)
        binding.root.setOnClickListener {
            secretTapCount++
            if (secretTapCount >= 4) {
                Toast.makeText(this, "Modo Falso Desactivado", Toast.LENGTH_SHORT).show()
                finish()
            }
            // Resetear contador después de 2 segundos
            Handler(Looper.getMainLooper()).postDelayed({ secretTapCount = 0 }, 2000)
        }
    }

    private fun simulateShutdownProcess() {
        // Mostrar spinner 3 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            // Ocultar spinner, dejar pantalla negra total
            binding.shutdownAnimation.visibility = View.GONE
        }, 3000)
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}