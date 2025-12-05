package com.tecsup.aurora.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tecsup.aurora.R
import com.tecsup.aurora.ui.adapter.DeviceAdapter
import com.tecsup.aurora.databinding.ActivityDevicesBinding

class DevicesActivity : BaseActivity() {

//NO USAMOS ESTA ACTIVITY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)
        setupSystemBars()
    }
}
