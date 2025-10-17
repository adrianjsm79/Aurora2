package com.tecsup.aurora.activities

import android.content.Intent
import android.os.Bundle
import com.tecsup.aurora.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Usamos la función de la clase base y le pasamos la vista raíz
        setupEdgeToEdge(binding.main)

        // Listener para el botón de login usando View Binding
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
