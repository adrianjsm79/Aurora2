plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Plugin de Realm para Kotlin
    alias(libs.plugins.realm)
}

android {
    namespace = "com.tecsup.aurora"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tecsup.aurora"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        viewBinding = true
    }

}

kotlin {
    jvmToolchain(17)
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation("androidx.activity:activity-ktx:1.11.0") // Dependencia para by viewModels en Activity
    implementation("androidx.fragment:fragment-ktx:1.8.1") // Dependencia para by activityViewModels en Fragment
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //servicios de google
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Country Code Picker
    implementation("com.hbb20:ccp:2.7.0")

    // --- MVVM: VIEWMODEL, LIVEDATA Y CORRUTINAS ---
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // --- NETWORKING: RETROFIT Y GSON ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // --- BASE DE DATOS LOCAL: REALM ---
    implementation(libs.realm.library.base)
}