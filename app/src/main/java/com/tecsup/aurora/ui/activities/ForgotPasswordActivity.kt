package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityForgotPasswordBinding
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.PasswordResetState

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    private var email = ""
    private var isCodeSentMode = false // Controla si estamos en el paso 1 o 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Limpiamos estado anterior por si acaso
        viewModel.resetState()

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnAction.setOnClickListener {
            if (!isCodeSentMode) {
                //ENVIAR CÓDIGO
                email = binding.inputEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    viewModel.requestPasswordReset(email)
                } else {
                    binding.layoutEmail.error = "Ingresa tu correo"
                }
            } else {
                //VERIFICAR CÓDIGO
                val code = binding.inputCode.text.toString().trim()
                if (code.length == 6) {
                    viewModel.verifyResetCode(email, code)
                } else {
                    binding.layoutCode.error = "El código debe tener 6 dígitos"
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.passwordResetState.observe(this) { state ->
            when (state) {
                is PasswordResetState.Loading -> {
                    binding.btnAction.isEnabled = false
                    binding.btnAction.text = "Cargando..."
                }

                is PasswordResetState.CodeSent -> {
                    //El correo se envió
                    binding.btnAction.isEnabled = true

                    // Cambiamos la UI al SEGUNDO MODE
                    isCodeSentMode = true
                    binding.layoutEmail.isEnabled = false // Bloqueamos el email
                    binding.layoutCode.visibility = View.VISIBLE // Mostramos input código
                    binding.btnAction.text = "Verificar Código"
                    binding.textInstruction.text = "Hemos enviado un código a $email. Ingrésalo abajo."

                    Toast.makeText(this, "Código enviado a tu correo", Toast.LENGTH_SHORT).show()
                }

                is PasswordResetState.CodeVerified -> {
                    //El código es válido
                    binding.btnAction.isEnabled = true
                    binding.btnAction.text = "Verificado"

                    // Navegar a la pantalla de cambio de contraseña
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.putExtra("EMAIL", email)
                    intent.putExtra("CODE", binding.inputCode.text.toString().trim())
                    startActivity(intent)
                    finish()
                }

                is PasswordResetState.Error -> {
                    binding.btnAction.isEnabled = true
                    // se restaura el texto del botón según el modo
                    binding.btnAction.text = if (isCodeSentMode) "Verificar Código" else "Enviar Código"

                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }

                is PasswordResetState.Idle -> {
                    binding.btnAction.isEnabled = true
                }
                else -> {}
            }
        }
    }
}