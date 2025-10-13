package com.tecsup.aurora.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.tecsup.aurora.R

class HomeActivity : BaseActivity() {

    //usamos lateinit para iniciar las variables despues.
    //son accesibles desde cualquier función de la clase.
    //se le asocia un tipo de dato que hace referencia a componentes que hayas usado en el xml
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var btnContacts: LinearLayout
    private lateinit var btnMap: LinearLayout
    private lateinit var btnWeb: ImageButton
    private lateinit var hamburgerButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //en oncreate ponemos todas las funciones de la configuracion de la pantalla
        initViews()

        setupEdgeToEdge(R.id.drawer_layout)
        setupDrawer()
        setupBottomNavigation()
        setupClickListeners()
        setupOnBackPressed() //esto está aqui porque como tal no forma parte de las opciones del menu
    }

    //aca asignale un id a cada variable que hayas declarado arriba
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        navView = findViewById(R.id.nav_view)
        bottomNavView = findViewById(R.id.bottom_nav_view)
        btnContacts = findViewById(R.id.card_contacts)
        btnMap = findViewById(R.id.find_devices_button)
        btnWeb = findViewById(R.id.link_web)
        hamburgerButton = findViewById(R.id.hamburger_button_right)
    }



    //listeners para botones y demás intents
    private fun setupClickListeners() {
        btnContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        btnMap.setOnClickListener {
            startActivity(Intent(this, SearchmapActivity::class.java))
        }

        btnWeb.setOnClickListener {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroraweb-zoe5.onrender.com"))
            startActivity(webIntent)
        }

        //si quieres añadir un nuevo botón:
        //declararlo arriba con el lateinit.
        //inicializalo en initViews().
        //añade su listener aquí, sin cambiar ninguna otra función.

        //nuevoBoton.setOnClickListener{
        // startActivity(Intent(this, EjemploActivity::class.java))
        //}

        //para implicitos hazlo diferente, poné algo asi antes del startActivity:
        //val ejemploIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ejemplo.com"))
        //ten en cuenta que la parte de "ACTION_VIEW" cambia dependiendo lo que quieras hacer"
    }




    //configuracion del menu lateral
    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close
        )
        toggle.isDrawerIndicatorEnabled = false //esto deshabilita el icono que tiene android por defecto no lo quites porfa.
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        hamburgerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerNavigation(menuItem.itemId)
            true
        }

        val headerView = navView.getHeaderView(0)
        headerView.findViewById<ImageButton>(R.id.back_button_header)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    //barra de navegacion inferior
    private fun setupBottomNavigation() {
        bottomNavView.selectedItemId = R.id.bottom_home
        bottomNavView.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == bottomNavView.selectedItemId) return@setOnItemSelectedListener false

            when (menuItem.itemId) {
                R.id.bottom_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.bottom_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
    }

    //LAS OPCIONES del menu lateral
    private fun handleDrawerNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_notifications -> Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
            R.id.nav_about -> Toast.makeText(this, "Acerca de", Toast.LENGTH_SHORT).show()
            R.id.nav_support -> Toast.makeText(this, "Soporte", Toast.LENGTH_SHORT).show()
            R.id.nav_share -> shareApp()
            R.id.btn_logout -> logout()
        }
        drawerLayout.closeDrawer(GravityCompat.END)
    }

    //accion para cerrar la sesion
    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    //intent implisito para compartir la app desde el menu lateral
    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Descarga Aurora App")
            putExtra(Intent.EXTRA_TEXT, "¡Te recomiendo Aurora! Una app increíble para nuestra seguridad. Descárgala aquí: https://github.com/adrianjsm79/Aurora2")
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir vía"))
    }

    //comportamiento del boton de regresar en el menu lateral
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
