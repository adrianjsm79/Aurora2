package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.databinding.ActivityRegisterBinding
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.RegistrationState
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment

class RegisterActivity : AppCompatActivity() {

    // 1. Configurar ViewBinding
    private lateinit var binding: ActivityRegisterBinding

    // 2. Obtener el ViewModel
    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        // Le pasamos AMBOS, el repositorio y la application a la fábrica
        AuthViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar el layout usando ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- ¡CAMBIOS CLAVE AQUÍ! ---
        // 1. Deshabilita el botón por defecto
        binding.btnCreateAccount.isEnabled = false

        // 2. Añade un listener al checkbox
        binding.checkboxTerms.setOnCheckedChangeListener { _, isChecked ->
            // 3. Habilita o deshabilita el botón según el estado del checkbox
            binding.btnCreateAccount.isEnabled = isChecked
        }

        // Configurar el Listener del Botón
        binding.btnCreateAccount.setOnClickListener {
            handleRegistration()
        }

        // Configurar el Observador de Estado
        viewModel.registrationState.observe(this) { state ->
            // El 'when' reacciona a los cambios de estado
            when (state) {
                is RegistrationState.Loading -> {
                    // Muestra un ProgressBar, deshabilita el botón
                    binding.btnCreateAccount.isEnabled = false
                    ProgressDialogFragment.show(supportFragmentManager)
                }
                is RegistrationState.Success -> {
                    // Éxito: Muestra un mensaje y ve al Login
                    binding.btnCreateAccount.isEnabled = true
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is RegistrationState.Error -> {
                    // Error: Muestra el error
                    // Re-habilita el botón solo si el checkbox está marcado
                    binding.btnCreateAccount.isEnabled = binding.checkboxTerms.isChecked
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is RegistrationState.Idle -> {
                    // Estado inicial, el botón se controla por el checkbox
                    binding.btnCreateAccount.isEnabled = binding.checkboxTerms.isChecked
                    ProgressDialogFragment.hide(supportFragmentManager)
                }
            }
        }

        binding.loginLink.setOnClickListener {
            // Lógica para ir a LoginActivity
            finish() // Cierra esta activity
        }
    }

    private fun handleRegistration() {
        // 5. La Activity solo recolecta datos y los pasa al ViewModel
        val nombre = binding.inputNombre.text.toString()
        val email = binding.inputCorreo.text.toString()
        val pass1 = binding.inputPassword.text.toString()
        val pass2 = binding.inputConfirmPassword.text.toString()

        // Importante: Combina el código de país con el número
        val numero = binding.ccp.selectedCountryCodeWithPlus +
                binding.inputCelular.text.toString()

        // Llama al "cerebro" (ViewModel)
        viewModel.onRegisterClicked(nombre, email, numero, pass1, pass2)
    }
}