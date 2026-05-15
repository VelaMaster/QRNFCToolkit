package com.colectivobarrios.qrnfctoolkit.utilidades

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import java.nio.charset.Charset
import java.util.*

object NFCHelper {

    data class NFCResult(
        val id: String,
        val type: String,
        val payload: String?,
        val techList: List<String>
    )

    fun readTag(tag: Tag): NFCResult {
        // ... (existing code remains the same)
        val id = tag.id.joinToString(":") { "%02X".format(it) }
        val techList = tag.techList.map { it.substringAfterLast(".") }
        
        val ndef = Ndef.get(tag)
        var payloadText: String? = null
        var typeText = "Desconocido"

        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                if (ndefMessage != null) {
                    val record = ndefMessage.records.firstOrNull()
                    if (record != null) {
                        payloadText = parseRecord(record)
                        typeText = when {
                            record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT) -> "Texto NDEF"
                            record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_URI) -> "Enlace NDEF"
                            record.tnf == NdefRecord.TNF_ABSOLUTE_URI -> "URI Absoluta"
                            else -> "Datos NDEF"
                        }
                    }
                }
                ndef.close()
            } catch (e: Exception) {
                payloadText = "Error al leer NDEF: ${e.message}"
            }
        }

        return NFCResult(
            id = id,
            type = if (ndef != null) typeText else "Tag sin NDEF",
            payload = payloadText,
            techList = techList
        )
    }

    fun writeTag(tag: Tag, message: NdefMessage): Result<Unit> {
        val ndef = Ndef.get(tag) ?: return Result.failure(Exception("Tag no soporta NDEF"))
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                return Result.failure(Exception("Tag es de solo lectura"))
            }
            if (ndef.maxSize < message.toByteArray().size) {
                ndef.close()
                return Result.failure(Exception("Mensaje demasiado grande para este tag"))
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createTextRecord(text: String, locale: Locale = Locale.getDefault()): NdefRecord {
        val langBytes = locale.language.toByteArray(Charset.forName("US-ASCII"))
        val utf8 = Charset.forName("UTF-8")
        val textBytes = text.toByteArray(utf8)
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)

        payload[0] = langLength.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }

    fun createUriRecord(uriString: String): NdefRecord {
        return NdefRecord.createUri(uriString)
    }

    fun createWiFiRecord(ssid: String, password: String?, security: String): NdefMessage {
        // Simple WiFi NDEF format (Android application record or smart poster)
        // For broad compatibility, we often use a custom mime type or a specific format
        // Here we'll use the standardized "application/vnd.wfa.wsc" or just a text record for simplicity if not complex
        val wifiData = "WIFI:S:$ssid;T:$security;P:${password ?: ""};;"
        return NdefMessage(createTextRecord(wifiData))
    }

    private fun parseRecord(record: NdefRecord): String? {
        return when {
            record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_TEXT) -> parseTextRecord(record)
            record.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.type, NdefRecord.RTD_URI) -> parseUriRecord(record)
            else -> record.payload.decodeToString()
        }
    }

    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        val statusByte = payload[0].toInt()
        val languageCodeLength = statusByte and 0x1F
        val charset = if ((statusByte and 0x80) == 0) Charset.forName("UTF-8") else Charset.forName("UTF-16")
        return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, charset)
    }

    private fun parseUriRecord(record: NdefRecord): String {
        val payload = record.payload
        val prefix = URI_PREFIX_MAP[payload[0].toInt()] ?: ""
        val content = String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
        return prefix + content
    }

    private val URI_PREFIX_MAP = mapOf(
        0x00 to "",
        0x01 to "http://www.",
        0x02 to "https://www.",
        0x03 to "http://",
        0x04 to "https://",
        0x05 to "tel:",
        0x06 to "mailto:",
        0x07 to "ftp://anonymous:anonymous@",
        0x08 to "ftp://ftp.",
        0x09 to "ftps://",
        0x0A to "sftp://",
        0x0B to "smb://",
        0x0C to "nfs://",
        0x0D to "ftp://",
        0x0E to "dav://",
        0x0F to "news:",
        0x10 to "telnet://",
        0x11 to "imap:",
        0x12 to "rtsp://",
        0x13 to "urn:",
        0x14 to "pop:",
        0x15 to "sip:",
        0x16 to "sips:",
        0x17 to "tftp:",
        0x18 to "btspp://",
        0x19 to "btl2cap://",
        0x1A to "btgoep://",
        0x1B to "tcpobex://",
        0x1C to "irdaobex://",
        0x1D to "file://",
        0x1E to "urn:epc:id:",
        0x1F to "urn:epc:tag:",
        0x20 to "urn:epc:pat:",
        0x21 to "urn:epc:raw:",
        0x22 to "urn:epc:",
        0x23 to "urn:nfc:"
    )
}
