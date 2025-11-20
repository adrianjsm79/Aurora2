package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.tecsup.aurora.R

class ProgressDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout que creamos
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Haz que el diálogo no se pueda cancelar con el botón de atrás
        isCancelable = false
        // Haz que el fondo del diálogo sea transparente para que solo se vea el ProgressBar
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    companion object {
        private const val TAG = "ProgressDialog"

        // Función para MOSTRAR el diálogo
        fun show(fragmentManager: FragmentManager) {
            // Evita mostrar múltiples diálogos si ya hay uno visible
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                ProgressDialogFragment().show(fragmentManager, TAG)
            }
        }

        // Función para OCULTAR el diálogo
        fun hide(fragmentManager: FragmentManager) {
            (fragmentManager.findFragmentByTag(TAG) as? DialogFragment)?.dismissAllowingStateLoss()
        }
    }
}