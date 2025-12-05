package com.tecsup.aurora.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.tecsup.aurora.data.local.DeviceMapState
import com.tecsup.aurora.data.local.TracePoint
import com.tecsup.aurora.data.remote.ApiService
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort

class MapRepository(
    private val realm: Realm,
    private val apiService: ApiService,
    private val context: Context
) {


    fun setRouting(deviceId: Int) {
        realm.writeBlocking {
            val all = query<DeviceMapState>().find()
            all.forEach { it.isRoutingEnabled = false }

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

    fun clearTrace(deviceId: Int) {
        realm.writeBlocking {
            val points = query<TracePoint>("deviceId == $0", deviceId).find()
            delete(points)

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

    //GESTIÓN DE PUNTOS

    fun addTracePoint(deviceId: Int, lat: Double, lon: Double) {
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
            .toList()
    }

    //API DE DIRECCIONES

    suspend fun getRealRoute(origin: LatLng, dest: LatLng, apiKeyFromViewModel: String? = null): List<LatLng> {
        // Intentamos obtener la Key del Manifest, si falla, usamos la que pase el ViewModel (opcional)
        val apiKey = if (apiKeyFromViewModel.isNullOrEmpty()) getGoogleApiKey() else apiKeyFromViewModel

        if (apiKey.isEmpty()) {
            Log.e("MapRepository", "API Key no encontrada. La ruta no se calculará.")
            return emptyList()
        }

        val originStr = "${origin.latitude},${origin.longitude}"
        val destStr = "${dest.latitude},${dest.longitude}"
        val url = "https://maps.googleapis.com/maps/api/directions/json"

        return try {
            val response = apiService.getDirections(url, originStr, destStr, apiKey)

            if (response.isSuccessful && response.body() != null) {
                val routes = response.body()!!.routes
                if (routes.isNotEmpty()) {
                    val encodedString = routes[0].overviewPolyline.points
                    // Decodifica el string a puntos LatLng
                    PolyUtil.decode(encodedString)
                } else {
                    emptyList()
                }
            } else {
                Log.e("MapRepository", "Error API Google: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MapRepository", "Excepción al obtener ruta", e)
            emptyList()
        }
    }

    private fun getGoogleApiKey(): String {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val bundle = appInfo.metaData
            return bundle.getString("com.google.android.geo.API_KEY", "")
        } catch (e: Exception) {
            Log.e("MapRepository", "No se pudo leer la API Key del Manifest", e)
            return ""
        }
    }
}