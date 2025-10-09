package com.tecsup.aurora

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class HomeActivity : BaseActivity() {

    // 1. Declara las variables para el Drawer a nivel de clase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // --- INICIO: LÓGICA DEL NAVIGATION DRAWER (CORREGIDO) ---

        // 2. Inicializa el DrawerLayout y la Toolbar desde el layout
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar, // Se pasa la toolbar para que el botón se integre en ella
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 4. Maneja los clics en los ítems del menú lateral
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_notifications -> {
                    // Acción para "Notificaciones"
                }
                R.id.nav_about -> {
                    // Acción para "Acerca de"
                }
                R.id.nav_support -> {
                    // Acción para "Soporte"
                }
                R.id.nav_logout -> {
                    // Acción para "Cerrar Sesión"
                }
            }
            // Cierra el menú después de seleccionar una opción
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // --- FIN: LÓGICA DEL NAVIGATION DRAWER ---


        // --- NAVEGACIÓN INFERIOR (BOTTOM NAVIGATION) ---
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNavView.selectedItemId = R.id.bottom_home

        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_home -> true // Ya estamos aquí, no hacer nada

                R.id.bottom_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                R.id.bottom_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        /**
         * Sobrescribimos este método para cerrar el drawer si está abierto
         * cuando el usuario presiona el botón de "Atrás" del sistema.
         * Es una buena práctica para la experiencia de usuario.
         */
        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                // Si el menú lateral está abierto, ciérralo.
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Si el menú está cerrado, realiza la acción por defecto (cerrar la app, ir atrás, etc.)
                    // Para ello, desactivamos este callback temporalmente y llamamos a onBackPressed() de nuevo.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}

