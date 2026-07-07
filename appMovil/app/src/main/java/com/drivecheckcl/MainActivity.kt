package com.drivecheckcl

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.drivecheckcl.data.local.LocalStorageManager
import com.drivecheckcl.data.network.RetrofitClient
import com.drivecheckcl.ui.screens.*
import com.drivecheckcl.ui.theme.DriveCheckTheme
import com.drivecheckcl.ui.viewmodel.getSavedUserName
import com.drivecheckcl.ui.viewmodel.isSessionActive

enum class Screen {
    LOGIN,
    REGISTER,
    HOME,
    MIS_INFORMES,
    CREAR_INFORME,
    MIS_ARCHIVOS,
    DASHCAM,
    CONFIGURACION
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${packageName}")
            }
            startActivity(intent)
        } else {
            inicializar()
        }

        setContent {
            DriveCheckTheme {
                DriveCheckApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            Environment.isExternalStorageManager()
        ) {
            inicializar()
        }
    }

    private fun inicializar() {
        LocalStorageManager.inicializarEstructura(this)
        RetrofitClient.init(this)
    }
}

@Composable
fun DriveCheckApp() {
    val context = LocalContext.current
    var currentScreen by remember {
        mutableStateOf(if (isSessionActive(context)) Screen.HOME else Screen.LOGIN)
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { currentScreen = Screen.HOME },
            onGoToRegister = { currentScreen = Screen.REGISTER }
        )
        Screen.REGISTER -> RegisterScreen(
            onRegisterSuccess = { currentScreen = Screen.HOME },
            onGoToLogin       = { currentScreen = Screen.LOGIN }
        )
        Screen.HOME -> HomeScreen(
            userName          = getSavedUserName(context),
            onGoToInformes    = { currentScreen = Screen.MIS_INFORMES },
            onGoToConfig      = { currentScreen = Screen.CONFIGURACION },
            onGoToDashcam     = { currentScreen = Screen.DASHCAM },
            onGoToMisArchivos = { currentScreen = Screen.MIS_ARCHIVOS }
        )
        Screen.MIS_INFORMES -> MisInformesScreen(
            onBack         = { currentScreen = Screen.HOME },
            onCrearInforme = { currentScreen = Screen.CREAR_INFORME }
        )
        Screen.CREAR_INFORME -> CrearInformeScreen(
            onBack    = { currentScreen = Screen.MIS_INFORMES },
            onSuccess = { currentScreen = Screen.MIS_INFORMES }
        )
        Screen.MIS_ARCHIVOS -> MisArchivosScreen(
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.CONFIGURACION -> ConfiguracionScreen(
            onBack   = { currentScreen = Screen.HOME },
            onLogout = { currentScreen = Screen.LOGIN }
        )
        Screen.DASHCAM -> DashcamScreen(
            onBack = { currentScreen = Screen.HOME }
        )
    }
}