package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityLocationBinding
import com.tecsup.aurora.ui.fragments.ProgressDialogFragment // Tu fragmento
import com.tecsup.aurora.utils.MorseEmitter
import com.tecsup.aurora.viewmodel.LocationViewModel
import com.tecsup.aurora.viewmodel.LocationViewModelFactory
import kotlinx.coroutines.launch

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

        binding.switchBoot.isChecked = viewModel.isStartOnBootEnabled()
    }

    private fun setupListeners() {
        // Listener del Switch (Activar/Desactivar)
        binding.switchTracking.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                viewModel.onTrackingSwitchChanged(isChecked)
            }
        }

        binding.switchBoot.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onStartOnBootChanged(isChecked)
            if (isChecked) {
                Toast.makeText(this, "Protección de localizacion contra reinicio activada", Toast.LENGTH_SHORT).show()
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

        // Botón de emitir señales de busqueda
        binding.btnEmitSignal.setOnClickListener {
            Toast.makeText(this, "Emitiendo señal sónica...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val emitter = MorseEmitter()
                emitter.sendSignal("SOS")
            }
        }
    }

    private fun observeViewModel() {
        // Observa si el tracking está activo o inactivo
        viewModel.isTrackingEnabled.observe(this) { isEnabled ->
            isUpdatingSwitch = true
            binding.switchTracking.isChecked = isEnabled
            isUpdatingSwitch = false

            if(isEnabled) {
                binding.switchTracking.text = "Rastreo Activo"
            } else {
                binding.switchTracking.text = "Activar Rastreo en Tiempo Real"
            }
        }

        //Observa si esta cargando
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                ProgressDialogFragment.show(supportFragmentManager)
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