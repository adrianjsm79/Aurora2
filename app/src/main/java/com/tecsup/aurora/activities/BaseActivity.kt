package com.tecsup.aurora.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Una clase base para las actividades de la aplicación.
 * Implementa automáticamente el modo inmersivo a pantalla completa.
 * Todas las actividades que hereden de esta clase ocuparán toda la pantalla,
 * ocultando las barras del sistema. Las barras pueden ser reveladas
 * temporalmente deslizando desde los bordes de la pantalla.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        //Obtenemos el controlador de las barras del sistema para poder manipularlas.
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        //Ocultamos las barras de estado (arriba) y de navegación (abajo).
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        //al deslizar desde los bordes de la pantalla aparecen denuevo
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    protected fun setupEdgeToEdge(rootId: Int) {
        val rootView = findViewById<View>(rootId)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}