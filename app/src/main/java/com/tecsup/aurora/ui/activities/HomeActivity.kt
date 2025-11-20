package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.viewModelScope
import com.google.android.material.navigation.NavigationView
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.databinding.ActivityHomeBinding
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.HomeState
import com.tecsup.aurora.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.ui.adapter.DeviceAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.utils.NotificationHelper
import android.provider.Settings
import android.util.Log
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var deviceAdapter: DeviceAdapter

    // 1. Obtenemos el HomeViewModel para los datos de esta pantalla
    private val homeViewModel: HomeViewModel by viewModels {
        (application as MyApplication).homeViewModelFactory
    }

    // 2. Obtenemos el AuthViewModel para la lógica de Logout
    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository, application)
    }

    // --- MANEJO DE PERMISOS ---
    // 1. Launcher para permiso 'FINE' (En primer plano)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBackgroundPermissionRationaleDialog()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launcher para permiso 'BACKGROUND' (En segundo plano)
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 4. ¡PERMISOS COMPLETOS! Inicia el servicio.
            startTrackingService()
        } else {
            // ESTO ES LO QUE TE ESTÁ PASANDO AHORA
            Toast.makeText(this, "Permiso en segundo plano denegado", Toast.LENGTH_SHORT).show()

            // Opcional: Guía al usuario a la configuración si lo niega
            AlertDialog.Builder(this)
                .setTitle("Permiso Requerido")
                .setMessage("Para rastrear su dispositivo, la app necesita permiso de 'Ubicación todo el tiempo'. Por favor, actívelo en la configuración de la app.")
                .setPositiveButton("Ir a Configuración") { _, _ ->
                    // Abre la configuración de la app
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Crea el canal de notificación
        NotificationHelper.createNotificationChannel(this)
        //listeners de navegación
        setupRecyclerView()
        setupNavigation()

        observeViewModel()

    }

    private fun setupRecyclerView() {
        Log.d("AURORA_DEBUG", "HomeActivity: Configurando RecyclerView...")
        deviceAdapter = DeviceAdapter()
        binding.devicesRecyclerView.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
            setHasFixedSize(true) // Optimización
        }
    }

    private fun observeViewModel() {
        homeViewModel.homeState.observe(this) { state ->
            when (state) {
                is HomeState.Loading -> {
                    Log.d("AURORA_DEBUG", "HomeActivity: Estado LOADING")
                    // (Opcional) Mostrar progress bar
                }
                is HomeState.Success -> {
                    Log.d("AURORA_DEBUG", "HomeActivity: Estado SUCCESS")

                    // 1. Actualizar Drawer (Nombre de Usuario)
                    updateNavigationHeader(state.userProfile.nombre, state.userProfile.email)

                    // 2. Actualizar Lista de Dispositivos
                    if (state.devices.isEmpty()) {
                        Log.w("AURORA_DEBUG", "HomeActivity: La lista de dispositivos llegó VACÍA del backend.")
                    } else {
                        Log.d("AURORA_DEBUG", "HomeActivity: Enviando ${state.devices.size} dispositivos al adaptador.")
                        deviceAdapter.submitList(state.devices)
                    }
                }
                is HomeState.Error -> {
                    Log.e("AURORA_DEBUG", "HomeActivity: Estado ERROR -> ${state.message}")
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    if (state.message.contains("401") || state.message.contains("Sesión")) {
                        handleLogout()
                    }
                }
            }
        }
    }


    private fun updateNavigationHeader(nombre: String, email: String) {
        try {
            // Accedemos al HeaderView (índice 0)
            val headerView = binding.navView.getHeaderView(0)
            if (headerView != null) {
                val nameText = headerView.findViewById<TextView>(R.id.text_username_nav)

                // Asegúrate de tener estos IDs en tu nav_header.xml
                nameText?.text = nombre
                Log.d("AURORA_DEBUG", "HomeActivity: Drawer actualizado con: $nombre")
            } else {
                Log.e("AURORA_DEBUG", "HomeActivity: No se encontró el HeaderView en el NavigationView")
            }
        } catch (e: Exception) {
            Log.e("AURORA_DEBUG", "HomeActivity: Error actualizando Drawer", e)
        }
    }

    private fun showLocationOptInDialog() {
        AlertDialog.Builder(this)
            .setTitle("Habilitar Rastreo")
            .setMessage("Para protegerte, Aurora necesita enviar tu ubicación en tiempo real. ¿Deseas activar esta función?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                // Inicia la cadena de permisos
                requestFineLocationPermission()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // DIÁLOGO #2: Explica por qué se necesita el permiso "Background"
    private fun showBackgroundPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("¡Un paso más!")
            .setMessage("Aurora necesita permiso para acceder a su ubicación 'todo el tiempo' (en segundo plano) para poder rastrear su dispositivo incluso si la app está cerrada o la pantalla bloqueada.\n\nSe le dirigirá a la configuración para habilitarlo.")
            .setPositiveButton("Entendido") { dialog, _ ->
                // 3. Ahora SÍ pedimos el permiso background
                requestBackgroundLocationPermission()
                dialog.dismiss()
            }
            .setNegativeButton("Ahora no") { dialog, _ ->
                Toast.makeText(this, "El rastreo solo funcionará mientras la app esté abierta", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun requestFineLocationPermission() {
        // Pedimos primero el permiso 'normal' (FINE)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // En Android 10+ (Q), pedimos 'BACKGROUND'
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // En Android 9 (Pie) e inferior, el permiso FINE ya incluye BACKGROUND
            startTrackingService()
        }
    }

    private fun startTrackingService() {
        // Inicia el servicio
        val intent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_SERVICE
        }
        startForegroundService(intent)

        Toast.makeText(this, "Rastreo iniciado", Toast.LENGTH_SHORT).show()
    }



    private fun setupNavigation() {
        // --- Toolbar ---
        binding.hamburgerButtonRight.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.linkWeb.setOnClickListener {
            val url = "https://auroraweb-topaz.vercel.app/" // Tu URL web
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // --- (Menú Hamburguesa) ---
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> handleLogout()
                R.id.nav_share -> shareApp()
                // R.id.nav_notifications -> ...
                // R.id.nav_about -> ...
                // R.id.nav_support -> ...
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // --- Botón Principal ---
        binding.findDevicesButton.setOnClickListener {
            // Ir al Mapa
            // startActivity(Intent(this, MapActivity::class.java))
        }

        // --- Cards ---
        // (Configura los listeners para las 4 cards)
        binding.cardLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }
        binding.cardContacts.setOnClickListener {

            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)

        }
        binding.cardSecurity.setOnClickListener {

        }
        binding.cardDevices.setOnClickListener {

        }

        // --- Bottom Navigation ---
        binding.bottomNavView.selectedItemId = R.id.bottom_home // Marca "Home" como activo
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true // Ya estamos aquí
                R.id.bottom_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.bottom_settings -> {
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun handleLogout() {
        // ¡Correcto! La Activity solo notifica la intención al ViewModel.
        authViewModel.onLogoutClicked()

        // El resto de la navegación se queda igual.
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    private fun shareApp() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "¡Descarga Aurora para mantenerte seguro! [Link]")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Compartir app..."))
    }
}