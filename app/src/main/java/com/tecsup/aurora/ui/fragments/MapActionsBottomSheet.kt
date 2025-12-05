package com.tecsup.aurora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.LayoutMapContextMenuBinding // Necesitas crear este layout con tu menu
import com.tecsup.aurora.data.model.DeviceResponse

class MapActionsBottomSheet(
    private val device: DeviceResponse,
    private val onAction: (Int) -> Unit // Devuelve el ID de la acción seleccionada
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //Inflamos un NavigationView que usa el XML de menú
        val navigationView = com.google.android.material.navigation.NavigationView(requireContext())
        navigationView.inflateMenu(R.menu.map_context_menu) // Tu XML de menú

        val menu = navigationView.menu
        if (device.is_lost) {
            menu.findItem(R.id.map_device_lost).isVisible = false
            menu.findItem(R.id.map_device_found).isVisible = true
        } else {
            menu.findItem(R.id.map_device_lost).isVisible = true
            menu.findItem(R.id.map_device_found).isVisible = false
        }

        navigationView.setNavigationItemSelectedListener { item ->
            onAction(item.itemId)
            dismiss()
            true
        }
        return navigationView
    }
}