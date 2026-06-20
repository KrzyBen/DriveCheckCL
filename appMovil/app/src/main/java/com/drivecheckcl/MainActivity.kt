package com.drivecheckcl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.drivecheckcl.data.local.LocalStorageManager
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

        // Crea estructura de carpetas al arrancar
        LocalStorageManager.inicializarEstructura(this)

        setContent {
            DriveCheckTheme {
                DriveCheckApp()
            }
        }
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
            onBack = { currentScreen = Screen.HOME },
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