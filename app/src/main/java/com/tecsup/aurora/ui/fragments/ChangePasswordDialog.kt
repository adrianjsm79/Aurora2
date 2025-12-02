package com.tecsup.aurora.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tecsup.aurora.databinding.DialogChangePasswordBinding

class ChangePasswordDialog(
    private val onPasswordConfirmed: (String) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogChangePasswordBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChangePasswordBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        // Fondo transparente para que se vean las esquinas redondeadas del layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        binding.btnCancelPassword.setOnClickListener {
            dismiss()
        }

        binding.btnSavePassword.setOnClickListener {
            val pass1 = binding.inputNewPassword.text.toString()
            val pass2 = binding.inputConfirmNewPassword.text.toString()

            if (pass1.length < 8) {
                binding.inputNewPassword.error = "Mínimo 8 caracteres"
                return@setOnClickListener
            }

            if (pass1 != pass2) {
                binding.inputConfirmNewPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // Si todo está bien, enviamos la contraseña a la Activity
            onPasswordConfirmed(pass1)
            dismiss()
        }

        return dialog
    }
}