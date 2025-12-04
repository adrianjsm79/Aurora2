package com.tecsup.aurora.ui.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tecsup.aurora.MyApplication
import com.tecsup.aurora.R
import com.tecsup.aurora.data.model.DeviceResponse
import com.tecsup.aurora.databinding.ActivitySearchMapBinding
import com.tecsup.aurora.databinding.ViewMapMarkerBinding
import com.tecsup.aurora.ui.adapter.MapDeviceAdapter
import com.tecsup.aurora.utils.DeviceHelper
import com.tecsup.aurora.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.tecsup.aurora.ui.fragments.DeviceInfoBottomSheet


class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivitySearchMapBinding
    private lateinit var map: GoogleMap

    // Mapas para rastrear objetos en el mapa y actualizarlos sin borrarlos
    private val markersMap = mutableMapOf<Int, Marker>()
    private val circlesMap = mutableMapOf<Int, Circle>()

    private var hasCenteredCamera = false // Bandera para centrar solo la primera vez

    private val viewModel: MapViewModel by viewModels {
        (application as MyApplication).mapViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón atrás
        binding.fabBack.setOnClickListener { finish() }

        // Configurar mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar BottomSheet
        setupBottomSheet()

        // Configurar Botón de Ubicación
        binding.btnToggleLocation.setOnClickListener {
            viewModel.toggleTracking()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = false // Ocultar botones zoom default
        map.uiSettings.isCompassEnabled = true

        // Observar datos del mapa
        viewModel.uiState.observe(this) { state ->
            updateMapObjects(state.devices, state.currentUserId)

            // Actualizar lista del bottom sheet
            (binding.mapDevicesRecycler.adapter as? MapDeviceAdapter)?.submitList(state.devices)
        }

        // Observar estado del rastreo (Botón flotante)
        viewModel.isTrackingActive.observe(this) { isEnabled ->
            updateLocationButton(isEnabled)
        }
    }

    private fun updateMapObjects(devices: List<DeviceResponse>, currentUserId: Int) {
        val currentDeviceID = DeviceHelper.getDeviceIdentifier(this)

        devices.forEach { device ->
            if (device.latitude != null && device.longitude != null) {
                val position = LatLng(device.latitude, device.longitude)

                // Lógica de colores corregida
                // Asegúrate que device.user venga como INT desde el backend
                val isMyDevice = device.user == currentUserId
                val isThisPhone = device.device_identifier == currentDeviceID

                // Colores: Rojo (Perdido) > Morado (Mío) > Verde (Contacto)
                val colorRes = when {
                    device.is_lost -> R.color.red_error
                    isMyDevice -> R.color.purple
                    else -> R.color.green_success
                }

                // 1. ACTUALIZAR MARCADOR
                if (markersMap.containsKey(device.id)) {
                    markersMap[device.id]?.position = position
                } else {
                    createCustomMarker(device, colorRes, isThisPhone) { bitmap ->
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(device.name)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .anchor(0.5f, 1f) // Ancla en la punta de abajo
                        )
                        marker?.tag = device
                        if (marker != null) markersMap[device.id] = marker
                    }
                }

                // 2. ACTUALIZAR CÍRCULO DE PRECISIÓN
                val radius = device.accuracy?.toDouble() ?: 0.0
                val circleColor = ContextCompat.getColor(this, colorRes)
                // Color de relleno con 20% de opacidad (0x33)
                val fillColor = Color.argb(0x33, Color.red(circleColor), Color.green(circleColor), Color.blue(circleColor))

                if (circlesMap.containsKey(device.id)) {
                    circlesMap[device.id]?.center = position
                    circlesMap[device.id]?.radius = radius
                } else {
                    val circle = map.addCircle(
                        CircleOptions()
                            .center(position)
                            .radius(radius)
                            .strokeWidth(2f)
                            .strokeColor(circleColor)
                            .fillColor(fillColor)
                    )
                    circlesMap[device.id] = circle
                }

                // 3. CENTRAR CÁMARA (Solo la primera vez y si es ESTE dispositivo)
                if (!hasCenteredCamera && isThisPhone) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                    hasCenteredCamera = true
                }
            }
        }

        // Listener click marcador
        map.setOnMarkerClickListener { marker ->
            val device = marker.tag as? DeviceResponse
            if (device != null) {
                // Mover cámara y mostrar info
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                // Podrías expandir el bottom sheet aquí o mostrar dialog
            }
            false
        }
    }

    private fun updateLocationButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.cardLocationStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green_success))
            binding.iconLocationStatus.setColorFilter(Color.WHITE)
            binding.iconLocationStatus.setImageResource(R.drawable.ic_location)
            binding.textLocationStatus.text = "Ubicación Activa"
            binding.textLocationStatus.setTextColor(Color.WHITE)
        } else {
            binding.cardLocationStatus.setCardBackgroundColor(Color.WHITE)
            binding.iconLocationStatus.setColorFilter(Color.BLACK)
            binding.iconLocationStatus.setImageResource(R.drawable.ic_location_off)
            binding.textLocationStatus.text = "Activar GPS"
            binding.textLocationStatus.setTextColor(Color.BLACK)
        }
    }

    private fun setupBottomSheet() {
        // Inicializamos el adaptador con DOS lambdas
        val adapter = MapDeviceAdapter(
            // Acción 1: Clic en la fila -> Mover Cámara
            onRowClick = { device ->
                if (device.latitude != null && device.longitude != null) {
                    val pos = LatLng(device.latitude, device.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                    // Opcional: Colapsar sheet parcialmente para ver mapa
                    BottomSheetBehavior.from(binding.bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    Toast.makeText(this, "Sin ubicación", Toast.LENGTH_SHORT).show()
                }
            },
            // Acción 2: Clic en el Chevron -> Mostrar Detalles
            onInfoClick = { device ->
                showDeviceDetailsBottomSheet(device)
            }
        )

        binding.mapDevicesRecycler.layoutManager = LinearLayoutManager(this)
        binding.mapDevicesRecycler.adapter = adapter
    }

    private fun showDeviceDetailsBottomSheet(device: DeviceResponse) {
        val sheet = DeviceInfoBottomSheet(
            deviceName = device.name,
            deviceId = device.device_identifier,
            ownerEmail = device.user_email, // Asegúrate de que el modelo tenga este campo
            lastSeen = device.last_seen,
            accuracy = device.accuracy?.toDouble() // Pasamos la precisión
        )
        sheet.show(supportFragmentManager, "DeviceInfoSheet")
    }

    private fun createCustomMarker(
        device: DeviceResponse,
        colorRes: Int,
        isThisPhone: Boolean,
        onReady: (Bitmap) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val markerBinding = ViewMapMarkerBinding.inflate(LayoutInflater.from(this@SearchMapActivity))

            // 1. Configurar color del borde
            val color = ContextCompat.getColor(this@SearchMapActivity, colorRes)
            markerBinding.markerBorder.background.setTint(color)
            markerBinding.markerArrow.setColorFilter(color)

            // 2. Borde azul para "Este Dispositivo" (usando FrameLayout padding hack)
            if (isThisPhone) {
                val blueColor = ContextCompat.getColor(this@SearchMapActivity, R.color.blue)
                markerBinding.markerContainer.background.setTint(blueColor)
                markerBinding.markerContainer.setPadding(8, 8, 8, 8)
            } else {
                markerBinding.markerContainer.background.setTintList(null) // Blanco default
                markerBinding.markerContainer.setPadding(2, 2, 2, 2)
            }

            // 3. Imagen (Simplificado por brevedad, igual que antes con Coil)
            if (!device.photoUrl.isNullOrEmpty()) {
                // Cargar coil...
                markerBinding.markerImage.setImageResource(R.drawable.ic_phone)
                markerBinding.markerImage.setColorFilter(color)
            } else {
                markerBinding.markerImage.setImageResource(R.drawable.ic_phone)
                markerBinding.markerImage.setColorFilter(color)
            }

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
}