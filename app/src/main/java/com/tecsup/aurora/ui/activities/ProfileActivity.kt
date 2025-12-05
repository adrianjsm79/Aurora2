package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.colorResource
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import coil.load
import coil.transform.CircleCropTransformation
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityProfileBinding
import com.tecsup.aurora.service.TrackingService
import com.tecsup.aurora.utils.NavigationDrawerController
import com.tecsup.aurora.viewmodel.ProfileState
import com.tecsup.aurora.viewmodel.ProfileViewModel
import com.tecsup.aurora.viewmodel.ProfileViewModelFactory
import com.tecsup.aurora.data.model.UserProfile
import com.tecsup.aurora.ui.fragments.ChangePasswordDialog
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var drawerController: NavigationDrawerController

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory((application as MyApplication).authRepository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        }
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
        binding.hamburgerButtonRight.setOnClickListener {
            drawerController.openDrawer()
        }

        binding.btnChangePassword.setOnClickListener {
            val dialog = ChangePasswordDialog { newPassword ->
                // Cuando el usuario confirma en el diálogo, ejecutamos esto:
                updatePasswordOnly(newPassword)
            }
            dialog.show(supportFragmentManager, "ChangePasswordDialog")
        }

        // 3. Botón Guardar (CORREGIDO)
        binding.btnSave.setOnClickListener {
            val nombre = binding.inputNombre.text.toString()
            val numero = binding.inputNumero.text.toString()
            val email = binding.inputEmail.text.toString()

            // Convertimos la URI de la imagen seleccionada (si hay) a un Archivo real
            val imageFile = viewModel.selectedImageUri.value?.let { uri ->
                uriToFile(uri)
            }

            viewModel.saveProfile(nombre, email, numero, null, imageFile)
        }

        binding.profileImageContainer.setOnClickListener {
            // Abrir galería solo imágenes
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.bottomNavView.selectedItemId = R.id.bottom_profile
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_profile -> true
                R.id.bottom_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    // Flags para limpiar la pila y volver al Home existente si es posible
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish() // Cerramos ProfileActivity para ahorrar memoria
                    true
                }
                R.id.bottom_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
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

                    binding.inputNombre.setText(state.userProfile.nombre)
                    binding.inputEmail.setText(state.userProfile.email)
                    binding.inputNumero.setText(state.userProfile.numero)

                    binding.inputEmail.isEnabled = true

                    // Cargar imagen desde URL (si no hay una nueva seleccionada localmente)
                    if (viewModel.selectedImageUri.value == null) {
                        state.userProfile.image?.let { imageUrl ->
                            binding.profileImage.load(imageUrl) {
                                transformations(CircleCropTransformation())
                                placeholder(R.drawable.ic_person)
                                error(R.drawable.ic_lock)
                            }
                        }
                    }
                    drawerController.updateHeaderUserInfo(
                        state.userProfile.nombre,
                        state.userProfile.email,
                        state.userProfile.image
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

    // Función auxiliar para convertir Uri de galería a File real
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File.createTempFile("profile_upload", ".jpg", cacheDir)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun updatePasswordOnly(newPassword: String) {
        // Reusamos los datos actuales de los campos de texto
        val nombre = binding.inputNombre.text.toString()
        val numero = binding.inputNumero.text.toString()
        val email = binding.inputEmail.text.toString()

        // Llamamos a saveProfile
        viewModel.saveProfile(nombre, email, numero, newPassword, null)
    }
}