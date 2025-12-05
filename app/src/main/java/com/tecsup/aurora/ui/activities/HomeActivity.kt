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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.ActivityHomeBinding
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.ui.adapter.DeviceAdapter
import com.tecsup.aurora.utils.NavigationDrawerController
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
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

    // 1. Obtenemos el HomeViewModel
    private val homeViewModel: HomeViewModel by viewModels {
        (application as MyApplication).homeViewModelFactory
    }

    // 2. Obtenemos el AuthViewModel para Logout
    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    // --- MANEJO DE PERMISOS ---
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBackgroundPermissionRationaleDialog()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTrackingService()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        setupDrawer()
        setupRecyclerView()
        setupClickListeners()

        // Observamos antes de lógica
        observeViewModel()

        // Verificación inicial de permisos (si es necesario)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showLocationOptInDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Aseguramos que el tab correcto esté seleccionado al volver
        binding.bottomNavView.selectedItemId = R.id.bottom_home
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

            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun observeViewModel() {
        homeViewModel.homeState.observe(this) { state ->
            when (state) {
                is HomeState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        ProgressDialogFragment.show(supportFragmentManager)
                    }
                }
                is HomeState.Success -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    binding.swipeRefresh.isRefreshing = false

                    Log.d("AURORA_DEBUG", "HomeActivity: Datos recibidos. Usuario ID: ${state.userProfile.id}")

                    drawerController.updateHeaderUserInfo(
                        state.userProfile.nombre,
                        state.userProfile.email,
                        state.userProfile.image
                    )

                    val myDevices = state.devices.filter { device ->
                        device.user == state.userProfile.id
                    }

                    if (myDevices.isEmpty()) {
                        binding.devicesRecyclerView.visibility = View.GONE
                        binding.emptyDevicesView.visibility = View.VISIBLE
                    } else {
                        binding.devicesRecyclerView.visibility = View.VISIBLE
                        binding.emptyDevicesView.visibility = View.GONE

                        deviceAdapter.submitList(myDevices) {
                            binding.devicesRecyclerView.scrollToPosition(0)
                            binding.devicesRecyclerView.requestLayout() // Fuerza el redibujado
                        }
                    }
                }
                is HomeState.Error -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    binding.swipeRefresh.isRefreshing = false

                    Log.e("AURORA_DEBUG", "HomeActivity: Error -> ${state.message}")

                    if (state.message.contains("401") || state.message.contains("Sesión")) {
                        handleLogout()
                    } else {
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        homeViewModel.timeUpdateEvent.observe(this) {
             deviceAdapter.notifyItemRangeChanged(0, deviceAdapter.itemCount, DeviceAdapter.PAYLOAD_UPDATE_TIME)
        }
    }

    private fun setupClickListeners() {
        // Swipe Refresh
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.magenta) // Asegúrate que este color exista, o usa R.color.purple_500
            setOnRefreshListener {
                Log.d("AURORA_DEBUG", "HomeActivity: Swipe detectado...")
                homeViewModel.loadData()
            }
        }

        // Toolbar
        binding.hamburgerButtonRight.setOnClickListener {
            drawerController.openDrawer()
        }
        binding.linkWeb.setOnClickListener {
            val url = "https://auroraweb-topaz.vercel.app/"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        // Hero Button
        binding.findDevicesButton.setOnClickListener {
            startActivity(Intent(this, SearchMapActivity::class.java))
        }

        // Lista de Acciones
        binding.btnActionLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
        binding.btnActionContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        binding.btnActionSecurity.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        // Bottom Navigation
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> true
                R.id.bottom_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleLogout() {
        // Parar servicio
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_SERVICE
        }
        startService(stopIntent)

        // Limpiar datos
        authViewModel.onLogoutClicked()

        // Ir a Login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- DIÁLOGOS Y PERMISOS ---

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
            .setMessage("Sin el permiso de ubicación en segundo plano, el rastreo no funcionará correctamente.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
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

    // --- DIÁLOGOS DE ACCIÓN ---

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
            // Padding para que no se vea pegado
            setPadding(50, 30, 50, 30)
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