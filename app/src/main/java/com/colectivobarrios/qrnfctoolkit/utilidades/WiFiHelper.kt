package com.colectivobarrios.qrnfctoolkit.utilidades

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiNetworkSpecifier
import android.net.NetworkRequest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast

data class WiFiConfig(
    val ssid: String,
    val password: String?,
    val security: String?
)

object WiFiHelper {
    
    fun parseWiFiQR(text: String): WiFiConfig? {
        if (!text.startsWith("WIFI:", ignoreCase = true)) return null
        
        val ssid = extractValue(text, "S:") ?: return null
        val password = extractValue(text, "P:")
        val security = extractValue(text, "T:")
        
        return WiFiConfig(ssid, password, security)
    }

    private fun extractValue(text: String, prefix: String): String? {
        val start = text.indexOf(prefix, ignoreCase = true)
        if (start == -1) return null
        val valueStart = start + prefix.length
        val end = text.indexOf(";", valueStart)
        return if (end == -1) text.substring(valueStart) else text.substring(valueStart, end)
    }

    fun connectToWiFi(context: Context, config: WiFiConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(config.ssid)
            
            if (config.password != null) {
                specifier.setWpa2Passphrase(config.password)
            }
            
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier.build())
                .build()

            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            connectivityManager?.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {})
            
            Toast.makeText(context, "Solicitando conexión a ${config.ssid}...", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Abre los ajustes para conectar a ${config.ssid}", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    fun getCurrentSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        if (info != null && info.networkId != -1) {
            val ssid = info.ssid
            return if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid.substring(1, ssid.length - 1)
            } else {
                ssid
            }
        }
        return null
    }
}
