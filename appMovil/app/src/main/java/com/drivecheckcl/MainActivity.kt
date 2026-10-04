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
import com.drivecheckcl.feature.auth.ForgotPasswordScreen
import com.drivecheckcl.feature.auth.LoginScreen
import com.drivecheckcl.feature.auth.RegisterScreen
import com.drivecheckcl.feature.auth.SessionManager
import com.drivecheckcl.feature.dashcam.DashcamScreen
import com.drivecheckcl.feature.files.MyFilesScreen
import com.drivecheckcl.feature.home.HomeScreen
import com.drivecheckcl.feature.reports.CreateReportScreen
import com.drivecheckcl.feature.reports.MyReportsScreen
import com.drivecheckcl.feature.settings.SettingsScreen
import com.drivecheckcl.navigation.Screen
import com.drivecheckcl.theme.DriveCheckTheme

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
        mutableStateOf(if (SessionManager.verificarSesion(context)) Screen.HOME else Screen.LOGIN)
    }

    var loginInfoMessage by remember { mutableStateOf<String?>(null) }

    var videoPreseleccionado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SessionManager.sessionExpired.collect {
            loginInfoMessage = "Tu sesión ha expirado. Inicia sesión nuevamente."
            currentScreen = Screen.LOGIN
        }
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = {
                loginInfoMessage = null
                currentScreen = Screen.HOME
            },
            onGoToRegister = {
                loginInfoMessage = null
                currentScreen = Screen.REGISTER
            },
            onGoToForgotPassword = {
                loginInfoMessage = null
                currentScreen = Screen.RECUPERAR_CONTRASENA
            },
            infoMessage = loginInfoMessage
        )
        Screen.REGISTER -> RegisterScreen(
            onRegisterSuccess = {
                loginInfoMessage = "Cuenta creada con éxito. Inicia sesión para continuar."
                currentScreen = Screen.LOGIN
            },
            onGoToLogin        = { currentScreen = Screen.LOGIN }
        )
        Screen.RECUPERAR_CONTRASENA -> ForgotPasswordScreen(
            onBack           = { currentScreen = Screen.LOGIN },
            onPasswordReset  = {
                loginInfoMessage = "Contraseña actualizada. Inicia sesión con tu nueva contraseña."
                currentScreen = Screen.LOGIN
            }
        )
        Screen.HOME -> HomeScreen(
            userName          = SessionManager.obtenerNombreUsuario(context),
            onGoToInformes    = { currentScreen = Screen.MIS_INFORMES },
            onGoToConfig      = { currentScreen = Screen.CONFIGURACION },
            onGoToDashcam     = { currentScreen = Screen.DASHCAM },
            onGoToMisArchivos = { currentScreen = Screen.MIS_ARCHIVOS }
        )
        Screen.MIS_INFORMES -> MyReportsScreen(
            onBack         = { currentScreen = Screen.HOME },
            onCrearInforme = { currentScreen = Screen.CREAR_INFORME }
        )
        Screen.CREAR_INFORME -> CreateReportScreen(
            onBack    = { videoPreseleccionado = null; currentScreen = Screen.MIS_INFORMES },
            onSuccess = { videoPreseleccionado = null; currentScreen = Screen.MIS_INFORMES },
            videoPreseleccionado = videoPreseleccionado
        )
        Screen.MIS_ARCHIVOS -> MyFilesScreen(
            onBack = { currentScreen = Screen.HOME },
            onCrearReporte = { videoPath ->
                videoPreseleccionado = videoPath
                currentScreen = Screen.CREAR_INFORME
            }
        )
        Screen.CONFIGURACION -> SettingsScreen(
            onBack   = { currentScreen = Screen.HOME },
            onLogout = { currentScreen = Screen.LOGIN }
        )
        Screen.DASHCAM -> DashcamScreen(
            onBack = { currentScreen = Screen.HOME }
        )
    }
}
