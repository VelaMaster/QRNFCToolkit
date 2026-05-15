package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.datos.HistorialEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(
    dao: HistorialDao,
    onBack: () -> Unit
) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf(
        stringResource(R.string.history_filter_all),
        stringResource(R.string.history_filter_qr),
        stringResource(R.string.history_filter_nfc)
    )

    val items by remember(filterIndex) {
        when (filterIndex) {
            1 -> dao.getFiltered("QR_%", "QR_%")
            2 -> dao.getFiltered("NFC_%", "NFC_%")
            else -> dao.getAll()
        }
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                filters.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = filterIndex == index,
                        onClick = { filterIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                        label = { Text(label) }
                    )
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                val qrItems = remember(items) { items.filter { it.tipo.startsWith("QR") } }
                val nfcItems = remember(items) { items.filter { it.tipo.startsWith("NFC") } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filterIndex == 0) {
                        // Sección QR
                        if (qrItems.isNotEmpty()) {
                            item {
                                SeccionHeader(
                                    icono = Icons.Rounded.QrCode,
                                    titulo = stringResource(R.string.history_filter_qr),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(qrItems, key = { it.id }) { item ->
                                TarjetaHistorial(item = item)
                            }
                        }
                        // Sección NFC
                        if (nfcItems.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                SeccionHeader(
                                    icono = Icons.Rounded.Nfc,
                                    titulo = stringResource(R.string.history_filter_nfc),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            items(nfcItems, key = { it.id }) { item ->
                                TarjetaHistorial(item = item)
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            TarjetaHistorial(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeccionHeader(
    icono: ImageVector,
    titulo: String,
    color: Color,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = tint)
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
fun TarjetaHistorial(item: HistorialEntity) {
    val isQr = item.tipo.startsWith("QR")
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    val titleRes = when (item.tipo) {
        "QR_SCAN" -> R.string.history_type_qr_scan
        "QR_GEN" -> R.string.history_type_qr_gen
        "NFC_READ" -> R.string.history_type_nfc_read
        "NFC_WRITE" -> R.string.history_type_nfc_write
        else -> R.string.app_name
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isQr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isQr) Icons.Rounded.QrCode else Icons.Rounded.Nfc,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (isQr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = item.contenido,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
