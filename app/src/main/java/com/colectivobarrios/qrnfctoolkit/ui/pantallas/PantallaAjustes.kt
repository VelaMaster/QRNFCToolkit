package com.colectivobarrios.qrnfctoolkit.ui.pantallas

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.colectivobarrios.qrnfctoolkit.R
import com.colectivobarrios.qrnfctoolkit.utilidades.PreferenciasManager
import com.colectivobarrios.qrnfctoolkit.utilidades.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(
    prefs: PreferenciasManager,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var currentTheme by remember { mutableStateOf(prefs.themeMode) }
    var isChangingLanguage by remember { mutableStateOf(false) }
    
    val currentLang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: prefs.language

    if (isChangingLanguage) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.loading_language), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.menu_ajustes), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaTemaSmall(label = stringResource(R.string.settings_theme_light), icon = Icons.Rounded.LightMode, selected = currentTheme == ThemeMode.LIGHT, modifier = Modifier.weight(1f)) { currentTheme = ThemeMode.LIGHT; prefs.themeMode = ThemeMode.LIGHT; onThemeChange(ThemeMode.LIGHT) }
                    TarjetaTemaSmall(label = stringResource(R.string.settings_theme_system), icon = Icons.Rounded.SettingsSuggest, selected = currentTheme == ThemeMode.SYSTEM, modifier = Modifier.weight(1f)) { currentTheme = ThemeMode.SYSTEM; prefs.themeMode = ThemeMode.SYSTEM; onThemeChange(ThemeMode.SYSTEM) }
                    TarjetaTemaSmall(label = stringResource(R.string.settings_theme_dark), icon = Icons.Rounded.DarkMode, selected = currentTheme == ThemeMode.DARK, modifier = Modifier.weight(1f)) { currentTheme = ThemeMode.DARK; prefs.themeMode = ThemeMode.DARK; onThemeChange(ThemeMode.DARK) }
                }

                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        LanguageItem(label = stringResource(R.string.lang_es), selected = currentLang == "es", icon = "🇪🇸") { if (currentLang != "es") { scope.launch { isChangingLanguage = true; delay(600); changeLanguage("es", prefs, context as Activity) } } }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        LanguageItem(label = stringResource(R.string.lang_en), selected = currentLang == "en", icon = "🇺🇸") { if (currentLang != "en") { scope.launch { isChangingLanguage = true; delay(600); changeLanguage("en", prefs, context as Activity) } } }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        LanguageItem(label = stringResource(R.string.lang_fr), selected = currentLang == "fr", icon = "🇫🇷") { if (currentLang != "fr") { scope.launch { isChangingLanguage = true; delay(600); changeLanguage("fr", prefs, context as Activity) } } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaTemaSmall(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant), border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LanguageItem(label: String, selected: Boolean, icon: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) { Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

private fun changeLanguage(code: String, prefs: PreferenciasManager, activity: Activity) {
    prefs.language = code
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(code)
    AppCompatDelegate.setApplicationLocales(appLocale)
    activity.recreate()
}
