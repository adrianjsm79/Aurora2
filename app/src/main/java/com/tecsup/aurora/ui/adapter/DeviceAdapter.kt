package com.tecsup.aurora.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.ItemDeviceBinding
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DeviceAdapter : ListAdapter<DeviceResponse, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    // Constante para identificar la actualización "ligera" de solo tiempo
    companion object {
        const val PAYLOAD_UPDATE_TIME = "PAYLOAD_UPDATE_TIME"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    // --- Bind NORMAL (Carga completa inicial o cuando cambian datos grandes) ---
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

    // --- Bind PARCIAL (Optimizado para el cronómetro) ---
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_UPDATE_TIME)) {
            // Si recibimos la señal de tiempo, SOLO actualizamos texto y color
            // Esto es muy rápido y no causa parpadeos
            holder.updateTimeOnly(getItem(position))
        } else {
            // Si no es una actualización parcial, hacemos la carga completa
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {

        // Carga completa (Nombre + Estado)
        fun bind(device: DeviceResponse) {
            binding.deviceName.text = device.name
            updateTimeOnly(device)
        }

        // Carga ligera (Solo Estado: Texto y Color)
        fun updateTimeOnly(device: DeviceResponse) {
            val context = binding.root.context

            if (device.is_lost) {
                // Prioridad 1: Si está perdido, siempre rojo
                binding.deviceInfo.text = "Perdido"
                binding.deviceInfo.setTextColor(
                    ContextCompat.getColor(context, R.color.red_error)
                )
            } else {
                // Calcular tiempo relativo
                val secondsAgo = getSecondsAgo(device.last_seen)
                binding.deviceInfo.text = formatTime(secondsAgo)

                // Prioridad 2 y 3: Colores según antigüedad
                val colorRes = when {
                    secondsAgo > 86400 -> R.color.orange_warning // Más de 24h (Naranja)
                    else -> R.color.green_success // Reciente (Verde)
                }
                binding.deviceInfo.setTextColor(
                    ContextCompat.getColor(context, colorRes)
                )
            }
        }
    }

    // --- AYUDANTES DE TIEMPO ---

    /**
     * Calcula cuántos segundos han pasado desde la fecha del servidor.
     * Robusto para diferentes formatos ISO de Django.
     */
    private fun getSecondsAgo(apiTimestamp: String): Long {
        return try {
            // Intentamos parsear con Offset (ej. 2025-11-20T10:00:00+00:00)
            // Si falla, intentamos como Instant directo (ej. 2025-11-20T10:00:00Z)
            val time = try {
                OffsetDateTime.parse(apiTimestamp, DateTimeFormatter.ISO_DATE_TIME).toInstant()
            } catch (e: Exception) {
                Instant.parse(apiTimestamp)
            }

            val now = Instant.now()
            Duration.between(time, now).seconds
        } catch (e: Exception) {
            Log.e("DeviceAdapter", "Error parseando fecha: $apiTimestamp", e)
            -1L // Retorna -1 si hay error
        }
    }

    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 0 -> "Desconocido"
            seconds < 60 -> "Hace un momento"
            seconds < 3600 -> "Hace ${seconds / 60} min"
            seconds < 86400 -> "Hace ${seconds / 3600} h" // Menos de 24 horas
            else -> "Hace ${seconds / 86400} días" // Más de 1 día
        }
    }
}

class DeviceDiffCallback : DiffUtil.ItemCallback<DeviceResponse>() {
    override fun areItemsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse): Boolean {
        return oldItem == newItem
    }
}