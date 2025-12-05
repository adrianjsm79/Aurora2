package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityResetPasswordBinding
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding

    // Usamos el mismo AuthViewModel ya que contiene la lógica de autenticación
    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    private var email: String? = null
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar datos del Intent
        email = intent.getStringExtra("EMAIL")
        code = intent.getStringExtra("CODE")

        if (email == null || code == null) {
            Toast.makeText(this, "Error: Datos de recuperación faltantes", Toast.LENGTH_LONG).show()
            finish() // Cierra si no hay datos
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnReset.setOnClickListener {
            val pass1 = binding.inputNewPassword.text.toString()
            val pass2 = binding.inputConfirmPassword.text.toString()

            // Validaciones Locales
            if (pass1.length < 8) {
                binding.inputNewPassword.error = "Mínimo 8 caracteres"
                return@setOnClickListener
            }

            if (pass1 != pass2) {
                binding.inputConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            performReset(pass1)
        }
    }

    private fun performReset(newPass: String) {
        // Deshabilitar botón para evitar doble click
        binding.btnReset.isEnabled = false
        binding.btnReset.text = "Cargando..."

        // Aquí lo hago directo con el repositorio a través del scope para simplificar,
        // Hay que moverlo al viewmodel.

        lifecycleScope.launch {
            try {
                val repository = (application as MyApplication).authRepository

                // Llamada al endpoint 'confirmPasswordReset' que creamos antes
                repository.confirmPasswordReset(email!!, code!!, newPass)

                Toast.makeText(this@ResetPasswordActivity, "Contraseña actualizada con éxito", Toast.LENGTH_LONG).show()

                val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                // Limpiar pila para que no pueda volver atrás
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                binding.btnReset.isEnabled = true
                binding.btnReset.text = "Restablecer Contraseña"
                Toast.makeText(this@ResetPasswordActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}