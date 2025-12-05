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
        isCancelable = false

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    companion object {
        private const val TAG = "ProgressDialog"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                ProgressDialogFragment().show(fragmentManager, TAG)
            }
        }

        fun hide(fragmentManager: FragmentManager) {
            (fragmentManager.findFragmentByTag(TAG) as? DialogFragment)?.dismissAllowingStateLoss()
        }
    }
}