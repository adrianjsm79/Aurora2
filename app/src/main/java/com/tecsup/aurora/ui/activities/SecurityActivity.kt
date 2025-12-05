package com.tecsup.aurora.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.databinding.ActivitySecurityBinding
import com.tecsup.aurora.service.AntiTheftService
import com.tecsup.aurora.viewmodel.SecurityViewModel
import androidx.lifecycle.lifecycleScope
import com.tecsup.aurora.service.SoundListenerService

class SecurityActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityBinding

    private val viewModel: SecurityViewModel by viewModels {
        (application as MyApplication).securityViewModelFactory
    }

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Si aceptó, activamos el switch y el servicio
            viewModel.toggleSoundListener(true)
        } else {
            binding.switchSoundListener.isChecked = false
            Toast.makeText(this, "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        // Actualizamos el texto del botón según el estado guardado
        viewModel.isFakeShutdownEnabled.observe(this) { isEnabled ->
            Log.d("AURORA_SECURITY", "Estado FakeShutdown observado: $isEnabled")

            if (isEnabled) {
                binding.btnEnableFakeShutdown.text = "Desactivar Protección"
                //Cambiar color del botón a rojo o gris para indicar apagado
            } else {
                binding.btnEnableFakeShutdown.text = "Activar Protección"
            }
        }

        // OBSERVAR ESTADO DEL SONAR
        viewModel.isSoundListenerEnabled.observe(this) { isEnabled ->
            binding.switchSoundListener.isChecked = isEnabled

            val intent = Intent(this, SoundListenerService::class.java)
            if (isEnabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                stopService(intent)
            }
        }
    }

    private fun setupListeners() {

        // Ir a configuración de Bloqueo de Pantalla
        binding.btnOpenLockSettings.setOnClickListener {
            try {
                // Intentamos ir directo a seguridad
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Busca 'Ajustes de bloqueo seguro'", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Si falla, vamos a la configuración general
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }

        //Ir a configuración general para buscar bloqueo de apagado
        binding.btnOpenSecuritySettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Busca 'Bloquear red y seguridad'", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        // Activar/Desactivar Apagado Falso
        binding.btnEnableFakeShutdown.setOnClickListener {
            viewModel.toggleFakeShutdown()

            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Activa el servicio 'Aurora Anti-Robo'", Toast.LENGTH_LONG).show()
            } else {
                val isEnabled = viewModel.isFakeShutdownEnabled.value == true

                Toast.makeText(this, "Configuración actualizada", Toast.LENGTH_SHORT).show()
            }
        }

        // LISTENER DEL SWITCH DE SONIDO
        binding.switchSoundListener.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // verificamos permiso
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                    binding.switchSoundListener.isChecked = false
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.toggleSoundListener(true)
                }
            } else {
                viewModel.toggleSoundListener(false)
            }
        }
    }

    // Función auxiliar para verificar si el servicio está corriendo
    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return prefString?.contains("$packageName/${AntiTheftService::class.java.name}") == true
    }
}