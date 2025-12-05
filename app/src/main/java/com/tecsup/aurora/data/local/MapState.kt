package com.tecsup.aurora.data.local

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class DeviceMapState : RealmObject {
    @PrimaryKey
    var deviceId: Int = 0
    var isTracingEnabled: Boolean = false // "Seguir rastro"
    var isRoutingEnabled: Boolean = false // "Trazar ruta"
}

class TracePoint : RealmObject {
    var deviceId: Int = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var timestamp: Long = 0
}