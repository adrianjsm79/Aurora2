package com.tecsup.aurora

import android.app.Application
import com.tecsup.aurora.data.local.DeviceMapState
import com.tecsup.aurora.data.local.TracePoint
import com.tecsup.aurora.data.local.UserSession
import com.tecsup.aurora.data.remote.ApiService
import com.tecsup.aurora.data.remote.LocationWebSocketClient
import com.tecsup.aurora.data.repository.AuthRepository
import com.tecsup.aurora.data.repository.ContactsRepository
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.data.repository.LocationRepository
import com.tecsup.aurora.data.repository.MapRepository
import com.tecsup.aurora.data.repository.SettingsRepository
import com.tecsup.aurora.service.TrackingServiceManager
import com.tecsup.aurora.viewmodel.ContactsViewModelFactory
import com.tecsup.aurora.viewmodel.HomeViewModelFactory
import com.tecsup.aurora.viewmodel.LocationViewModelFactory
import com.tecsup.aurora.viewmodel.MapViewModelFactory
import com.tecsup.aurora.viewmodel.ProfileViewModelFactory
import com.tecsup.aurora.viewmodel.SecurityViewModelFactory
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyApplication : Application() {

    private val BASE_URL = "https://aurorabackend.up.railway.app"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    private val locationWebSocketClient by lazy {
        LocationWebSocketClient()
    }

    val realm: Realm by lazy {
        val config = RealmConfiguration.Builder(
            schema = setOf(
                UserSession::class,
                DeviceMapState::class, // Registra el nuevo modelo
                TracePoint::class      // Registra el nuevo modelo
            )
        ).build()
        Realm.open(config)
    }

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

    val trackingServiceManager by lazy {
        TrackingServiceManager(applicationContext)
    }


    val homeViewModelFactory by lazy {
        HomeViewModelFactory(
            authRepository, deviceRepository, locationRepository, settingsRepository
        )
    }

    val locationViewModelFactory by lazy {
        LocationViewModelFactory(settingsRepository, trackingServiceManager)
    }

    val contactsViewModelFactory by lazy {
        ContactsViewModelFactory(authRepository, contactsRepository)
    }

    val securityViewModelFactory by lazy {
        SecurityViewModelFactory(settingsRepository)
    }

    val profileViewModelFactory by lazy {
        ProfileViewModelFactory(authRepository)
    }

    val mapRepository by lazy {
        MapRepository(realm, apiService, applicationContext)
    }

    val mapViewModelFactory by lazy {
        MapViewModelFactory(
            authRepository,
            deviceRepository,
            locationRepository,
            settingsRepository,
            trackingServiceManager,
            mapRepository
        )
    }
}