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
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIngresar.setOnClickListener {
            handleLogin()
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.btnIngresar.isEnabled = false
                    ProgressDialogFragment.show(supportFragmentManager)
                }
                is LoginState.Success -> {
                    // Realm ya tiene el token.
                    binding.btnIngresar.isEnabled = true
                    ProgressDialogFragment.hide(supportFragmentManager)

                    // Le decimos a HomeActivity que muestre el diálogo.
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("SHOW_LOCATION_DIALOG", true)
                    }
                    startActivity(intent)
                    finish()
                }
                is LoginState.Error -> {
                    binding.btnIngresar.isEnabled = true
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is LoginState.Idle -> {
                    binding.btnIngresar.isEnabled = true
                    ProgressDialogFragment.hide(supportFragmentManager)
                }
            }
        }

        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleLogin() {
        val email = binding.inputEmail.text.toString().trim()
        val pass = binding.inputPassword.text.toString()

        viewModel.onLoginClicked(email, pass, this)
    }
}