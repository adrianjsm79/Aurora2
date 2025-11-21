package com.tecsup.aurora.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityHomeBinding
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.ui.adapter.DeviceAdapter
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
import com.tecsup.aurora.utils.NavigationDrawerController
import com.tecsup.aurora.utils.NotificationHelper
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.HomeState
import com.tecsup.aurora.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var deviceAdapter: DeviceAdapter

    // [COPIAR] Declarar el controlador del Drawer
    private lateinit var drawerController: NavigationDrawerController

    private val homeViewModel: HomeViewModel by viewModels {
        (application as MyApplication).homeViewModelFactory
    }

    // [COPIAR] Necesitas el AuthViewModel para la lógica de Logout
    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository, application)
    }

    // --- MANEJO DE PERMISOS (Específico de HomeActivity) ---
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showBackgroundPermissionRationaleDialog()
        else Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startTrackingService()
        else showPermissionDeniedDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        // [COPIAR] Inicializar el Drawer
        setupDrawer()

        setupRecyclerView()
        setupClickListeners() // (Contiene la lógica de botones y navegación)
        observeViewModel()

        // Lógica de permisos (específico de Home)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showLocationOptInDialog()
            }
        }
    }

    // [COPIAR] Función para configurar el Drawer Controller
    private fun setupDrawer() {
        drawerController = NavigationDrawerController(
            this,
            binding.drawerLayout,
            binding.navView
        )
        // Le pasamos nuestra función de logout
        drawerController.setup(onLogout = {
            handleLogout()
        })
    }

    private fun setupRecyclerView() {
        Log.d("AURORA_DEBUG", "HomeActivity: Configurando RecyclerView...")
        deviceAdapter = DeviceAdapter()
        binding.devicesRecyclerView.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        homeViewModel.homeState.observe(this) { state ->
            when (state) {
                is HomeState.Loading -> {
                    ProgressDialogFragment.show(supportFragmentManager)
                }
                is HomeState.Success -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    // [COPIAR] Actualizar Header del Drawer con datos reales
                    drawerController.updateHeaderUserInfo(
                        state.userProfile.nombre,
                        state.userProfile.email
                    )

                    // Lógica específica de Home (Lista de dispositivos)
                    if (state.devices.isEmpty()) {
                        Log.w("AURORA_DEBUG", "Lista de dispositivos vacía.")
                    } else {
                        deviceAdapter.submitList(state.devices)
                    }
                }
                is HomeState.Error -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    if (state.message.contains("401") || state.message.contains("Sesión")) {
                        handleLogout()
                    } else {
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // [COPIAR] Botón Hamburguesa usando el controlador
        binding.hamburgerButtonRight.setOnClickListener {
            drawerController.openDrawer()
        }

        binding.linkWeb.setOnClickListener {
            val url = "https://auroraweb-topaz.vercel.app/"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // --- Cards ---
        binding.cardLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
        binding.cardContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        binding.cardSecurity.setOnClickListener {
            // TODO: Implementar ActivitySecurity
        }
        binding.cardDevices.setOnClickListener {
            // TODO: Implementar ActivityDevices
        }

        // --- Bottom Navigation ---
        binding.bottomNavView.selectedItemId = R.id.bottom_home
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true
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
    }

    // [COPIAR] Lógica de Logout estándar
    private fun handleLogout() {
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }
        startService(stopIntent)
        authViewModel.onLogoutClicked()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- FUNCIONES DE PERMISOS (Específicas de Home, no copiar a otras activities) ---

    private fun showLocationOptInDialog() {
        AlertDialog.Builder(this)
            .setTitle("Habilitar Rastreo")
            .setMessage("Para protegerte, Aurora necesita enviar tu ubicación en tiempo real. ¿Deseas activar esta función?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showBackgroundPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("¡Un paso más!")
            .setMessage("Aurora necesita permiso para acceder a su ubicación 'todo el tiempo' para rastrear el dispositivo bloqueado.")
            .setPositiveButton("Entendido") { dialog, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    startTrackingService()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Ahora no") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Requerido")
            .setMessage("Sin permiso de segundo plano, el rastreo se detendrá al cerrar la app.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Rastreo iniciado", Toast.LENGTH_SHORT).show()
    }
}