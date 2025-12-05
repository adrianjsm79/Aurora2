package com.tecsup.aurora.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tecsup.aurora.databinding.ActivitySupportBinding

class SupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.cardEmail.setOnClickListener {
            sendEmail(
                subject = "Consulta sobre Aurora App",
                body = "Hola, quisiera consultar lo siguiente..."
            )
        }

        binding.cardWhatsapp.setOnClickListener {
            openWhatsApp()
        }

        binding.btnSendReport.setOnClickListener {
            val description = binding.inputReport.text.toString().trim()

            if (description.isEmpty()) {
                binding.inputReport.error = "Por favor describe el problema"
                return@setOnClickListener
            }

            val body = buildReportBody(description)
            sendEmail(
                subject = "Reporte de Error - Aurora Android",
                body = body
            )
        }

        binding.btnFaq.setOnClickListener {
            val url = "https://auroraweb-topaz.vercel.app/terms/faq.html" // Tu sección de FAQ
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildReportBody(userDescription: String): String {
        val sb = StringBuilder()
        sb.append("Descripción del problema:\n")
        sb.append(userDescription)
        sb.append("\n\n--------------------------------\n")

        if (binding.checkDeviceInfo.isChecked) {
            sb.append("Información Técnica (Generada automáticamente):\n")
            sb.append("• Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            sb.append("• Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            sb.append("• App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}\n")
        }

        return sb.toString()
    }

    private fun sendEmail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Solo apps de correo
            putExtra(Intent.EXTRA_EMAIL, arrayOf("adrian.silvasantiste@tecsup.edu.pe"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo con..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró ninguna aplicación de correo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsApp() {
        val phoneNumber = "51947179270"
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }
}