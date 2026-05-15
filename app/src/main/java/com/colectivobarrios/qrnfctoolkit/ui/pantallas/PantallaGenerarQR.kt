package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import android.graphics.Bitmap
import android.graphics.Canvas as ACanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as APaint
import android.graphics.Path as APath
import android.graphics.RectF as ARectF
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.datos.HistorialEntity
import com.colectivobarrios.qrnfctoolkit.utilidades.QRGeneratorLogic
import com.colectivobarrios.qrnfctoolkit.utilidades.WiFiHelper
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder as QREncoder
import kotlinx.coroutines.launch

private data class TipoConfig(val label: String, val icon: ImageVector, val hint: String)
private data class PaletaMD3(val fg: Int, val bg: Int, val nameRes: Int)

private val PALETAS_MD3 = listOf(
    PaletaMD3(AndroidColor.BLACK, AndroidColor.WHITE, R.string.settings_theme_light),
    PaletaMD3(AndroidColor.rgb(103, 80, 164), AndroidColor.rgb(234, 221, 255), R.string.settings_appearance),
    PaletaMD3(AndroidColor.rgb(0, 107, 107), AndroidColor.rgb(186, 255, 255), R.string.settings_appearance),
    PaletaMD3(AndroidColor.rgb(146, 65, 59), AndroidColor.rgb(255, 218, 214), R.string.settings_appearance),
    PaletaMD3(AndroidColor.rgb(56, 107, 33), AndroidColor.rgb(184, 244, 151), R.string.settings_appearance),
    PaletaMD3(AndroidColor.rgb(125, 82, 0), AndroidColor.rgb(255, 223, 153), R.string.settings_appearance),
    PaletaMD3(AndroidColor.rgb(0, 95, 175), AndroidColor.rgb(212, 227, 255), R.string.settings_appearance),
    PaletaMD3(AndroidColor.WHITE, AndroidColor.rgb(28, 27, 31), R.string.settings_theme_dark)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGenerarQR(
    dao: HistorialDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // ... (rest of logic)

    val tipos = listOf(
        TipoConfig(stringResource(R.string.qr_scanner_result_text), Icons.Rounded.TextFields, stringResource(R.string.qr_scanner_hint)),
        TipoConfig(stringResource(R.string.qr_scanner_result_url), Icons.Rounded.Language, "https://ejemplo.com"),
        TipoConfig(stringResource(R.string.menu_leer_nfc), Icons.Rounded.Wifi, stringResource(R.string.wifi_ssid_hint)),
        TipoConfig(stringResource(R.string.lang_en), Icons.Rounded.Email, "correo@ejemplo.com"),
        TipoConfig(stringResource(R.string.lang_fr), Icons.Rounded.Smartphone, "+1 123 456 789")
    )

    var tipoIdx by remember { mutableIntStateOf(0) }
    var contenido by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSegIdx by remember { mutableIntStateOf(0) }
    val seguridades = listOf("WPA", "WEP", "nopass")
    var wifiPassVisible by remember { mutableStateOf(false) }
    var paletaIdx by remember { mutableIntStateOf(0) }
    var mostrarLogo by remember { mutableStateOf(true) }
    var showColorDialog by remember { mutableStateOf(false) }
    var qrMatrix by remember { mutableStateOf<com.google.zxing.qrcode.encoder.ByteMatrix?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorGeneracion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(paletaIdx, mostrarLogo, qrMatrix) {
        val matrix = qrMatrix ?: return@LaunchedEffect
        val paleta = PALETAS_MD3[paletaIdx]
        val base = renderBitmapFromMatrix(matrix, paleta.fg, paleta.bg)
        qrBitmap = if (mostrarLogo && base != null) overlayLogoStatic(base, tipoIdx, paleta.fg, paleta.bg) else base
    }

    fun generarQR() {
        val text = when (tipoIdx) {
            0 -> contenido.takeIf { it.isNotBlank() }
            1 -> if (contenido.isBlank()) null else if (contenido.startsWith("http")) contenido else "https://$contenido"
            2 -> if (wifiSsid.isBlank()) null else "WIFI:T:${seguridades[wifiSegIdx]};S:${wifiSsid};P:${wifiPassword};;"
            3 -> if (contenido.isBlank()) null else "mailto:$contenido"
            4 -> if (contenido.isBlank()) null else "tel:$contenido"
            else -> null
        }
        if (text == null) { errorGeneracion = context.getString(R.string.qr_generator_error_incomplete); return }
        errorGeneracion = null
        try { 
            qrMatrix = QREncoder.encode(text, ErrorCorrectionLevel.H, hashMapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix 
            // LOG TO HISTORY
            scope.launch {
                dao.insert(HistorialEntity(
                    tipo = "QR_GEN",
                    contenido = text,
                    timestamp = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) { errorGeneracion = "Error" }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.qr_generator_title), fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(R.string.qr_generator_type_label), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { for (i in 0..2) { TarjetaTipoSymmetricRaw(tipos[i].label, tipos[i].icon, tipoIdx == i, Modifier.weight(1f)) { tipoIdx = i; qrBitmap = null; qrMatrix = null } } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { for (i in 3..4) { TarjetaTipoSymmetricRaw(tipos[i].label, tipos[i].icon, tipoIdx == i, Modifier.weight(1f)) { tipoIdx = i; qrBitmap = null; qrMatrix = null } }
                    Spacer(Modifier.weight(1f)) }
            }
            AnimatedContent(targetState = tipoIdx, label = "anim") { idx ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (idx == 2) { SeccionWifiInputsLocalized(ssid = wifiSsid, onSsidChange = { wifiSsid = it; qrBitmap = null; qrMatrix = null }, password = wifiPassword, onPasswordChange = { wifiPassword = it; qrBitmap = null; qrMatrix = null }, securityIdx = wifiSegIdx, onSecurityChange = { wifiSegIdx = it; qrBitmap = null; qrMatrix = null }, passwordVisible = wifiPassVisible, onToggleVisible = { wifiPassVisible = !wifiPassVisible }, onCurrentWifi = { val ssid = WiFiHelper.getCurrentSsid(context); if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") wifiSsid = ssid }) }
                        else { OutlinedTextField(value = contenido, onValueChange = { contenido = it; qrBitmap = null; qrMatrix = null }, label = { Text(tipos[idx].label) }, placeholder = { Text(tipos[idx].hint) }, leadingIcon = { Icon(tipos[idx].icon, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
                    }
                }
            }
            Text(stringResource(R.string.qr_generator_color_label), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaColorLocalized(stringResource(R.string.settings_theme_light), paletaIdx == 0, Icons.Rounded.LightMode, Modifier.weight(1f)) { paletaIdx = 0 }
                TarjetaColorLocalized(stringResource(R.string.menu_ajustes), paletaIdx != 0 && paletaIdx != PALETAS_MD3.lastIndex, Icons.Rounded.Palette, Modifier.weight(1f)) { showColorDialog = true }
                TarjetaColorLocalized(stringResource(R.string.settings_theme_dark), paletaIdx == PALETAS_MD3.lastIndex, Icons.Rounded.DarkMode, Modifier.weight(1f)) { paletaIdx = PALETAS_MD3.lastIndex }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.qr_generator_logo_label), style = MaterialTheme.typography.bodyMedium); Switch(checked = mostrarLogo, onCheckedChange = { mostrarLogo = it }) } }
            Button(onClick = { generarQR() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.QrCode, null); Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.qr_generator_action_generate), fontWeight = FontWeight.Bold) }
            AnimatedVisibility(visible = qrBitmap != null, enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()) { qrBitmap?.let { bitmap -> ResultadoQRLocalized(bitmap, Color(PALETAS_MD3[paletaIdx].bg)) { QRGeneratorLogic.shareQRCode(context, bitmap) } } }
            if (errorGeneracion != null) { Text(errorGeneracion!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (showColorDialog) { AlertDialog(onDismissRequest = { showColorDialog = false }, confirmButton = { TextButton(onClick = { showColorDialog = false }) { Text(stringResource(R.string.qr_generator_color_dialog_close)) } }, title = { Text(stringResource(R.string.qr_generator_color_dialog_title), fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { val sub = PALETAS_MD3.subList(1, PALETAS_MD3.lastIndex); sub.chunked(4).forEach { fila -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { fila.forEach { paleta -> val idx = PALETAS_MD3.indexOf(paleta); Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(paleta.bg)).border(3.dp, if (paletaIdx == idx) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).clickable { paletaIdx = idx }, contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(paleta.fg))) } } } } } }, shape = RoundedCornerShape(28.dp), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaTipoSymmetricRaw(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(84.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(28.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaColorLocalized(label: String, selected: Boolean, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant), border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeccionWifiInputsLocalized(ssid: String, onSsidChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit, securityIdx: Int, onSecurityChange: (Int) -> Unit, passwordVisible: Boolean, onToggleVisible: () -> Unit, onCurrentWifi: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = ssid, onValueChange = onSsidChange, label = { Text(stringResource(R.string.wifi_ssid_label)) }, placeholder = { Text(stringResource(R.string.wifi_ssid_hint)) }, leadingIcon = { Icon(Icons.Rounded.Wifi, null) }, trailingIcon = { IconButton(onClick = onCurrentWifi) { Icon(Icons.Rounded.MyLocation, null) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { val options = listOf("WPA", "WEP", "OPEN"); options.forEachIndexed { idx, s -> SegmentedButton(selected = securityIdx == idx, onClick = { onSecurityChange(idx) }, shape = SegmentedButtonDefaults.itemShape(idx, 3), label = { Text(s, style = MaterialTheme.typography.labelSmall) }) } }
        if (securityIdx != 2) { OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text(stringResource(R.string.wifi_pass_label)) }, placeholder = { Text(stringResource(R.string.wifi_pass_hint)) }, leadingIcon = { Icon(Icons.Rounded.Lock, null) }, trailingIcon = { IconButton(onClick = onToggleVisible) { Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)) }
    }
}

@Composable
private fun ResultadoQRLocalized(bitmap: Bitmap, bgColor: Color, onShare: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(260.dp).clip(RoundedCornerShape(24.dp)).background(bgColor).padding(16.dp), contentAlignment = Alignment.Center) { Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.fillMaxSize()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.qr_generator_share))
                }
                Button(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.qr_generator_save))
                }
            }
        }
    }
}

private fun renderBitmapFromMatrix(matrix: com.google.zxing.qrcode.encoder.ByteMatrix, fg: Int, bg: Int, size: Int = 1024): Bitmap? {
    return try { val n = matrix.width; val margin = 2; val total = n + margin * 2; val cell = size.toFloat() / total; val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); val canvas = ACanvas(bmp); canvas.drawColor(bg); val paint = APaint(APaint.ANTI_ALIAS_FLAG).apply { color = fg; style = APaint.Style.FILL }; for (x in 0 until n) { for (y in 0 until n) { if (matrix.get(x, y).toInt() == 1) { canvas.drawRect((x + margin) * cell, (y + margin) * cell, (x + margin + 1) * cell, (y + margin + 1) * cell, paint) } } }; bmp } catch (e: Exception) { null }
}

private fun overlayLogoStatic(bitmap: Bitmap, typeIdx: Int, fg: Int, bg: Int): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true); val canvas = ACanvas(result); val sz = result.width.toFloat(); val cx = sz / 2f; val cy = sz / 2f; val logoR = sz * 0.13f
    val bgPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply { color = bg; style = APaint.Style.FILL }
    canvas.drawRoundRect(ARectF(cx - logoR, cy - logoR, cx + logoR, cy + logoR), logoR * 0.4f, logoR * 0.4f, bgPaint)
    val paint = APaint(APaint.ANTI_ALIAS_FLAG).apply { color = fg; style = APaint.Style.STROKE; strokeWidth = logoR * 0.18f; strokeCap = APaint.Cap.ROUND; strokeJoin = APaint.Join.ROUND }
    val fill = APaint(APaint.ANTI_ALIAS_FLAG).apply { color = fg; style = APaint.Style.FILL }; val ic = logoR * 0.65f
    when (typeIdx) {
        2 -> { val dotR = ic * 0.2f; canvas.drawCircle(cx, cy + ic * 0.5f, dotR, fill); canvas.drawArc(ARectF(cx - ic, cy - ic * 0.4f, cx + ic, cy + ic * 1.5f), 220f, 100f, false, paint); canvas.drawArc(ARectF(cx - ic * 0.65f, cy - ic * 0.15f, cx + ic * 0.65f, cy + ic * 1.15f), 220f, 100f, false, paint) }
        1 -> { canvas.drawCircle(cx, cy, ic * 0.9f, paint); canvas.drawOval(ARectF(cx - ic * 0.4f, cy - ic * 0.9f, cx + ic * 0.4f, cy + ic * 0.9f), paint); canvas.drawLine(cx - ic * 0.9f, cy, cx + ic * 0.9f, cy, paint) }
        3 -> { canvas.drawRoundRect(ARectF(cx - ic, cy - ic * 0.65f, cx + ic, cy + ic * 0.65f), ic * 0.2f, ic * 0.2f, paint); val p = APath().apply { moveTo(cx - ic, cy - ic * 0.65f); lineTo(cx, cy); lineTo(cx + ic, cy - ic * 0.65f) }; canvas.drawPath(p, paint) }
        4 -> { val pw = ic * 0.55f; val ph = ic * 0.9f; canvas.drawRoundRect(ARectF(cx - pw, cy - ph, cx + pw, cy + ph), ic * 0.2f, ic * 0.2f, paint); canvas.drawLine(cx - pw * 0.25f, cy - ph + ph * 0.2f, cx + pw * 0.25f, cy - ph + ph * 0.2f, paint); canvas.drawCircle(cx, cy + ph * 0.7f, ic * 0.15f, paint) }
        0 -> { val tp = APaint(APaint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = logoR * 1.3f; textAlign = APaint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }; canvas.drawText("T", cx, cy + logoR * 0.45f, tp) }
    }
    return result
}
