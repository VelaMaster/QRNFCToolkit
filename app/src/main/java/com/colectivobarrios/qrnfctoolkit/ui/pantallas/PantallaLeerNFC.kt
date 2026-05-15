package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.datos.HistorialEntity
import com.colectivobarrios.qrnfctoolkit.utilidades.NFCHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLeerNFC(
    dao: HistorialDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var nfcState by remember { mutableStateOf(getNFCStatus(nfcAdapter)) }
    var scannedResult by remember { mutableStateOf<NFCHelper.NFCResult?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    DisposableEffect(Unit) {
        val nfcCallback = NfcAdapter.ReaderCallback { tag ->
            val result = NFCHelper.readTag(tag)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scannedResult = result
            showBottomSheet = true
            
            // LOG TO HISTORY
            scope.launch {
                dao.insert(HistorialEntity(
                    tipo = "NFC_READ",
                    contenido = result.payload ?: result.id,
                    timestamp = System.currentTimeMillis(),
                    metadata = result.id
                ))
            }
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled) { nfcAdapter.enableReaderMode(context as Activity, nfcCallback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null) }
        onDispose { if (nfcAdapter != null) { nfcAdapter.disableReaderMode(context as Activity) } }
    }
    LaunchedEffect(Unit) { while (true) { nfcState = getNFCStatus(nfcAdapter); delay(2000) } }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nfc_read_title), fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (nfcState) {
                NFCStatus.NoSoportado -> NFCErrorViewLocalized(
                    icon = Icons.Rounded.Nfc,
                    titulo = stringResource(R.string.nfc_status_unsupported),
                    descripcion = stringResource(R.string.nfc_read_error_desc)
                )
                NFCStatus.Desactivado -> NFCErrorViewLocalized(
                    icon = Icons.Rounded.Nfc,
                    titulo = stringResource(R.string.nfc_status_disabled),
                    descripcion = stringResource(R.string.nfc_read_disabled_desc),
                    actionText = stringResource(R.string.nfc_action_settings),
                    onAction = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                )
                NFCStatus.Listo -> ScanningAnimationLocalized()
            }
        }
    }
    if (showBottomSheet && scannedResult != null) { ModalBottomSheet(onDismissRequest = { showBottomSheet = false; scannedResult = null }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) { ResultNFCContentLocalized(result = scannedResult!!, onDismiss = { showBottomSheet = false; scannedResult = null }) } }
}

@Composable
fun NFCErrorViewLocalized(icon: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, descripcion: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp)); Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp)); Text(descripcion, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionText != null && onAction != null) { Spacer(modifier = Modifier.height(24.dp)); Button(onClick = onAction) { Text(actionText) } }
    }
}

@Composable
fun ScanningAnimationLocalized() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_scan")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.5f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_alpha")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) { drawCircle(color = Color.Cyan.copy(alpha = alpha), radius = size.minDimension / 2 * scale, style = Stroke(width = 4.dp.toPx())) }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) { Icon(Icons.Rounded.Nfc, contentDescription = null, modifier = Modifier.padding(16.dp).size(48.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(modifier = Modifier.height(32.dp)); Text(stringResource(R.string.nfc_status_ready), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text(stringResource(R.string.nfc_hint_approach), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ResultNFCContentLocalized(result: NFCHelper.NFCResult, onDismiss: () -> Unit) {
    val context = LocalContext.current; val isUrl = remember(result.payload) { result.payload?.startsWith("http", ignoreCase = true) == true }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = if (isUrl) Icons.Rounded.Language else Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp)); Text(result.type, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp)); Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) { LabelValueLocalized(stringResource(R.string.nfc_read_result_id), result.id); if (result.payload != null) { Spacer(modifier = Modifier.height(8.dp)); LabelValueLocalized(stringResource(R.string.nfc_read_result_content), result.payload) }; Spacer(modifier = Modifier.height(8.dp)); LabelValueLocalized(stringResource(R.string.nfc_read_result_tech), result.techList.joinToString(", ")) } }
        Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isUrl) { Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(result.payload))); onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.nfc_read_action_open)) } }
            else if (result.payload != null) { Button(onClick = { val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NFC Content", result.payload)); Toast.makeText(context, context.getString(R.string.qr_scanner_copied), Toast.LENGTH_SHORT).show(); onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.nfc_read_action_copy)) } }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.nfc_read_action_close)) }
        }
    }
}

@Composable
fun LabelValueLocalized(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge) } }
private enum class NFCStatus { Listo, Desactivado, NoSoportado }
private fun getNFCStatus(adapter: NfcAdapter?): NFCStatus { return when { adapter == null -> NFCStatus.NoSoportado; !adapter.isEnabled -> NFCStatus.Desactivado; else -> NFCStatus.Listo } }
