package com.tecsup.aurora.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ItemDeviceBinding
import com.tecsup.aurora.data.model.Device

class DeviceAdapter(
    private var devices: List<Device>,
    private val listener: DeviceInteractionListener
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var expandedPosition = -1

    interface DeviceInteractionListener {
        fun onLocateClicked(device: Device)
        // Pasamos el nuevo estado deseado al listener
        fun onVisibilityClicked(device: Device, newVisibilityState: Boolean)
        fun onEditNameClicked(device: Device)
        fun onUnlinkClicked(device: Device)
    }

    inner class DeviceViewHolder(val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousExpandedPosition = expandedPosition
                    expandedPosition = if (expandedPosition == position) -1 else position
                    notifyItemChanged(previousExpandedPosition)
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val isExpanded = position == expandedPosition

        with(holder.binding) {
            textDeviceName.text = device.name
            textDeviceStatus.text = "Vinculado el ${device.linkedDate}"

            expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            iconExpandArrow.setImageResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)

            optionUnlink.visibility = if (devices.size > 1) View.VISIBLE else View.GONE
            // --- INICIO DE LA NUEVA LÓGICA DE VISIBILIDAD ---

            val context = holder.itemView.context
            if (device.isVisibleToContacts) {
                // Estado: VISIBLE (la opción será para desactivar)
                optionVisibility.text = "Desactivar visibilidad para contactos"
                val visibilityOnIcon = ContextCompat.getDrawable(context, R.drawable.ic_visibility)
                optionVisibility.setCompoundDrawablesWithIntrinsicBounds(visibilityOnIcon, null, null, null)
            } else {
                // Estado: OCULTO (la opción será para activar)
                optionVisibility.text = "Activar visibilidad para contactos"
                val visibilityOffIcon = ContextCompat.getDrawable(context, R.drawable.ic_visibility_off)
                optionVisibility.setCompoundDrawablesWithIntrinsicBounds(visibilityOffIcon, null, null, null)
            }

            // --- FIN DE LA NUEVA LÓGICA ---

            // Configurar listeners para las opciones
            optionLocate.setOnClickListener { listener.onLocateClicked(device) }
            // Al hacer clic, pasamos el estado opuesto al actual
            optionVisibility.setOnClickListener { listener.onVisibilityClicked(device, !device.isVisibleToContacts) }
            optionEditName.setOnClickListener { listener.onEditNameClicked(device) }
            optionUnlink.setOnClickListener { listener.onUnlinkClicked(device) }
        }
    }

    override fun getItemCount() = devices.size

    fun updateDevices(newDevices: List<Device>) {
        this.devices = newDevices
        expandedPosition = -1
        notifyDataSetChanged()
    }
}
