package com.tecsup.aurora.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityHomeBinding // Importante: el import de la clase Binding
import com.tecsup.aurora.fragments.DeviceItemFragment

class HomeActivity : BaseActivity() {

    //objeto de vista que Contiene todas las demás.
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inflar el layout y establecer la vista usando View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //lista de datos de (ejemplo)
        val devicesList = listOf(
            "Galaxy S23" to true,
            "Xiaomi Mi 12" to false
        )

        //llamar a las funciones de configuración y cargar los dispositivos
        loadDevices(devicesList) //se pasa la lista a la función que crea los fragments

        // Pasamos la vista raíz del layout a la función de la clase base
        setupEdgeToEdge(binding.drawerLayout)

        setupDrawer()
        setupBottomNavigation()
        setupClickListeners()
        setupOnBackPressed()
    }

    // listeners para botones y demás intents, ahora usando 'binding'
    private fun setupClickListeners() {
        binding.cardContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.cardLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        binding.cardSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        binding.cardDevices.setOnClickListener {
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        binding.findDevicesButton.setOnClickListener {
            startActivity(Intent(this, SearchmapActivity::class.java))
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

        // Acceder al botón dentro del header del NavigationView
        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.back_button_header)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    // Barra de navegacion inferior, usando 'binding'
    private fun setupBottomNavigation() {
        binding.bottomNavView.selectedItemId = R.id.bottom_home
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == binding.bottomNavView.selectedItemId) return@setOnItemSelectedListener false

            when (menuItem.itemId) {
                R.id.bottom_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.bottom_settings -> startActivity(Intent(this, SettingsActivity::class.java))
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
            R.id.btn_logout -> logout()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.END)
    }

    // Funcion para cerrar la sesion
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
                    // Logica para salir de la app o ir hacia atras
                    if (isTaskRoot) {
                        finish()                    } else {
                        super@HomeActivity.onBackPressed()
                    }
                }
            }
        })
    }

    // Funcion para cargar los dispositivos como fragments
    private fun loadDevices(devices: List<Pair<String, Boolean>>) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        devices.forEach { (name, isActive) ->
            val deviceFragment = DeviceItemFragment.newInstance(name, isActive)
            // Añadimos cada fragment al contenedor LinearLayout usando el id desde binding
            fragmentTransaction.add(binding.devicesContainer.id, deviceFragment)
        }

        fragmentTransaction.commit()
    }
}
