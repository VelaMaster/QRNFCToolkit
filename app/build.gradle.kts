import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("androidx.room") version "2.8.4"
    kotlin("plugin.serialization") version "2.2.10"
}

// Cargar keystore desde local.properties — NUNCA hardcodear claves aqui.
// Para firmar el release, rellena las 4 claves en local.properties (ver comentarios alli).
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

android {
    namespace = "com.colectivobarrios.qrnfctoolkit"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.colectivobarrios.qrnfctoolkit"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile     = localProps.getProperty("KEYSTORE_PATH")?.let { file(it) }
            storePassword = localProps.getProperty("STORE_PASSWORD")
            keyAlias      = localProps.getProperty("KEY_ALIAS")
            keyPassword   = localProps.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true   // Activa R8 + ofuscacion
            isShrinkResources = true   // Elimina recursos no usados
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled     = false
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Reemplaza el bloque kotlinOptions (deprecado) con la nueva API compilerOptions
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)

    // ML Kit
    implementation(libs.mlkit.barcode.scanning)

    // ZXing for QR Generation
    implementation(libs.zxing.core)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
