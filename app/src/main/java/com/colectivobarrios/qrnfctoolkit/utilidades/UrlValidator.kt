package com.colectivobarrios.qrnfctoolkit.utilidades

import android.net.Uri

/**
 * Utilidad de seguridad para validar URLs antes de abrirlas con Intent.ACTION_VIEW.
 * Solo se permiten esquemas http y https para evitar exploits via QR/NFC maliciosos
 * que podrian incluir esquemas peligrosos como javascript:, file://, intent://, etc.
 */
object UrlValidator {

    /** Esquemas permitidos para abrir con el navegador del sistema */
    private val SAFE_SCHEMES = setOf("http", "https")

    /**
     * Retorna true si la URL tiene un esquema seguro (http o https).
     * Retorna false para cualquier otro esquema o URL malformada.
     */
    fun isSafeUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url.trim())
            uri.scheme?.lowercase() in SAFE_SCHEMES
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detecta si el texto parece una URL (empieza con http/https).
     * Usar para decidir si mostrar boton "Abrir" en la UI.
     */
    fun looksLikeUrl(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
}
