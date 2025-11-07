package com.tecsup.aurora.data.local

import io.realm.kotlin.types.RealmObject

// Este es el objeto que guardaremos en Realm
// para mantener la sesión activa.
class UserSession : RealmObject {
    var token: String = ""
    var refreshToken: String = ""
}