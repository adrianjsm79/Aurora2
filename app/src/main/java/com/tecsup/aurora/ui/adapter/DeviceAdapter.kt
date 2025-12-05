package com.tecsup.aurora.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
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

class DeviceAdapter(

    private val onDeviceAction: (DeviceResponse, DeviceAction) -> Unit

) : ListAdapter<DeviceResponse, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    // Constante para identificar la actualización "ligera" de solo tiempo
    companion object {
        const val PAYLOAD_UPDATE_TIME = "PAYLOAD_UPDATE_TIME"
    }

    // Definimos un enum para saber qué acción se eligió
    enum class DeviceAction {
        TOGGLE_LOST,
        DELETE,
        EDIT_NAME
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_UPDATE_TIME)) {
            holder.updateTimeOnly(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DeviceResponse) {
            binding.deviceName.text = device.name
            updateTimeOnly(device)

            binding.moreOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.inflate(R.menu.device_options_menu)

                val lostItem = popup.menu.findItem(R.id.action_toggle_lost)
                if (device.is_lost) {
                    lostItem.title = "Marcar como seguro"
                } else {
                    lostItem.title = "Marcar como perdido"
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_toggle_lost -> {
                            onDeviceAction(device, DeviceAction.TOGGLE_LOST)
                            true
                        }
                        R.id.action_edit_name -> {
                            onDeviceAction(device, DeviceAction.EDIT_NAME)
                            true
                        }
                        R.id.action_delete -> {
                            onDeviceAction(device, DeviceAction.DELETE)
                            true
                        }
                        else -> false
                    }
                }

                popup.show()
            }
        }

        fun updateTimeOnly(device: DeviceResponse) {
            val context = binding.root.context

            if (device.is_lost) {
                binding.deviceInfo.text = "Perdido"
                binding.deviceInfo.setTextColor(
                    ContextCompat.getColor(context, R.color.red_error)
                )
            } else {
                val secondsAgo = getSecondsAgo(device.last_seen)
                binding.deviceInfo.text = formatTime(secondsAgo)

                val colorRes = when {
                    secondsAgo > 86400 -> R.color.orange_warning // Más de 24h (Naranja)
                    else -> R.color.green_success
                }
                val color = ContextCompat.getColor(context, colorRes)
                binding.deviceInfo.text = formatTime(secondsAgo)
                binding.deviceInfo.setTextColor(color)
                binding.statusDot.setColorFilter(color)
            }
        }
    }

    // AYUDANTES DE TIEMPo

    private fun getSecondsAgo(apiTimestamp: String): Long {
        return try {
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
            seconds < 86400 -> "Hace ${seconds / 3600} h"
            else -> "Hace ${seconds / 86400} días"
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