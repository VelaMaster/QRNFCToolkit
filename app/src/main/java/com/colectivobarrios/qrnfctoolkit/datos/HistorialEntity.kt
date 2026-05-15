package com.colectivobarrios.qrnfctoolkit.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial")
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: String, // QR_SCAN, QR_GEN, NFC_READ, NFC_WRITE
    val contenido: String,
    val timestamp: Long,
    val metadata: String? = null
)
