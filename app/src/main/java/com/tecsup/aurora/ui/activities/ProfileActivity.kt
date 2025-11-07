package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.GravityCompat
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityProfileBinding

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        setupDrawer()
        setupBottomNavigation()
        setupClickListeners()
        setupOnBackPressed()
    }

    // Listeners para botones y demás intents usando View Binding
    private fun setupClickListeners() {
        binding.goToSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }
        binding.btnEditUser.setOnClickListener {
            Toast.makeText(this, "Editar usuario", Toast.LENGTH_SHORT).show()
        }
        binding.btnEditNumber.setOnClickListener {
            Toast.makeText(this, "Editar numero", Toast.LENGTH_SHORT).show()
        }
        binding.btnEditEmail.setOnClickListener {
            Toast.makeText(this, "Editar email", Toast.LENGTH_SHORT).show()
        }

        binding.linkWeb.setOnClickListener {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroraweb-zoe5.onrender.com"))
            startActivity(webIntent)
        }
    }

    // Configuracion del menu lateral usando View Binding
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
        headerView.findViewById<AppCompatImageButton>(R.id.back_button_header)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    // Barra de navegacion inferior usando View Binding
    private fun setupBottomNavigation() {
        binding.bottomNavView.selectedItemId = R.id.bottom_profile
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == binding.bottomNavView.selectedItemId) return@setOnItemSelectedListener false

            when (menuItem.itemId) {
                R.id.bottom_home -> startActivity(Intent(this, HomeActivity::class.java))
                R.id.bottom_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
    }

    // Opciones del menu lateral
    private fun handleDrawerNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_notifications -> Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
            R.id.nav_about -> Toast.makeText(this, "Acerca de", Toast.LENGTH_SHORT).show()
            R.id.nav_support -> Toast.makeText(this, "Soporte", Toast.LENGTH_SHORT).show()
            R.id.nav_share -> shareApp()
            R.id.btn_logout -> logout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.END)
    }

    // Acción para cerrar la sesión
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

    // Comportamiento del boton de regresar
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    // Al estar en una pantalla secundaria, simplemente volvemos atrás
                    super@ProfileActivity.onBackPressed()
                }
            }
        })
    }
}
