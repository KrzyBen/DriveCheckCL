package com.drivecheckcl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.drivecheckcl.ui.screens.*
import com.drivecheckcl.ui.theme.DriveCheckTheme

enum class Screen {
    LOGIN,
    REGISTER,
    HOME,
    MIS_INFORMES,
    RANKING,
    MI_PROGRESO,
    CONFIGURACION
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            onGoToInformes = { currentScreen = Screen.MIS_INFORMES },
            onGoToRanking  = { currentScreen = Screen.RANKING },
            onGoToProgreso = { currentScreen = Screen.MI_PROGRESO },
            onGoToConfig   = { currentScreen = Screen.CONFIGURACION },
            onGoToDashcam  = { }
        )
        Screen.MIS_INFORMES -> MisInformesScreen(
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.RANKING -> RankingScreen(
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.MI_PROGRESO -> MiProgresoScreen(
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.CONFIGURACION -> ConfiguracionScreen(
            onBack   = { currentScreen = Screen.HOME },
            onLogout = { currentScreen = Screen.LOGIN }
        )
    }
}