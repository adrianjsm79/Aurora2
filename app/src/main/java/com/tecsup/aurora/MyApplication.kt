package com.tecsup.aurora

import android.app.Application
import com.tecsup.aurora.data.local.UserSession // <-- 1. Importa tu modelo de Realm
import com.tecsup.aurora.data.remote.ApiService
import com.tecsup.aurora.data.repository.AuthRepository
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.tecsup.aurora.data.repository.DeviceRepository
import com.tecsup.aurora.viewmodel.HomeViewModelFactory

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

    // 2. Añade UserSession al esquema de Realm
    val realm: Realm by lazy {
        val config = RealmConfiguration.Builder(
            schema = setOf(UserSession::class) // <-- AÑADIDO
        ).build()
        Realm.open(config)
    }

    // 3. Pasa la instancia de 'realm' al repositorio
    val authRepository by lazy {
        AuthRepository(apiService, realm) // <-- ACTUALIZADO
    }

    // 3. Crea la instancia del DeviceRepository
    val deviceRepository by lazy {
        DeviceRepository(apiService)
    }

    // 4. Crea una instancia de la HomeViewModelFactory
    val homeViewModelFactory by lazy {
        HomeViewModelFactory(authRepository, deviceRepository)
    }

}