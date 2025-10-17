package com.tecsup.aurora.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.FragmentDeviceItemBinding

class DeviceItemFragment : Fragment() {

    private var _binding: FragmentDeviceItemBinding? = null
    private val binding get() = _binding!!

    private var deviceName: String? = null
    private var isDeviceActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceName = it.getString(ARG_DEVICE_NAME)
            isDeviceActive = it.getBoolean(ARG_IS_ACTIVE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asignar el nombre del dispositivo
        binding.deviceName.text = deviceName ?: "Dispositivo Desconocido"

        // Configurar el estado (activo/inactivo)
        if (isDeviceActive) {
            binding.deviceStatus.text = "Activo"
            binding.deviceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.turquesa))
            binding.deviceStatus.setBackgroundResource(R.drawable.status_active_background)
        } else {
            binding.deviceStatus.text = "Inactivo"
            binding.deviceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.background_primary))
            binding.deviceStatus.setBackgroundResource(R.drawable.status_inactive_background)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DEVICE_NAME = "device_name"
        private const val ARG_IS_ACTIVE = "is_active"

        // Función para crear instancias del fragment de forma segura
        fun newInstance(name: String, isActive: Boolean): DeviceItemFragment {
            val fragment = DeviceItemFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_NAME, name)
            args.putBoolean(ARG_IS_ACTIVE, isActive)
            fragment.arguments = args
            return fragment
        }
    }
}