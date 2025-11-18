package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityLoginBinding // Usa el binding de Login
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.LoginState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // 1. Obtenemos el MISMO AuthViewModel.
    // Usamos la MISMA Factory que creamos para RegisterActivity.
    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        // Pasamos ambos parámetros requeridos por la Factory
        AuthViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Listener para el botón de Ingresar
        binding.btnIngresar.setOnClickListener {
            handleLogin()
        }

        // 3. Observador para el ESTADO de LOGIN
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.btnIngresar.isEnabled = false
                    // binding.progressBar.visibility = View.VISIBLE
                }
                is LoginState.Success -> {
                    // ÉXITO: Realm ya tiene el token.
                    binding.btnIngresar.isEnabled = true
                    // binding.progressBar.visibility = View.GONE

                    // ¡CAMBIO CLAVE AQUÍ!
                    // Le decimos a HomeActivity que muestre el diálogo.
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("SHOW_LOCATION_DIALOG", true)
                    }
                    startActivity(intent)
                    finish() // Cierra LoginActivity para que no pueda volver
                }
                is LoginState.Error -> {
                    binding.btnIngresar.isEnabled = true
                    // binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is LoginState.Idle -> {
                    binding.btnIngresar.isEnabled = true
                    // binding.progressBar.visibility = View.GONE
                }
            }
        }

        binding.registerLink.setOnClickListener {
            // Lógica para ir a RegisterActivity
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        // 4. La Activity solo recolecta datos y avisa al ViewModel
        val email = binding.inputEmail.text.toString().trim()
        val pass = binding.inputPassword.text.toString()

        viewModel.onLoginClicked(email, pass)
    }
}