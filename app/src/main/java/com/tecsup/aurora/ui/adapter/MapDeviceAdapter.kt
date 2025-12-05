package com.tecsup.aurora.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.ItemMapDeviceBinding
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MapDeviceAdapter(
    private val onRowClick: (DeviceResponse) -> Unit, // Acción: Mover cámara
    private val onInfoClick: (DeviceResponse) -> Unit // Acción: Abrir BottomSheet
) : ListAdapter<DeviceResponse, MapDeviceAdapter.ViewHolder>(MapDeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMapDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMapDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DeviceResponse) {
            binding.deviceName.text = device.name
            val context = binding.root.context

            val secondsAgo = getSecondsAgo(device.last_seen)
            if (device.is_lost) {
                binding.deviceStatus.text = "PERDIDO"
                binding.deviceStatus.setTextColor(ContextCompat.getColor(context, R.color.red_error))
                binding.statusDot.background.setTint(ContextCompat.getColor(context, R.color.red_error))
            } else {
                binding.deviceStatus.text = formatTime(secondsAgo)
                val colorRes = when {
                    secondsAgo > 86400 -> R.color.orange_warning
                    else -> R.color.green_success
                }
                binding.deviceStatus.setTextColor(ContextCompat.getColor(context, android.R.color.tab_indicator_text))
                binding.statusDot.background.setTint(ContextCompat.getColor(context, colorRes))
            }

            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRowClick(getItem(adapterPosition))
                }
            }

            binding.infoDevice.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onInfoClick(getItem(adapterPosition))
                }
            }
        }
    }

    private fun getSecondsAgo(apiTimestamp: String): Long {
        return try {
            val time = try {
                OffsetDateTime.parse(apiTimestamp, DateTimeFormatter.ISO_DATE_TIME).toInstant()
            } catch (e: Exception) {
                Instant.parse(apiTimestamp)
            }
            val now = Instant.now()
            Duration.between(time, now).seconds
        } catch (e: Exception) { -1L }
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

class MapDeviceDiffCallback : DiffUtil.ItemCallback<DeviceResponse>() {
    override fun areItemsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DeviceResponse, newItem: DeviceResponse) = oldItem == newItem
}