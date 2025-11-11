package com.tecsup.aurora.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.FragmentDeviceBinding // <-- 1. Importa el ViewBinding de tu XML
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 2. Usamos ListAdapter para eficiencia (en lugar de un Adapter básico)
class DeviceAdapter : ListAdapter<DeviceResponse, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    // 3. Crea el ViewHolder (el contenedor de tu layout)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = FragmentDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    // 4. "Pinta" los datos en el layout
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

    // 5. El ViewHolder (la lógica de "pintar")
    inner class DeviceViewHolder(private val binding: FragmentDeviceBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DeviceResponse) {
            binding.deviceName.text = device.name

            val context = binding.root.context

            if (device.is_lost) {
                // --- Caso 1: Dispositivo Perdido ---
                binding.deviceInfo.text = "Perdido"
                binding.deviceInfo.setTextColor(
                    ContextCompat.getColor(context, R.color.rojo)
                )
            } else {
                // --- Caso 2: Dispositivo Seguro ---
                binding.deviceInfo.text = formatRelativeTime(device.last_seen)
                binding.deviceInfo.setTextColor(
                    ContextCompat.getColor(context, R.color.verde)
                )
            }
        }
    }

    // 6. Función que Convierte la fecha de la API en "hace 5 minutos" ejem
    private fun formatRelativeTime(apiTimestamp: String): String {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val date = format.parse(apiTimestamp) ?: return "hace mucho"

            val now = Date().time
            val diff = now - date.time

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "Ahora"
                minutes < 60 -> "hace $minutes min"
                hours < 24 -> "hace $hours h"
                days == 1L -> "Ayer"
                else -> "hace $days días"
            }
        } catch (e: Exception) {
            return "N/A"
        }
    }
}

// 7. DiffUtil: Ayuda a RecyclerView a animar los cambios en la lista
class DeviceDiffCallback : DiffUtil.ItemCallback<DeviceResponse>() {
    override fun areItemsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse): Boolean {
        return oldItem == newItem
    }
}