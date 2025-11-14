package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivitySettingsBinding // ¡Importante! Añadir el import del binding

class SettingsActivity : BaseActivity() {

    // 1. La única variable que necesitas para las vistas
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        // 1. Obtener las preferencias guardadas
        val sharedPrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        // 2. Leer el valor guardado. Si no existe, el valor por defecto será 'false' (tema claro).
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)

        // 3. Establecer el estado del switch SIN disparar el listener
        binding.switchTheme.isChecked = isDarkMode


        //Llamar a las funciones de configuración
        setupDrawer()
        setupBottomNavigation()
        setupClickListeners()
        setupOnBackPressed()
    }


    // listeners para botones, ahora usando 'binding'
    private fun setupClickListeners() {

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // 1. Aplicar el tema inmediatamente
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            // 2. Guardar la preferencia del usuario
            val sharedPrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putBoolean("is_dark_mode", isChecked)
                apply() // 'apply()' guarda los cambios en segundo plano
            }
        }

        binding.opcion1.setOnClickListener {
            Toast.makeText(this, "Opción 1 seleccionada", Toast.LENGTH_SHORT).show()
        }

        binding.opcion2.setOnClickListener {
            Toast.makeText(this, "Opción 2 seleccionada", Toast.LENGTH_SHORT).show()
        }

        binding.opcion3.setOnClickListener {
            Toast.makeText(this, "Opción 3 seleccionada", Toast.LENGTH_SHORT).show()
        }

        binding.linkWeb.setOnClickListener {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroraweb-zoe5.onrender.com"))
            startActivity(webIntent)
        }
    }

    // Configuracion del menu lateral, usando 'binding'
    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.drawer_open, R.string.drawer_close
        )
        toggle.isDrawerIndicatorEnabled = false
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.hamburgerButtonRight.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerNavigation(menuItem.itemId)
            true
        }

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<ImageButton>(R.id.back_button_header)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    // Barra de navegacion inferior, usando 'binding'
    private fun setupBottomNavigation() {
        binding.bottomNavView.selectedItemId = R.id.bottom_settings
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == binding.bottomNavView.selectedItemId) return@setOnItemSelectedListener false

            when (menuItem.itemId) {
                R.id.bottom_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.bottom_home -> startActivity(Intent(this, HomeActivity::class.java))
            }
            true
        }
    }

    // LAS OPCIONES del menu lateral
    private fun handleDrawerNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_notifications -> Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
            R.id.nav_about -> Toast.makeText(this, "Acerca de", Toast.LENGTH_SHORT).show()
            R.id.nav_support -> Toast.makeText(this, "Soporte", Toast.LENGTH_SHORT).show()
            R.id.nav_share -> shareApp()
            R.id.nav_logout -> logout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.END)
    }

    // Accion para cerrar la sesion
    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Intent implícito para compartir la app desde el menu lateral
    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Descarga Aurora App")
            putExtra(Intent.EXTRA_TEXT, "¡Te recomiendo Aurora! Una app increíble para nuestra seguridad. https://github.com/adrianjsm79/Aurora2/releases/tag/debug1")
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir vía"))
    }

    // Comportamiento del boton de regresar, usando 'binding'
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    // Esta lógica es más segura que deshabilitar el callback
                    if (isTaskRoot) {
                        finish() // Si es la última actividad, la cierra
                    } else {
                        super@SettingsActivity.onBackPressed() // Si no, regresa
                    }
                }
            }
        })
    }
}
