package com.colectivobarrios.qrnfctoolkit

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.colectivobarrios.qrnfctoolkit.datos.AppDatabase
import com.colectivobarrios.qrnfctoolkit.datos.HistorialDao
import com.colectivobarrios.qrnfctoolkit.ui.navegacion.*
import com.colectivobarrios.qrnfctoolkit.ui.pantallas.*
import com.colectivobarrios.qrnfctoolkit.ui.theme.QRNFCToolkitTheme
import com.colectivobarrios.qrnfctoolkit.utilidades.PreferenciasManager
import com.colectivobarrios.qrnfctoolkit.utilidades.ThemeMode
import java.util.*

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenciasManager(newBase)
        val locale = Locale.Builder().setLanguage(prefs.language).build()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenciasManager(this)
        val dao = AppDatabase.getDatabase(this).historialDao()
        
        // Sincronizar con AppCompatDelegate para componentes del sistema
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(prefs.language)
        AppCompatDelegate.setApplicationLocales(appLocale)

        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            
            QRNFCToolkitTheme(themeMode = themeMode) {
                AppNavigation(
                    prefs = prefs,
                    dao = dao,
                    onThemeChange = { themeMode = it }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    prefs: PreferenciasManager,
    dao: HistorialDao,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Inicio
    ) {
        composable<Inicio> {
            PantallaInicio(
                onHerramientaClick = { destino ->
                    navController.navigate(destino)
                }
            )
        }
        composable<EscanearQR> {
            PantallaEscanearQR(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable<GenerarQR> {
            PantallaGenerarQR(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable<LeerNFC> {
            PantallaLeerNFC(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable<EscribirNFC> {
            PantallaEscribirNFC(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Historial> {
            PantallaHistorial(
                dao = dao,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Ajustes> {
            PantallaAjustes(
                prefs = prefs,
                onBack = { 
                    navController.popBackStack()
                },
                onThemeChange = { onThemeChange(it) }
            )
        }
    }
}
