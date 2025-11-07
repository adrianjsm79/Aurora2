package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tecsup.aurora.ui.adapter.DeviceAdapter
import com.tecsup.aurora.databinding.ActivityDevicesBinding
import com.tecsup.aurora.data.model.Device

class DevicesActivity : BaseActivity(), DeviceAdapter.DeviceInteractionListener {

    private lateinit var binding: ActivityDevicesBinding
    private lateinit var deviceAdapter: DeviceAdapter
    private val devicesList = mutableListOf<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        loadFakeData()
        setupRecyclerView()
        setupClickListeners()
        updateUI()
    }

    private fun loadFakeData() {
        // Ahora inicializamos el dispositivo con su estado de visibilidad
        devicesList.add(Device(1, "Teléfono Principal (Pixel 7)", "15/08/2025", isVisibleToContacts = true))
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(devicesList, this)
        binding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupClickListeners() {
        binding.toolbarDevices.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Gestión de Dispositivos")
                .setMessage("Aquí puedes ver los dispositivos vinculados a tu cuenta Aurora. Puedes tener un máximo de 3 dispositivos vinculados.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        binding.fabAddDevice.setOnClickListener {
            if (devicesList.size < 3) {
                val newDevice = Device(
                    id = (devicesList.maxOfOrNull { it.id } ?: 0) + 1,
                    name = "Nuevo Dispositivo #${devicesList.size + 1}",
                    linkedDate = "22/10/2025",
                    isVisibleToContacts = false // Por defecto, el nuevo es privado
                )
                devicesList.add(newDevice)
                updateUI()
                Toast.makeText(this, "Nuevo dispositivo vinculado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Has alcanzado el límite de 3 dispositivos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI() {
        deviceAdapter.updateDevices(devicesList.toList()) // Enviamos una copia de la lista
        binding.textEmptyList.visibility = if (devicesList.isEmpty()) View.VISIBLE else View.GONE
        binding.fabAddDevice.visibility = if (devicesList.size < 3) View.VISIBLE else View.GONE
    }

    // --- Implementación de los listeners ---

    override fun onLocateClicked(device: Device) {
        Toast.makeText(this, "Localizando ${device.name}...", Toast.LENGTH_SHORT).show()
    }

    // LÓGICA DEL DIÁLOGO DE VISIBILIDAD
    override fun onVisibilityClicked(device: Device, newVisibilityState: Boolean) {
        val title = if (newVisibilityState) "¿Activar Visibilidad?" else "¿Desactivar Visibilidad?"
        val message = if (newVisibilityState) {
            "Si activas la visibilidad, tus contactos de confianza podrán ver la ubicación de '${device.name}'."
        } else {
            "Si desactivas la visibilidad, tus contactos de confianza ya no podrán ver la ubicación de '${device.name}'."
        }
        val actionButtonText = if (newVisibilityState) "Activar" else "Desactivar"

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(actionButtonText) { _, _ ->
                // Actualizar el estado del dispositivo y la UI
                val index = devicesList.indexOf(device)
                if (index != -1) {
                    devicesList[index].isVisibleToContacts = newVisibilityState
                    updateUI() // Llama a notifyDataSetChanged() dentro del adapter
                    val feedback = if(newVisibilityState) "Visibilidad activada" else "Visibilidad desactivada"
                    Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onEditNameClicked(device: Device) {
        val editText = EditText(this).apply {
            setText(device.name)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Nombre")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val index = devicesList.indexOf(device)
                    if (index != -1) {
                        devicesList[index] = device.copy(name = newName)
                        updateUI()
                        Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    override fun onUnlinkClicked(device: Device) {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Desvincular Dispositivo?")
            .setMessage("Si desvinculas '${device.name}', se perderá toda la configuración. ¿Estás seguro?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desvincular") { _, _ ->
                devicesList.remove(device)
                updateUI()
                Toast.makeText(this, "${device.name} desvinculado", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
