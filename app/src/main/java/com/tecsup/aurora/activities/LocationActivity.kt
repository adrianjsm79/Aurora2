package com.tecsup.aurora.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tecsup.aurora.R
import com.tecsup.aurora.databinding.ActivityLocationBinding

class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupClickListeners()
    }

    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
