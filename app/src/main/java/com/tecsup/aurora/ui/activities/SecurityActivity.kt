package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar // Importante: usar la de androidx
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivitySecurityBinding

class SecurityActivity : BaseActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var binding: ActivitySecurityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        // Establece el listener para los clics en los ítems del menú (tres puntos)
        binding.toolbarSecurity.setOnMenuItemClickListener(this)

        // Establece el listener para el ícono de navegación (flecha atrás)
        binding.toolbarSecurity.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Listeners para las opciones de seguridad
        binding.optionChangePassword.setOnClickListener {
            Toast.makeText(this, "Navegando a 'Cambiar Contraseña'", Toast.LENGTH_SHORT).show()
        }

        binding.option2fa.setOnClickListener {
            Toast.makeText(this, "Navegando a 'Autenticación de dos factores'", Toast.LENGTH_SHORT).show()
        }

        binding.optionManageSessions.setOnClickListener {
            Toast.makeText(this, "Navegando a 'Gestionar Sesiones'", Toast.LENGTH_SHORT).show()
        }
    }

    // Este método se llama cuando un ítem del menú de la toolbar es presionado
    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_security_history -> {
                Toast.makeText(this, "Mostrando historial de seguridad", Toast.LENGTH_SHORT).show()
                true // Indica que hemos manejado el clic
            }
            R.id.menu_help -> {
                Toast.makeText(this, "Mostrando pantalla de ayuda", Toast.LENGTH_SHORT).show()
                true // Indica que hemos manejado el clic
            }
            else -> false // Deja que el sistema maneje otros clics
        }
    }
}
