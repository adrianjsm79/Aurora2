package com.tecsup.aurora.ui.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
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
import com.tecsup.aurora.ui.fragments.DeviceInfoBottomSheet
import com.tecsup.aurora.ui.fragments.MapActionsBottomSheet
import com.tecsup.aurora.utils.DeviceHelper
import com.tecsup.aurora.viewmodel.MapOverlays
import com.tecsup.aurora.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var binding: ActivitySearchMapBinding
    private lateinit var map: GoogleMap

    // Mapas para rastrear objetos en el mapa y actualizarlos sin borrarlos
    private val markersMap = mutableMapOf<Int, Marker>()
    private val circlesMap = mutableMapOf<Int, Circle>()

    // Lista para limpiar las líneas (rutas) cuando cambian
    private val activePolylines = mutableListOf<Polyline>()

    private var hasCenteredCamera = false

    private val viewModel: MapViewModel by viewModels {
        (application as MyApplication).mapViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabBack.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupBottomSheet()

        binding.btnToggleLocation.setOnClickListener {
            viewModel.toggleTracking()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = true

        // Listener para Menú Contextual (Long Press)
        map.setOnMapLongClickListener(this)

        // Observar Dispositivos
        viewModel.uiState.observe(this) { state ->
            updateMapObjects(state.devices, state.currentUserId)
            (binding.mapDevicesRecycler.adapter as? MapDeviceAdapter)?.submitList(state.devices)
        }

        // AQUÍ SE DIBUJA LA RUTA DE GOOGLE
        viewModel.mapOverlays.observe(this) { overlays ->
            drawMapOverlays(overlays)
        }

        viewModel.isTrackingActive.observe(this) { isEnabled ->
            updateLocationButton(isEnabled)
        }

        map.setOnMarkerClickListener { marker ->
            val device = marker.tag as? DeviceResponse
            if (device != null) {
                showDeviceDetailsBottomSheet(device)
            }
            true
        }
    }

    // ESTO MANEJA LA RESPUESTA DE GOOGLE
    private fun drawMapOverlays(overlays: MapOverlays) {
        activePolylines.forEach { it.remove() }
        activePolylines.clear()

        //Dibujar Rastros
        overlays.traces.forEach { (_, points) ->
            if (points.size > 1) {
                val line = map.addPolyline(
                    PolylineOptions()
                        .addAll(points)
                        .color(ContextCompat.getColor(this, R.color.orange_warning))
                        .width(12f)
                        .pattern(listOf(Dash(20f), Gap(15f))) // Efecto punteado
                        .geodesic(true)
                        .zIndex(1f) // Por debajo de la ruta principal
                )
                activePolylines.add(line)
            }
        }

        // Dibujar Ruta de Navegación (Google Directions
        if (overlays.routePoints.isNotEmpty()) {
            val line = map.addPolyline(
                PolylineOptions()
                    .addAll(overlays.routePoints) // Aquí vienen los puntos de la calle
                    .color(ContextCompat.getColor(this, R.color.blue))
                    .width(18f)
                    .geodesic(true)
                    .zIndex(2f)
            )
            activePolylines.add(line)

            //Hacer zoom para ver toda la ruta
            val builder = LatLngBounds.Builder()
            overlays.routePoints.forEach { builder.include(it) }
            try { map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100)) } catch(e:Exception){}
        }
    }

    //Menú Contextual
    override fun onMapLongClick(clickCoords: LatLng) {
        val projection = map.projection
        val clickPoint = projection.toScreenLocation(clickCoords)

        for ((_, marker) in markersMap) {
            val markerPosition = marker.position
            val markerPoint = projection.toScreenLocation(markerPosition)

            val distance = sqrt(
                (clickPoint.x - markerPoint.x).toDouble().pow(2.0) +
                        (clickPoint.y - markerPoint.y).toDouble().pow(2.0)
            )

            if (distance < 120) { // Radio de toque (en píxeles)
                val device = marker.tag as? DeviceResponse
                if (device != null) {
                    showMapActionsMenu(device)
                    return
                }
            }
        }
    }

    private fun showMapActionsMenu(device: DeviceResponse) {
        //vibración suave
        binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

        val menu = MapActionsBottomSheet(device) { actionId ->
            when (actionId) {
                R.id.map_routing -> {
                    Toast.makeText(this, "Calculando ruta por calles...", Toast.LENGTH_SHORT).show()
                    viewModel.setRouteTarget(device.id)
                }
                R.id.map_follow -> {
                    Toast.makeText(this, "Rastro activado", Toast.LENGTH_SHORT).show()
                    viewModel.toggleTrace(device.id)
                }
                R.id.map_clean_follow -> {
                    viewModel.clearTrace(device.id)
                    Toast.makeText(this, "Rastro borrado", Toast.LENGTH_SHORT).show()
                }
                R.id.map_device_lost -> {
                    viewModel.markAsLost(device.id, true)
                    Toast.makeText(this, "¡COMANDO DE EMERGENCIA ENVIADO!", Toast.LENGTH_LONG).show()
                }
                R.id.map_device_found -> {
                    viewModel.markAsLost(device.id, false)
                    Toast.makeText(this, "Marcado como seguro", Toast.LENGTH_SHORT).show()
                }
            }
        }
        menu.show(supportFragmentManager, "MapMenu")
    }

    //El resto de métodos de actualización

    private fun updateMapObjects(devices: List<DeviceResponse>, currentUserId: Int) {
        val currentDeviceID = DeviceHelper.getDeviceIdentifier(this)

        devices.forEach { device ->
            if (device.latitude != null && device.longitude != null) {
                val position = LatLng(device.latitude, device.longitude)
                val isMyDevice = device.user == currentUserId
                val isThisPhone = device.device_identifier == currentDeviceID

                val colorRes = when {
                    device.is_lost -> R.color.red_error
                    isMyDevice -> R.color.purple
                    else -> R.color.green_success
                }

                if (markersMap.containsKey(device.id)) {
                    markersMap[device.id]?.position = position
                } else {
                    createCustomMarker(device, colorRes, isThisPhone) { bitmap ->
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(device.name)
                                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                .anchor(0.5f, 1f)
                        )
                        marker?.tag = device
                        if (marker != null) markersMap[device.id] = marker
                    }
                }

                // Círculos de precisión
                val radius = device.accuracy?.toDouble() ?: 0.0
                val circleColor = ContextCompat.getColor(this, colorRes)
                val fillColor = Color.argb(0x22, Color.red(circleColor), Color.green(circleColor), Color.blue(circleColor))

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

                if (!hasCenteredCamera && isThisPhone) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                    hasCenteredCamera = true
                }
            }
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
        val adapter = MapDeviceAdapter(
            onRowClick = { device ->
                if (device.latitude != null && device.longitude != null) {
                    val pos = LatLng(device.latitude, device.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                    BottomSheetBehavior.from(binding.bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    Toast.makeText(this, "Sin ubicación", Toast.LENGTH_SHORT).show()
                }
            },
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
            ownerEmail = device.user_email,
            lastSeen = device.last_seen,
            accuracy = device.accuracy?.toDouble()
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
            val color = ContextCompat.getColor(this@SearchMapActivity, colorRes)

            markerBinding.markerBorder.background.setTint(color)
            markerBinding.markerArrow.setColorFilter(color)

            if (isThisPhone) {
                val blueColor = ContextCompat.getColor(this@SearchMapActivity, R.color.blue)
                markerBinding.markerContainer.background.setTint(blueColor)
                markerBinding.markerContainer.setPadding(8, 8, 8, 8)
            } else {
                markerBinding.markerContainer.background.setTintList(null)
                markerBinding.markerContainer.setPadding(2, 2, 2, 2)
            }

            val imageUrl = device.user_image
            if (!imageUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(this@SearchMapActivity)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request).drawable
                if (result != null) {
                    markerBinding.markerImage.setImageDrawable(result)
                    markerBinding.markerImage.clearColorFilter()
                    markerBinding.markerImage.imageTintList = null
                } else {
                    markerBinding.markerImage.setImageResource(R.drawable.ic_phone)
                    markerBinding.markerImage.setColorFilter(color)
                }
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