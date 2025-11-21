package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityProfileBinding
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.utils.NavigationDrawerController
import com.tecsup.aurora.viewmodel.ProfileState
import com.tecsup.aurora.viewmodel.ProfileViewModel
import com.tecsup.aurora.viewmodel.ProfileViewModelFactory
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var drawerController: NavigationDrawerController

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory((application as MyApplication).authRepository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()
        setupToolbar()
        setupListeners()
        observeViewModel()

        // Cargar datos al iniciar
        viewModel.loadProfile()
    }

    private fun setupDrawer() {
        val drawerLayout = binding.root as DrawerLayout

        drawerController = NavigationDrawerController(
            this,
            drawerLayout,
            binding.navView
        )
        drawerController.setup(onLogout = { handleLogout() })
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() } // Botón atrás
    }

    private fun setupListeners() {
        // Botón Hamburguesa
        binding.hamburgerButtonRight.setOnClickListener {
            drawerController.openDrawer()
        }

        // Botón Guardar
        binding.btnSave.setOnClickListener {
            val nombre = binding.inputNombre.text.toString()
            val numero = binding.inputNumero.text.toString()
            viewModel.saveProfile(nombre, numero)
        }

        // --- Bottom Navigation ---
        binding.bottomNavView.selectedItemId = R.id.bottom_profile
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_profile -> true
                R.id.bottom_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
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

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ProfileState.Loading -> {
                    ProgressDialogFragment.show(supportFragmentManager)
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = "Guardando..."
                }
                is ProfileState.DataLoaded -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Guardar Cambios"

                    // Rellenar campos
                    binding.inputNombre.setText(state.userProfile.nombre)
                    binding.inputEmail.setText(state.userProfile.email)
                    binding.inputNumero.setText(state.userProfile.numero)

                    drawerController.updateHeaderUserInfo(
                        state.userProfile.nombre,
                        state.userProfile.email
                    )
                    }
                is ProfileState.UpdateSuccess -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Guardar Cambios"
                    Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                is ProfileState.Error -> {
                    ProgressDialogFragment.hide(supportFragmentManager)
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Guardar Cambios"
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
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
}