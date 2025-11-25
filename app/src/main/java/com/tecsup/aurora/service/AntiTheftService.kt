package com.tecsup.aurora.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.ui.activities.FakeShutdownActivity

class AntiTheftService : AccessibilityService() {

    private lateinit var settingsRepository: SettingsRepository
    private var lastEventTime = 0L

    // Detectamos la marca del dispositivo al iniciar
    private val manufacturer = Build.MANUFACTURER.lowercase()
    private val isSamsung = manufacturer.contains("samsung")
    private val isXiaomi = manufacturer.contains("xiaomi")

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = SettingsRepository(applicationContext)
        Log.d("ANTI_THEFT", "Servicio iniciado en dispositivo: $manufacturer")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. VERIFICACIÓN MAESTRA
        val shouldIntercept = settingsRepository.isFakeShutdownEnabled() || settingsRepository.isDeviceLost()
        if (!shouldIntercept) return

        // 2. ANTI-REBOTE (Evitar disparos múltiples en milisegundos)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime < 400) return

        // 3. SÓLO ESCUCHAMOS CAMBIOS DE ESTADO DE VENTANA
        // (Ignoramos CONTENT_CHANGED para evitar bugs con notificaciones)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val packageName = event.packageName?.toString()?.lowercase() ?: ""
            val className = event.className?.toString()?.lowercase() ?: ""
            // Combinamos texto y descripción para mayor precisión
            val contentText = (event.text.toString() + (event.contentDescription ?: "")).lowercase()

            // --- FILTROS DE SEGURIDAD (WHITELIST) ---
            // Ignoramos nuestra propia app, launchers y notificaciones
            if (packageName.contains("com.tecsup.aurora") ||
                packageName.contains("launcher") ||
                className.contains("statusbar") ||
                className.contains("notification")) {
                return
            }

            // 4. HEURÍSTICA DE DETECCIÓN (El "Cerebro")
            var isPowerMenu = false

            // A. Detección por Nombre Técnico (Funciona en casi todos)
            if (className.contains("globalactions") || // Android Puro / Samsung
                className.contains("powerdialog") ||   // Xiaomi / Huawei
                className.contains("shutdown") ||
                (isSamsung && className.contains("dialoglite"))) { // Samsung OneUI específico
                isPowerMenu = true
            }

            // B. Detección por Texto (Respaldo si el nombre técnico falla)
            // Solo si viene del sistema o configuraciones
            else if (packageName == "android" || packageName.contains("systemui") || packageName.contains("settings")) {
                if (contentText.contains("apagar") ||
                    contentText.contains("power off") ||
                    contentText.contains("reiniciar") ||
                    contentText.contains("restart")) {

                    // Asegurarnos de que no sea un mensaje Toast
                    if (!className.contains("toast")) {
                        isPowerMenu = true
                    }
                }
            }

            // 5. ACCIÓN DE BLOQUEO
            if (isPowerMenu) {
                Log.w("ANTI_THEFT", "¡Menú de apagado detectado ($manufacturer)! Bloqueando...")
                lastEventTime = currentTime

                // A. CERRAR EL MENÚ REAL (Estrategia de "Fuerza Bruta")
                // Ejecutamos TODOS los métodos de cierre para asegurar compatibilidad

                // 1. Intent estándar para cerrar diálogos (Muy efectivo en Samsung/Android < 12)
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

                // 2. Botón Home (Efectivo en Android Puro)
                performGlobalAction(GLOBAL_ACTION_HOME)

                // 3. Botón Atrás (Efectivo en Xiaomi/Overlays)
                performGlobalAction(GLOBAL_ACTION_BACK)

                // B. LANZAR PANTALLA FALSA
                val intent = Intent(this, FakeShutdownActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // El servicio fue interrumpido
    }
}