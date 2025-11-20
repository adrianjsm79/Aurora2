package com.tecsup.aurora.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityLocationBinding
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment // Tu fragmento
import com.tecsup.aurora.viewmodel.LocationViewModel
import com.tecsup.aurora.viewmodel.LocationViewModelFactory

class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding

    private val viewModel: LocationViewModel by viewModels {
        (application as MyApplication).locationViewModelFactory
    }

    // Bandera para evitar que el listener del switch se dispare cuando lo cambiamos por código
    private var isUpdatingSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUIState()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUIState() {
        // Configura el RadioButton correcto según lo guardado
        val currentInterval = viewModel.trackingInterval
        val radioId = when (currentInterval) {
            30 -> R.id.radio_30s
            60 -> R.id.radio_60s
            else -> R.id.radio_10s
        }
        binding.radioGroupInterval.check(radioId)
    }

    private fun setupListeners() {
        // Listener del Switch (Activar/Desactivar)
        binding.switchTracking.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                viewModel.onTrackingSwitchChanged(isChecked)
            }
        }

        // Listener de los RadioButtons (Intervalo)
        binding.radioGroupInterval.setOnCheckedChangeListener { _, checkedId ->
            val seconds = when (checkedId) {
                R.id.radio_30s -> 30
                R.id.radio_60s -> 60
                else -> 10
            }
            viewModel.onIntervalChanged(seconds)
        }
    }

    private fun observeViewModel() {
        // 1. Observar si el tracking está activo o inactivo
        viewModel.isTrackingEnabled.observe(this) { isEnabled ->
            // Bloqueamos la bandera para que este cambio no dispare el listener de arriba
            isUpdatingSwitch = true
            binding.switchTracking.isChecked = isEnabled
            isUpdatingSwitch = false

            if(isEnabled) {
                binding.switchTracking.text = "Rastreo Activo"
            } else {
                binding.switchTracking.text = "Activar Rastreo en Tiempo Real"
            }
        }

        // 2. Observar si está cargando (para mostrar tu diálogo)
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                ProgressDialogFragment.show(supportFragmentManager)
                // Bloqueamos interacción mientras carga
                binding.switchTracking.isEnabled = false
                setRadioGroupEnabled(false)
            } else {
                ProgressDialogFragment.hide(supportFragmentManager)
                // Desbloqueamos interacción
                binding.switchTracking.isEnabled = true
                setRadioGroupEnabled(true)
            }
        }
    }

    private fun setRadioGroupEnabled(isEnabled: Boolean) {
        for (i in 0 until binding.radioGroupInterval.childCount) {
            binding.radioGroupInterval.getChildAt(i).isEnabled = isEnabled
        }
    }
}