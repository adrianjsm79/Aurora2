package com.tecsup.aurora.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
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
import com.tecsup.aurora.viewmodel.HomeViewModelFactory


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var deviceAdapter: DeviceAdapter

    private lateinit var drawerController: NavigationDrawerController

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as MyApplication
        HomeViewModelFactory(
            app.authRepository,
            app.deviceRepository,
            app.locationRepository,
            app.settingsRepository
        )
    }

    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository, application)
    }

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
        setupDrawer()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showLocationOptInDialog()
            }
        }
    }

    private fun setupDrawer() {
        drawerController = NavigationDrawerController(
            this,
            binding.drawerLayout,
            binding.navView
        )
        drawerController.setup(onLogout = {
            handleLogout()
        })
    }

    private fun setupRecyclerView() {
        Log.d("AURORA_DEBUG", "HomeActivity: Configurando RecyclerView...")
        deviceAdapter = DeviceAdapter { device, action ->
            when (action) {
                DeviceAdapter.DeviceAction.TOGGLE_LOST -> {
                    val statusMsg = if (device.is_lost) "seguro" else "perdido"
                    Toast.makeText(this, "Marcando como $statusMsg...", Toast.LENGTH_SHORT).show()
                    homeViewModel.toggleDeviceLostState(device)
                }
                DeviceAdapter.DeviceAction.DELETE -> {
                    showDeleteConfirmationDialog(device)
                }
                DeviceAdapter.DeviceAction.EDIT_NAME -> {
                    showRenameDialog(device)
                }
            }
        }
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
                    Log.d("AURORA_DEBUG", "HomeActivity: Datos actualizados")

                    binding.swipeRefresh.isRefreshing = false
                    drawerController.updateHeaderUserInfo(
                        state.userProfile.nombre,
                        state.userProfile.email,
                        state.userProfile.image
                    )

                    if (state.devices.isEmpty()) {
                        binding.devicesRecyclerView.visibility = View.GONE
                        binding.emptyDevicesView.visibility = View.VISIBLE
                    } else {
                        binding.devicesRecyclerView.visibility = View.VISIBLE
                        binding.emptyDevicesView.visibility = View.GONE
                        deviceAdapter.submitList(state.devices)
                    }
                }
                is HomeState.Error -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Log.e("AURORA_DEBUG", "HomeActivity: Error -> ${state.message}")

                    binding.swipeRefresh.isRefreshing = false

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

        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.turquesa)

            setOnRefreshListener {
                Log.d("AURORA_DEBUG", "HomeActivity: Swipe detectado, recargando...")
                homeViewModel.loadData()
            }
        }

        binding.hamburgerButtonRight.setOnClickListener {
            drawerController.openDrawer()
        }

        binding.linkWeb.setOnClickListener {
            val url = "https://auroraweb-topaz.vercel.app/"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        binding.findDevicesButton.setOnClickListener {
            startActivity(Intent(this, SearchMapActivity::class.java))
        }

        binding.btnActionLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        binding.btnActionContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.btnActionSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        binding.bottomNavView.selectedItemId = R.id.bottom_home // Marca Perfil como activo
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true // Ya estamos aquí
                R.id.bottom_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish() // Cerramos Home para ahorrar memoria
                    true
                }
                R.id.bottom_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish() // Cerramos ProfileActivity
                    true
                }
                else -> false
            }
        }
    }

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

    private fun showDeleteConfirmationDialog(device: DeviceResponse) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Dispositivo")
            .setMessage("¿Estás seguro de que quieres eliminar '${device.name}'? Dejarás de ver su ubicación.")
            .setPositiveButton("Eliminar") { _, _ ->
                homeViewModel.deleteDevice(device.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRenameDialog(device: DeviceResponse) {

        val input = EditText(this).apply {
            hint = "Nuevo nombre"
            setText(device.name)
            setSelection(text.length)
            setPadding(60, 40, 60, 0)
            background = null
        }

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Cambiar nombre")
            .setMessage("Introduce un nuevo nombre para este dispositivo:")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    homeViewModel.renameDevice(device.id, newName)
                } else {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}