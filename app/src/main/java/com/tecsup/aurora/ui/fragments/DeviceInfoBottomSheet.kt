package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tecsup.aurora.databinding.BottomSheetDeviceInfoBinding
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DeviceInfoBottomSheet(
    private val deviceName: String,
    private val deviceId: String,
    private val ownerEmail: String?, // O nombre si lo tienes disponible
    private val lastSeen: String,
    private val accuracy: Double?
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetDeviceInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetDeviceInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Rellenar datos
        binding.sheetDeviceName.text = deviceName
        binding.sheetDeviceId.text = "ID: ${deviceId.take(8)}..." // ID cortito

        // Propietario
        binding.sheetOwnerName.text = ownerEmail ?: "Desconocido"

        // Tiempo relativo
        binding.sheetLastSeen.text = formatRelativeTime(lastSeen)

        // Precisión
        val acc = accuracy ?: 0.0
        val quality = when {
            acc == 0.0 -> "Desconocida"
            acc < 20 -> "Excelente"
            acc < 50 -> "Buena"
            else -> "Baja"
        }
        binding.sheetAccuracy.text = "$quality (${String.format("%.1f", acc)} m)"

        // Botón cerrar
        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }
    }

    // Misma función de tiempo que en el Adapter (para consistencia)
    private fun formatRelativeTime(apiTimestamp: String): String {
        return try {
            val time = try {
                OffsetDateTime.parse(apiTimestamp, DateTimeFormatter.ISO_DATE_TIME).toInstant()
            } catch (e: Exception) {
                Instant.parse(apiTimestamp)
            }
            val now = Instant.now()
            val duration = Duration.between(time, now)

            when {
                duration.seconds < 60 -> "Hace un momento"
                duration.seconds < 3600 -> "Hace ${duration.toMinutes()} min"
                duration.toHours() < 24 -> "Hace ${duration.toHours()} h"
                else -> "Hace ${duration.toDays()} días"
            }
        } catch (e: Exception) {
            "Desconocido"
        }
    }
}