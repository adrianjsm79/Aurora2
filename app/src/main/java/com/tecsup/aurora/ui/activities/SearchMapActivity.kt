package com.tecsup.aurora.ui.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.ActivitySearchMapBinding
import com.tecsup.aurora.databinding.ViewMapMarkerBinding // Binding del layout personalizado
import com.tecsup.aurora.utils.DeviceHelper
import com.tecsup.aurora.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.card.MaterialCardView

class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivitySearchMapBinding
    private lateinit var map: GoogleMap
    private val markersMap = mutableMapOf<Int, Marker>() // Mapa para rastrear marcadores por ID

    private val viewModel: MapViewModel by viewModels {
        (application as MyApplication).mapViewModelFactory // Asegúrate de añadir esto a MyApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Inicializar mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        // Observar datos
        viewModel.uiState.observe(this) { state ->
            updateMarkers(state.devices, state.currentUserId)
        }
    }

    private fun updateMarkers(devices: List<DeviceResponse>, currentUserId: Int) {
        val currentDeviceID = DeviceHelper.getDeviceIdentifier(this) // ID hardware de este cel

        devices.forEach { device ->
            if (device.latitude != null && device.longitude != null) {
                val position = LatLng(device.latitude, device.longitude)

                // Determinar color y estilo
                val isMyDevice = device.user == currentUserId // Nota: DeviceResponse necesita campo user (id)
                val isThisPhone = device.device_identifier == currentDeviceID

                val colorRes = when {
                    device.is_lost -> R.color.red_error
                    isMyDevice -> R.color.purple
                    else -> R.color.green_success
                }

                // Si el marcador ya existe, solo lo movemos (animación suave)
                if (markersMap.containsKey(device.id)) {
                    markersMap[device.id]?.position = position
                } else {
                    // Si es nuevo, creamos el icono personalizado
                    createCustomMarker(device, colorRes, isThisPhone) { bitmap ->
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(device.name)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        )
                        marker?.tag = device // Guardamos el objeto en el tag para el click
                        if (marker != null) markersMap[device.id] = marker
                    }
                }
            }
        }

        // Listener de Click en Marcador
        map.setOnMarkerClickListener { marker ->
            val device = marker.tag as? DeviceResponse
            if (device != null) {
                showDeviceDetailsDialog(device)
            }
            false
        }
    }

    /**
     * Convierte el Layout XML a un Bitmap para Google Maps
     * Carga la imagen de perfil si existe.
     */
    private fun createCustomMarker(
        device: DeviceResponse,
        colorRes: Int,
        isThisPhone: Boolean,
        onReady: (Bitmap) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val markerBinding = ViewMapMarkerBinding.inflate(LayoutInflater.from(this@SearchMapActivity))

            // 1. Configurar Colores
            val color = ContextCompat.getColor(this@SearchMapActivity, colorRes)
            markerBinding.markerBorder.background.setTint(color)
            markerBinding.markerArrow.setColorFilter(color)

            // 2. Diferenciador si es "Este Dispositivo" (Borde azul extra)
            if (isThisPhone) {
                // Hacemos un 'cast' para acceder a las propiedades de MaterialCardView
                val card = markerBinding.markerCard as MaterialCardView
                card.strokeWidth = 8 // Un borde más grueso para que se note
                card.strokeColor = ContextCompat.getColor(this@SearchMapActivity, R.color.blue)
            }

            // 3. Cargar Imagen (Coil) de forma síncrona dentro de la corrutina
            // Nota: DeviceResponse debe tener un campo photoUrl: String?
            if (!device.photoUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(this@SearchMapActivity)
                    .data(device.photoUrl)
                    .build()

                // Usamos execute para esperar el resultado de forma síncrona en la corrutina
                val drawable = imageLoader.execute(request).drawable
                if (drawable != null) {
                    markerBinding.markerImage.setImageDrawable(drawable)
                } else {
                    // Fallback si la imagen no se puede cargar
                    markerBinding.markerImage.setImageResource(R.drawable.ic_phone)
                    markerBinding.markerImage.setColorFilter(color)
                }
            } else {
                // Usar ícono por defecto si no hay URL
                markerBinding.markerImage.setImageResource(R.drawable.ic_phone)
                markerBinding.markerImage.setColorFilter(color)
            }

            // 4. Convertir a Bitmap y llamar al callback UNA SOLA VEZ
            onReady(getBitmapFromView(markerBinding.root))
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun showDeviceDetailsDialog(device: DeviceResponse) {
        // Mostrar un BottomSheetDialog o AlertDialog con info
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(device.name)
            .setMessage("ID: ${device.device_identifier}\nÚltima vez: ${device.last_seen}")
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
