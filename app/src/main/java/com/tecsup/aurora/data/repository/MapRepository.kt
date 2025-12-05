package com.tecsup.aurora.data.repository

import com.tecsup.aurora.data.local.DeviceMapState
import com.tecsup.aurora.data.local.TracePoint
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import com.google.maps.android.PolyUtil
import com.google.android.gms.maps.model.LatLng
import com.tecsup.aurora.data.remote.ApiService

class MapRepository(private val realm: Realm, private val apiService: ApiService) {

    //GESTIÓN DE ESTADOs

    fun setRouting(deviceId: Int) {
        realm.writeBlocking {
            // Desactivar ruta en todos los demás (Solo una ruta activa a la vez)
            val all = query<DeviceMapState>().find()
            all.forEach { it.isRoutingEnabled = false }

            // Activar o Crear estado para este dispositivo
            val state = query<DeviceMapState>("deviceId == $0", deviceId).first().find()
                ?: copyToRealm(DeviceMapState().apply { this.deviceId = deviceId })

            state.isRoutingEnabled = true
        }
    }

    fun toggleTracing(deviceId: Int): Boolean {
        var isEnabled = false
        realm.writeBlocking {
            val state = query<DeviceMapState>("deviceId == $0", deviceId).first().find()
                ?: copyToRealm(DeviceMapState().apply { this.deviceId = deviceId })

            state.isTracingEnabled = !state.isTracingEnabled
            isEnabled = state.isTracingEnabled
        }
        return isEnabled
    }

    suspend fun getRealRoute(origin: LatLng, dest: LatLng, apiKey: String): List<LatLng> {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${dest.latitude},${dest.longitude}"

        val url = "https://maps.googleapis.com/maps/api/directions/json"

        try {
            val response = apiService.getDirections(url, originStr, destStr, apiKey)

            if (response.isSuccessful && response.body() != null) {
                val routes = response.body()!!.routes
                if (routes.isNotEmpty()) {
                    // Obtenemos el string encriptado
                    val encodedString = routes[0].overviewPolyline.points
                    //  decodificamos a una lista de puntos LatLng
                    return PolyUtil.decode(encodedString)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Si falla, devolvemos una lista vacía (o podrías devolver la línea recta como fallback)
        return emptyList()
    }

    fun clearTrace(deviceId: Int) {
        realm.writeBlocking {
            // Borrar puntos
            val points = query<TracePoint>("deviceId == $0", deviceId).find()
            delete(points)
            // Desactivar tracing
            val state = query<DeviceMapState>("deviceId == $0", deviceId).first().find()
            state?.isTracingEnabled = false
        }
    }

    fun getMapState(deviceId: Int): DeviceMapState? {
        return realm.query<DeviceMapState>("deviceId == $0", deviceId).first().find()
    }

    fun getActiveRouteDeviceId(): Int? {
        return realm.query<DeviceMapState>("isRoutingEnabled == $0", true).first().find()?.deviceId
    }

    // --- GESTIÓN DE PUNTOS (MIGAS DE PAN) ---

    fun addTracePoint(deviceId: Int, lat: Double, lon: Double) {
        // Solo guardamos si el tracing está activo para este device
        val state = getMapState(deviceId)
        if (state?.isTracingEnabled == true) {
            realm.writeBlocking {
                copyToRealm(TracePoint().apply {
                    this.deviceId = deviceId
                    this.latitude = lat
                    this.longitude = lon
                    this.timestamp = System.currentTimeMillis()
                })
            }
        }
    }

    fun getTracePoints(deviceId: Int): List<TracePoint> {
        return realm.query<TracePoint>("deviceId == $0", deviceId)
            .sort("timestamp", Sort.ASCENDING)
            .find()
            .toList() // Convertir a lista para usar fuera de Realm
    }
}