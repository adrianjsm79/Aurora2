package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupVersionInfo()
        setupLinks()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupVersionInfo() {
        try {
            // Obtiene la versión dinámicamente desde el build.gradle
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName

            binding.textVersion.text = "Versión $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.textVersion.text = "Versión Desconocida"
        }
    }

    private fun setupLinks() {
        // 1. Sitio Web Principal
        binding.btnWebsite.setOnClickListener {
            openUrl("https://auroraweb-topaz.vercel.app/")
        }

        // 2. Política de Privacidad (Apunta a tu HTML)
        binding.btnPrivacy.setOnClickListener {
            openUrl("https://auroraweb-topaz.vercel.app/terms/politicas.html")
            // O la ruta donde pusiste el HTML que te di antes
        }

        // 3. Términos y Condiciones
        binding.btnTerms.setOnClickListener {
            openUrl("https://auroraweb-topaz.vercel.app/terms/terminos.html")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la URL", Toast.LENGTH_SHORT).show()
        }
    }
}
