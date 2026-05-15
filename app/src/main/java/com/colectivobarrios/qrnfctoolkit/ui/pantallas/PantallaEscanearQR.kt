package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.colectivobarrios.qrnfctoolkit.utilidades.UrlValidator
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.datos.HistorialEntity
import com.colectivobarrios.qrnfctoolkit.ui.componentes.ScannerOverlay
import com.colectivobarrios.qrnfctoolkit.utilidades.WiFiHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscanearQR(
    dao: HistorialDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_scanner_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (permissionState.status.isGranted) {
                ScannerView(dao = dao, onBack = onBack, contentPadding = padding)
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.qr_scanner_permission_denied), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { permissionState.launchPermissionRequest() }) {
                            Text(stringResource(R.string.qr_scanner_request_permission))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerView(
    dao: HistorialDao,
    onBack: () -> Unit, 
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    val haptic = LocalHapticFeedback.current
    
    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val barcodeScannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(barcodeScannerOptions) }

    LaunchedEffect(cameraController) {
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(context)
            ) { result ->
                val barcodes = result?.getValue(barcodeScanner)
                if (!barcodes.isNullOrEmpty() && !showBottomSheet) {
                    val rawValue = barcodes.first().rawValue
                    if (rawValue != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastScannedValue = rawValue
                        showBottomSheet = true
                        
                        // LOG TO HISTORY
                        scope.launch {
                            dao.insert(HistorialEntity(
                                tipo = "QR_SCAN",
                                contenido = rawValue,
                                timestamp = System.currentTimeMillis()
                            ))
                        }
                    }
                }
            }
        )
        cameraController.bindToLifecycle(lifecycleOwner)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            cornerColor = MaterialTheme.colorScheme.primaryContainer,
            laserColor = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.qr_scanner_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (showBottomSheet && lastScannedValue != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false 
                lastScannedValue = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ResultContent(
                value = lastScannedValue!!,
                onDismiss = {
                    showBottomSheet = false
                    lastScannedValue = null
                }
            )
        }
    }
}

@Composable
fun ResultContent(value: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val wifiConfig = remember(value) { WiFiHelper.parseWiFiQR(value) }
    // Solo reconocer como URL si tiene esquema http/https valido (seguridad contra esquemas peligrosos)
    val isUrl = remember(value) { UrlValidator.looksLikeUrl(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when {
                wifiConfig != null -> Icons.Rounded.Wifi
                isUrl -> Icons.Rounded.Language
                else -> Icons.Rounded.Description
            },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when {
                wifiConfig != null -> stringResource(R.string.qr_scanner_result_wifi)
                isUrl -> stringResource(R.string.qr_scanner_result_url)
                else -> stringResource(R.string.qr_scanner_result_text)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (wifiConfig != null) "SSID: ${wifiConfig.ssid}" else value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (wifiConfig != null) {
                    WiFiHelper.connectToWiFi(context, wifiConfig)
                } else if (isUrl) {
                    // Validar esquema antes de abrir: protege contra QR maliciosos
                    // con esquemas como javascript:, file://, intent://, etc.
                    if (UrlValidator.isSafeUrl(value)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(value))
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "URL no segura bloqueada: esquema no permitido",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Copiar al portapapeles
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("QR Content", value)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.qr_scanner_copied), Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = when {
                    wifiConfig != null -> Icons.Rounded.WifiCalling3
                    isUrl -> Icons.AutoMirrored.Rounded.OpenInNew
                    else -> Icons.Rounded.ContentCopy
                },
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                when {
                    wifiConfig != null -> stringResource(R.string.qr_scanner_action_connect)
                    isUrl -> stringResource(R.string.qr_scanner_action_open)
                    else -> stringResource(R.string.qr_scanner_action_copy)
                }
            )
        }
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.qr_scanner_scan_another))
        }
    }
}
