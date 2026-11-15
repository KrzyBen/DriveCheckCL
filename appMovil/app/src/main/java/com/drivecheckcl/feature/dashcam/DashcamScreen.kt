package com.drivecheckcl.feature.dashcam

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drivecheckcl.theme.*
import com.drivecheckcl.util.AppPreferences
import com.drivecheckcl.util.Constantes
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors

@Composable
fun DashcamScreen(
    onBack: () -> Unit,
    viewModel: DashcamViewModel = viewModel()
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val state          by viewModel.state.collectAsStateWithLifecycle()
    val tiempoGrabacion by viewModel.tiempoGrabacion.collectAsStateWithLifecycle()

    // ── Permisos ──────────────────────────────────────────────────────────────
    var tienePermisos by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // Ubicación: es opcional. Sin ella el video se graba igual, sin coordenadas.
    var ubicacionPermitida by remember {
        mutableStateOf(ClipMetadataRecorder.tienePermiso(context))
    }
    val metadatos = remember { ClipMetadataRecorder(context) }

    val ubicacionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        ubicacionPermitida = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val permisosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        tienePermisos = permisos[Manifest.permission.CAMERA] == true
        // Se pide después de cámara y micrófono para no mostrar dos diálogos a la vez.
        if (tienePermisos && !ubicacionPermitida) {
            ubicacionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!tienePermisos) {
            permisosLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        } else if (!ubicacionPermitida) {
            ubicacionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Al salir de la pantalla se deja de escuchar el GPS.
    DisposableEffect(Unit) {
        onDispose { metadatos.liberar() }
    }

    // ── CameraX setup ─────────────────────────────────────────────────────────
    val previewView     = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording    by remember { mutableStateOf<Recording?>(null) }
    val cameraExecutor  = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(tienePermisos) {
        if (!tienePermisos) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(AppPreferences.obtenerCalidadGrabacion(context).cameraxQuality)
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                viewModel.onError("Error al iniciar cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ── Timer de grabación ────────────────────────────────────────────────────
    LaunchedEffect(state) {
        if (state is DashcamState.Recording) {
            var segundos = 0
            while (state is DashcamState.Recording) {
                delay(1000)
                segundos++
                viewModel.onTiempoActualizado(segundos)
                // Auto-detener al llegar a la duración máxima del clip
                if (segundos >= Constantes.DURACION_MAX_CLIP_SEGUNDOS) {
                    recording?.stop()
                    break
                }
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Preview de cámara
        if (tienePermisos) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Se necesitan permisos de cámara", color = White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        permisosLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    }) { Text("Conceder permisos") }
                }
            }
        }

        // ── Header overlay ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                recording?.stop()
                viewModel.resetear()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, null, tint = White, modifier = Modifier.size(28.dp))
            }

            // Timer
            if (state is DashcamState.Recording) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ChileRed.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    val minutos  = tiempoGrabacion / 60
                    val segundos = tiempoGrabacion % 60
                    Text(
                        text      = "● %02d:%02d".format(minutos, segundos),
                        color     = White,
                        fontSize  = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Estado del GPS
            val textoGps = when {
                !ubicacionPermitida -> "Sin ubicación"
                else -> when (metadatos.estado) {
                    EstadoGps.SIN_PERMISO   -> "Sin ubicación"
                    EstadoGps.EN_ESPERA     -> "GPS en espera"
                    EstadoGps.BUSCANDO      -> "Buscando GPS…"
                    EstadoGps.ACTIVO        -> "GPS activo"
                    EstadoGps.NO_DISPONIBLE -> "GPS no disponible"
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text     = textoGps,
                    color    = White,
                    fontSize = 12.sp
                )
            }
        }

        // ── Panel inferior ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Estado guardado
            if (state is DashcamState.Saved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessGreen.copy(alpha = 0.85f))
                        .padding(12.dp)
                ) {
                    Text("✓ Video guardado correctamente", color = White, fontSize = 13.sp)
                }
            }

            // Error
            if (state is DashcamState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChileRed.copy(alpha = 0.85f))
                        .padding(12.dp)
                ) {
                    Text((state as DashcamState.Error).mensaje, color = White, fontSize = 13.sp)
                }
            }

            // Botón grabar / detener
            val isRecording = state is DashcamState.Recording
            Button(
                onClick = {
                    if (!isRecording) {
                        // Iniciar grabación
                        val videoDir  = viewModel.getVideoDir(context)
                        val videoFile = File(videoDir, viewModel.generarNombreVideo())
                        val outputOptions = FileOutputOptions.Builder(videoFile).build()
                        metadatos.iniciar()
                        recording = videoCapture?.output
                            ?.prepareRecording(context, outputOptions)
                            ?.apply {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) withAudioEnabled()
                            }
                            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start  -> viewModel.onGrabacionIniciada()
                                    is VideoRecordEvent.Finalize -> {
                                        val datosClip = metadatos.detener()
                                        if (!event.hasError()) {
                                            metadatos.guardarJson(videoFile, datosClip)
                                            viewModel.onGrabacionGuardada(videoFile.absolutePath, context)
                                        } else {
                                            viewModel.onError("Error al guardar el video")
                                        }
                                    }
                                }
                            }
                        // Si la cámara no estaba lista no se grabó nada: se corta el GPS.
                        if (recording == null) metadatos.liberar()
                    } else {
                        // Detener grabación
                        recording?.stop()
                        recording = null
                    }
                },
                modifier = Modifier.size(72.dp),
                shape    = RoundedCornerShape(36.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) ChileRed else White
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint   = if (isRecording) White else ChileRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text     = if (isRecording) "Toca para detener (máx. ${Constantes.DURACION_MAX_CLIP_SEGUNDOS}s)" else "Toca para grabar",
                color    = White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}
