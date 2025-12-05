package com.tecsup.aurora.data.local

import io.realm.kotlin.types.RealmObject

class UserSession : RealmObject {
    var token: String = ""
    var refreshToken: String = ""
}