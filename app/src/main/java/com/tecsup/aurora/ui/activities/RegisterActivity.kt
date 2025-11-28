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

    private lateinit var binding: ActivityRegisterBinding

    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateAccount.isEnabled = false

        binding.checkboxTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnCreateAccount.isEnabled = isChecked
        }

        binding.btnCreateAccount.setOnClickListener {
            handleRegistration()
        }

        viewModel.registrationState.observe(this) { state ->
            when (state) {
                is RegistrationState.Loading -> {
                    binding.btnCreateAccount.isEnabled = false
                    ProgressDialogFragment.show(supportFragmentManager)
                }
                is RegistrationState.Success -> {
                    binding.btnCreateAccount.isEnabled = true
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is RegistrationState.Error -> {
                    binding.btnCreateAccount.isEnabled = binding.checkboxTerms.isChecked
                    ProgressDialogFragment.hide(supportFragmentManager)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is RegistrationState.Idle -> {
                    binding.btnCreateAccount.isEnabled = binding.checkboxTerms.isChecked
                    ProgressDialogFragment.hide(supportFragmentManager)
                }
            }
        }

        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun handleRegistration() {
        val nombre = binding.inputNombre.text.toString()
        val email = binding.inputCorreo.text.toString()
        val pass1 = binding.inputPassword.text.toString()
        val pass2 = binding.inputConfirmPassword.text.toString()

        val numero = binding.ccp.selectedCountryCodeWithPlus +
                binding.inputCelular.text.toString()

        viewModel.onRegisterClicked(nombre, email, numero, pass1, pass2)
    }
}