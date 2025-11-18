package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.viewmodel.ContactsViewModel

class AddByNumberDialogFragment : DialogFragment() {

    // 1. Obtiene el ViewModel COMPARTIDO con la Activity
    private val viewModel: ContactsViewModel by activityViewModels {
        (requireActivity().application as MyApplication).contactsViewModelFactory
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        // 2. Infla el layout
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_add_by_number, null)

        val numberInput = view.findViewById<EditText>(R.id.input_number_dialog)

        // 3. Crea el diálogo
        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Añadir") { _, _ ->
                val number = numberInput.text.toString()
                if (number.isNotBlank()) {
                    // 4. Llama al ViewModel
                    viewModel.addTrustedContact(number)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}