package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tecsup.aurora.R

class HelpDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setView(R.layout.fragment_help_contacts)
            .setPositiveButton("Entendido", null)
            .create()
    }
}