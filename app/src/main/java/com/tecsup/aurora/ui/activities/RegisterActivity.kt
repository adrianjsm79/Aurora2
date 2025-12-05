package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivityRegisterBinding
import com.tecsup.aurora.ui.fragments.TermsDialogFragment
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment
import com.tecsup.aurora.viewmodel.AuthViewModel
import com.tecsup.aurora.viewmodel.AuthViewModelFactory
import com.tecsup.aurora.viewmodel.RegistrationState

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val viewModel: AuthViewModel by viewModels {
        val repository = (application as MyApplication).authRepository
        AuthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnCreateAccount.isEnabled = false

        // que el checkbox NO sea clickeable directamente
        binding.checkboxTerms.isClickable = false
        binding.checkboxTerms.isFocusable = false
    }

    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener {
            handleRegistration()
        }

        binding.termsLink.setOnClickListener {
            openTermsDialog()
        }

        //interceptamos el click en el contenedor del checkbox para abrir el diálogo
        binding.checkboxTerms.setOnClickListener { // Asumiendo que el contenedor tiene un ID
            openTermsDialog()
        }

        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        // Observa estado del REGISTRO
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
                    // Reactivar botón si los términos ya estaban aceptados
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

        // Observa la carga de terminos

        viewModel.termsHtml.observe(this) { html ->
            if (!html.isNullOrEmpty()) {
                showTermsDialog(html)
                viewModel.termsShown() // Limpiar evento
            }
        }

        // Estado de carga (Spinner mientras baja el HTML)
        viewModel.termsLoading.observe(this) { isLoading ->
            if (isLoading) {
                ProgressDialogFragment.show(supportFragmentManager)
            } else {
                ProgressDialogFragment.hide(supportFragmentManager)
            }
        }

        // Error al descargar términos
        viewModel.termsError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTermsDialog() {
        // Pedimos el documento con código 100 (Términos y condiciones)
        viewModel.loadTerms(100)
    }

    private fun showTermsDialog(htmlContent: String) {
        val dialog = TermsDialogFragment(htmlContent) {
            // ESTE CALLBACK SE EJECUTA SOLO SI LEYÓ Y ACEPTÓ
            binding.checkboxTerms.isChecked = true
            binding.btnCreateAccount.isEnabled = true

            Toast.makeText(this, "Términos aceptados", Toast.LENGTH_SHORT).show()
        }
        dialog.show(supportFragmentManager, "TermsDialog")
    }

    private fun handleRegistration() {
        val nombre = binding.inputNombre.text.toString()
        val email = binding.inputCorreo.text.toString()
        val pass1 = binding.inputPassword.text.toString()
        val pass2 = binding.inputConfirmPassword.text.toString()

        // Limpiar espacios del teléfono antes de enviar
        val rawNumber = binding.inputCelular.text.toString().trim().replace(" ", "")
        val numero = binding.ccp.selectedCountryCodeWithPlus + rawNumber

        viewModel.onRegisterClicked(nombre, email, numero, pass1, pass2)
    }
}