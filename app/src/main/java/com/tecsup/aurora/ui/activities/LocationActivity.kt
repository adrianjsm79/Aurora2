package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityLocationBinding
import com.tecsup.aurora.viewmodel.LocationViewModel
import com.tecsup.aurora.viewmodel.LocationViewModelFactory

class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding

    private val viewModel: LocationViewModel by viewModels {
        LocationViewModelFactory(
            (application as MyApplication).settingsRepository,
            (application as MyApplication).trackingServiceManager
        )
    }

    private var isObserving = true // Variable para evitar bucles en los listeners

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish() // Cierra la activity y vuelve atrás
        }
    }

    private fun observeViewModel() {
        // 1. OBSERVA EL ESTADO DEL SWITCH
        viewModel.isTrackingEnabled.observe(this) { isEnabled ->
            isObserving = false // Pausa el listener para evitar bucle
            binding.switchTracking.isChecked = isEnabled
            isObserving = true // Reanuda el listener
        }

        // 2. ESTABLECE EL ESTADO INICIAL DEL RADIO GROUP
        val interval = viewModel.trackingInterval
        val radioId = when (interval) {
            30 -> R.id.radio_30s
            60 -> R.id.radio_60s
            else -> R.id.radio_10s
        }
        binding.radioGroupInterval.findViewById<RadioButton>(radioId)?.isChecked = true
    }

    private fun setupListeners() {
        // 3. ESCUCHA LOS CLICS DEL USUARIO EN EL SWITCH
        binding.switchTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isObserving) {
                // (Aquí deberías pedir permisos antes de llamar al ViewModel)
                // (Por ahora, asumimos que ya los tienes desde HomeActivity)
                viewModel.onTrackingSwitchChanged(isChecked)

                val status = if (isChecked) "activado" else "desactivado"
                Toast.makeText(this, "Rastreo $status", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. ESCUCHA LOS CLICS DEL USUARIO EN EL RADIO GROUP
        binding.radioGroupInterval.setOnCheckedChangeListener { _, checkedId ->
            val interval = when (checkedId) {
                R.id.radio_30s -> 30
                R.id.radio_60s -> 60
                else -> 10
            }
            viewModel.onIntervalChanged(interval)
            Toast.makeText(this, "Intervalo cambiado a $interval seg", Toast.LENGTH_SHORT).show()
        }
    }
}