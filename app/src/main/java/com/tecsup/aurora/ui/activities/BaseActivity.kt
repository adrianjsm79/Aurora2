package com.tecsup.aurora.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//ESTA ACTIVITY ES PARA MANEJAR LOS ESPACIOS DEL DISPOSITIVO
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    protected fun setupSystemBars() {
        window.setDecorFitsSystemWindows(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Versión para Android 11+
            val controller = window.insetsController ?: return

            // Ocultar la barra de navegación inferior
            controller.hide(WindowInsets.Type.systemBars())

            // Definir el comportamiento cuando el usuario desliza desde el borde inferior
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Solución para el teclado (esto no cambia)
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
                val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                if (imeVisible) {
                    controller.show(WindowInsets.Type.navigationBars())
                } else {
                    controller.hide(WindowInsets.Type.navigationBars())
                }
                insets
            }
        } else {
            // Versión antigua para Android 10 y anteriores
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Oculta la barra de navegación
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    protected fun setupContentPadding(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}