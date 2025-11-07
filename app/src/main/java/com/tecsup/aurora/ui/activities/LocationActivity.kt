package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.tecsup.aurora.databinding.ActivityLocationBinding

class LocationActivity : BaseActivity() {

    private lateinit var binding: ActivityLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.toolbarLocation.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnInfo.setOnClickListener {
            Toast.makeText(this, "Información sobre los modos de localización", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenMap.setOnClickListener {
            val intent = Intent(this, SearchmapActivity::class.java)
            startActivity(intent)
        }
    }
}
