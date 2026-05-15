package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.ui.componentes.TarjetaHerramienta
import com.colectivobarrios.qrnfctoolkit.ui.navegacion.*

data class Herramienta(
    val titulo: Int,
    val descripcion: Int,
    val icono: ImageVector,
    val destino: Any
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    onHerramientaClick: (Any) -> Unit
) {
    val herramientas = listOf(
        Herramienta(R.string.menu_escanear_qr, R.string.menu_escanear_qr_desc, Icons.Rounded.QrCodeScanner, EscanearQR),
        Herramienta(R.string.menu_generar_qr, R.string.menu_generar_qr_desc, Icons.Rounded.QrCode, GenerarQR),
        Herramienta(R.string.menu_leer_nfc, R.string.menu_leer_nfc_desc, Icons.Rounded.Nfc, LeerNFC),
        Herramienta(R.string.menu_escribir_nfc, R.string.menu_escribir_nfc_desc, Icons.Rounded.SettingsRemote, EscribirNFC),
        Herramienta(R.string.menu_historial, R.string.menu_historial_desc, Icons.Rounded.History, Historial),
        Herramienta(R.string.menu_ajustes, R.string.menu_ajustes_desc, Icons.Rounded.Settings, Ajustes)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(herramientas) { herramienta ->
                TarjetaHerramienta(
                    titulo = stringResource(herramienta.titulo),
                    descripcion = stringResource(herramienta.descripcion),
                    icono = herramienta.icono,
                    onClick = { onHerramientaClick(herramienta.destino) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaInicioPreview() {
    MaterialTheme {
        PantallaInicio(onHerramientaClick = {})
    }
}
