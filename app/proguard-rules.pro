# =============================================================
# ProGuard / R8 rules — QRNFCToolkit
# =============================================================

# ---- Room -------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }

# ---- Kotlin Serialization ---------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.colectivobarrios.qrnfctoolkit.**$$serializer { *; }
-keepclassmembers class com.colectivobarrios.qrnfctoolkit.** {
    *** Companion;
}
-keepclasseswithmembers class com.colectivobarrios.qrnfctoolkit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- ML Kit / Barcode Scanning ----------------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**

# ---- ZXing (generacion de QR) -----------------------------------
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ---- CameraX ----------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Jetpack Compose --------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---- NFC / Android API ------------------------------------------
-keep class android.nfc.** { *; }

# ---- Accompanist ------------------------------------------------
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ---- Stack traces legibles (oculta nombre fuente, conserva lineas) --
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ---- Entidades y DAOs del proyecto (Room usa reflexion) ----------
-keep class com.colectivobarrios.qrnfctoolkit.datos.** { *; }
-keep class com.colectivobarrios.qrnfctoolkit.utilidades.** { *; }
