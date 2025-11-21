package com.tecsup.aurora.utils

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.tecsup.aurora.R
import com.tecsup.aurora.ui.activities.HomeActivity

class NavigationDrawerController(
    private val activity: Activity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView
) {

    fun setup(onLogout: () -> Unit) {

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Si ya estamos en Home, solo cerramos el drawer
                    if (activity !is HomeActivity) {
                        val intent = Intent(activity, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        activity.startActivity(intent)
                        activity.finish()
                    }
                }
                R.id.nav_notifications -> {
                    Toast.makeText(activity, "Próximamente: Notificaciones", Toast.LENGTH_SHORT).show()
                    //intent a notificaciones
                }
                R.id.nav_about -> {
                    Toast.makeText(activity, "Aurora v1.0 - Tecsup", Toast.LENGTH_SHORT).show()
                    //intent a activity acerca de
                }
                R.id.nav_support -> {
                    //intent a activity soporte
                }
                R.id.nav_share -> {
                    shareApp()
                }
                R.id.nav_logout -> {
                    // Delegamos la lógica de logout a la Activity
                    onLogout()
                }
            }

            // Cerrar el drawer después de seleccionar
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    /**
     * Actualiza la cabecera del drawer con los datos del usuario.
     */
    fun updateHeaderUserInfo(name: String, email: String) {
        val headerView = navigationView.getHeaderView(0)
        if (headerView != null) {
            val nameText = headerView.findViewById<TextView>(R.id.text_username_nav)
            val emailText = headerView.findViewById<TextView>(R.id.text_email_nav)

            nameText.text = name
            emailText.text = email
        }
    }

    /**
     * Abre el drawer (se llama desde el botón hamburguesa).
     */
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.END)
    }

    // --- Funciones Auxiliares ---

    private fun shareApp() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Descarga Aurora para mantenerte seguro! https://github.com/adrianjsm79/Aurora2/releases")
            type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(sendIntent, "Compartir Aurora con..."))
    }

}