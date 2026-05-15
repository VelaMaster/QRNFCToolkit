package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.datos.HistorialEntity
import com.colectivobarrios.qrnfctoolkit.utilidades.NFCHelper
import com.colectivobarrios.qrnfctoolkit.utilidades.WiFiHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscribirNFC(
    dao: HistorialDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    
    val textoLabel = stringResource(R.string.qr_scanner_result_text)
    val webLabel = stringResource(R.string.qr_scanner_result_url)
    val wifiLabel = stringResource(R.string.menu_leer_nfc)

    var tipoNFCIdx by remember { mutableIntStateOf(0) }
    val tipos = listOf(Icons.Rounded.TextFields to textoLabel, Icons.Rounded.Language to webLabel, Icons.Rounded.Wifi to wifiLabel)

    var contenido by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiPassVisible by remember { mutableStateOf(false) }

    var showWriteSheet by remember { mutableStateOf(false) }
    var writeStatus by remember { mutableStateOf<WriteStatus>(WriteStatus.Idle) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFormValid = remember(tipoNFCIdx, contenido, wifiSsid) {
        when (tipoNFCIdx) {
            0 -> contenido.isNotBlank()
            1 -> contenido.isNotBlank() && (contenido.startsWith("http") || contenido.contains("."))
            2 -> wifiSsid.isNotBlank()
            else -> false
        }
    }

    DisposableEffect(showWriteSheet) {
        if (showWriteSheet && nfcAdapter != null && nfcAdapter.isEnabled) {
            val nfcCallback = NfcAdapter.ReaderCallback { tag ->
                val message = when (tipoNFCIdx) {
                    0 -> NdefMessage(NFCHelper.createTextRecord(contenido))
                    1 -> NdefMessage(NFCHelper.createUriRecord(if (contenido.startsWith("http")) contenido else "https://$contenido"))
                    2 -> NFCHelper.createWiFiRecord(wifiSsid, wifiPassword, "WPA")
                    else -> null
                }
                if (message != null) {
                    val result = NFCHelper.writeTag(tag, message)
                    scope.launch {
                        if (result.isSuccess) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            writeStatus = WriteStatus.Success
                            
                            // LOG TO HISTORY
                            dao.insert(HistorialEntity(
                                tipo = "NFC_WRITE",
                                contenido = if(tipoNFCIdx == 2) wifiSsid else contenido,
                                timestamp = System.currentTimeMillis()
                            ))

                            delay(2000)
                            showWriteSheet = false
                            writeStatus = WriteStatus.Idle
                        } else {
                            writeStatus = WriteStatus.Error(result.exceptionOrNull()?.message ?: "Error")
                        }
                    }
                }
            }
            nfcAdapter.enableReaderMode(context as Activity, nfcCallback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null)
        }
        onDispose { if (nfcAdapter != null) nfcAdapter.disableReaderMode(context as Activity) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nfc_write_title), fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(R.string.nfc_write_question), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                tipos.forEachIndexed { idx, pair ->
                    TarjetaTipoNFCLocalized(label = pair.second, icon = pair.first, selected = tipoNFCIdx == idx, modifier = Modifier.weight(1f)) { tipoNFCIdx = idx }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AnimatedContent(targetState = tipoNFCIdx, label = "input") { idx ->
                if (idx == 2) {
                    WiFiInputSectionLocalized(ssid = wifiSsid, onSsidChange = { wifiSsid = it }, password = wifiPassword, onPasswordChange = { wifiPassword = it }, passwordVisible = wifiPassVisible, onTogglePasswordVisible = { wifiPassVisible = !wifiPassVisible }, onUseCurrent = { val current = WiFiHelper.getCurrentSsid(context); if (!current.isNullOrBlank() && current != "<unknown ssid>") wifiSsid = current })
                } else {
                    OutlinedTextField(value = contenido, onValueChange = { contenido = it }, label = { Text(tipos[idx].second) }, leadingIcon = { Icon(tipos[idx].first, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = if (idx == 1) KeyboardType.Uri else KeyboardType.Text, imeAction = ImeAction.Done))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { if (nfcAdapter == null) { Toast.makeText(context, context.getString(R.string.nfc_status_unsupported), Toast.LENGTH_SHORT).show() } else if (!nfcAdapter.isEnabled) { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) } else { showWriteSheet = true } }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = isFormValid, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Edit, null); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.nfc_write_action_prepare), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showWriteSheet) {
        ModalBottomSheet(onDismissRequest = { showWriteSheet = false; writeStatus = WriteStatus.Idle }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            WriteBottomSheetContentLocalized(status = writeStatus)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaTipoNFCLocalized(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, label = "scale")
    Card(onClick = onClick, modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WiFiInputSectionLocalized(ssid: String, onSsidChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, passwordVisible: Boolean, onTogglePasswordVisible: () -> Unit, onUseCurrent: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = ssid, onValueChange = onSsidChange, label = { Text(stringResource(R.string.wifi_ssid_label)) }, placeholder = { Text(stringResource(R.string.wifi_ssid_hint)) }, leadingIcon = { Icon(Icons.Rounded.Wifi, null) }, trailingIcon = { IconButton(onClick = onUseCurrent) { Icon(Icons.Rounded.Router, null) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text(stringResource(R.string.wifi_pass_label)) }, placeholder = { Text(stringResource(R.string.wifi_pass_hint)) }, leadingIcon = { Icon(Icons.Rounded.Lock, null) }, trailingIcon = { IconButton(onClick = onTogglePasswordVisible) { Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
    }
}

@Composable
private fun WriteBottomSheetContentLocalized(status: WriteStatus) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (status) {
            WriteStatus.Idle -> { WritingAnimationLocalized(); Spacer(modifier = Modifier.height(24.dp)); Text(stringResource(R.string.nfc_write_sheet_idle_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(stringResource(R.string.nfc_write_sheet_idle_desc), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            WriteStatus.Success -> { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50)); Spacer(modifier = Modifier.height(16.dp)); Text(stringResource(R.string.nfc_write_success_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(stringResource(R.string.nfc_write_success_desc), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            is WriteStatus.Error -> { Icon(Icons.Rounded.Error, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error); Spacer(modifier = Modifier.height(16.dp)); Text(stringResource(R.string.nfc_write_error_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(status.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error) }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WritingAnimationLocalized() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_write")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.5f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_alpha")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = Color.Magenta.copy(alpha = alpha), radius = size.minDimension / 2 * scale, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())) }
        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(80.dp)) { Icon(Icons.Rounded.SettingsRemote, contentDescription = null, modifier = Modifier.padding(16.dp).size(48.dp), tint = MaterialTheme.colorScheme.secondary) }
    }
}

private sealed class WriteStatus { object Idle : WriteStatus(); object Success : WriteStatus(); data class Error(val message: String) : WriteStatus() }
