package com.tecsup.aurora.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Una clase base para las actividades de la aplicación.
 * Habilita el modo de borde a borde (edge-to-edge) para que la UI
 * se dibuje detrás de las barras del sistema.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita el modo edge-to-edge para que la app dibuje detrás de las barras de sistema.
        enableEdgeToEdge()
    }

    /**
     * Configura el padding para una vista raíz para evitar que el contenido se superponga
     * con las barras del sistema (barra de estado y de navegación).
     *
     * @param view La vista a la que se aplicará el padding.
     */
    protected fun setupEdgeToEdge(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Configura el padding para una vista raíz para evitar que el contenido se superponga
     * con las barras del sistema y el teclado (IME). Ideal para pantallas con EditTexts.
     *
     * @param view La vista a la que se aplicará el padding.
     */
    protected fun setupEdgeToEdgeWithIme(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeAndSystemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(imeAndSystemBars.left, imeAndSystemBars.top, imeAndSystemBars.right, imeAndSystemBars.bottom)
            insets
        }
    }
}
