package com.tecsup.aurora

import android.app.Application
import com.tecsup.aurora.data.local.UserSession
import com.tecsup.aurora.data.remote.ApiService
import com.tecsup.aurora.data.remote.LocationWebSocketClient
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.data.repository.LocationRepository
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager
import com.tecsup.aurora.viewmodel.HomeViewModelFactory
import com.tecsup.aurora.viewmodel.LocationViewModelFactory
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.tecsup.aurora.data.repository.ContactsRepository
import com.tecsup.aurora.viewmodel.ContactsViewModelFactory

class MyApplication : Application() {

    private val BASE_URL = "https://aurorabackend.up.railway.app"

    // --- Network (Retrofit) ---
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // --- Network (WebSocket) ---
    private val locationWebSocketClient by lazy {
        LocationWebSocketClient()
    }

    // --- Database (Realm) ---
    val realm: Realm by lazy {
        val config = RealmConfiguration.Builder(
            schema = setOf(UserSession::class)
        ).build()
        Realm.open(config)
    }

    // --- Repositories (La lógica de datos) ---
    val authRepository by lazy {
        AuthRepository(apiService, realm)
    }
    val deviceRepository by lazy {
        DeviceRepository(apiService)
    }
    val locationRepository by lazy {
        LocationRepository(locationWebSocketClient)
    }

    val settingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val contactsRepository by lazy {
        ContactsRepository(applicationContext)
    }

    // --- Managers (Controladores de Servicios) ---

    val trackingServiceManager by lazy {
        TrackingServiceManager(applicationContext)
    }

    // --- ViewModel Factories ---
    val homeViewModelFactory by lazy {
        HomeViewModelFactory(authRepository, deviceRepository, locationRepository)
    }

    val locationViewModelFactory by lazy {
        LocationViewModelFactory(settingsRepository, trackingServiceManager)
    }

    val contactsViewModelFactory by lazy {
        ContactsViewModelFactory(authRepository, contactsRepository)
    }
}