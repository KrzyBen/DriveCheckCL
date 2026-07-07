package com.drivecheckcl.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
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
import com.drivecheckcl.ui.theme.*
import com.drivecheckcl.ui.viewmodel.DashcamState
import com.drivecheckcl.ui.viewmodel.DashcamViewModel
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
    val detecciones    by viewModel.detecciones.collectAsStateWithLifecycle()
    val tiempoGrabacion by viewModel.tiempoGrabacion.collectAsStateWithLifecycle()

    // ── Permisos ──────────────────────────────────────────────────────────────
    var tienePermisos by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permisosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        tienePermisos = permisos[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!tienePermisos) {
            permisosLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
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
                .setQualitySelector(QualitySelector.from(Quality.HD))
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
                // Auto-detener a los 30 segundos
                if (segundos >= 30) {
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

            // Indicador de detecciones
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text     = "${detecciones.size} detecciones",
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
            // Última detección
            if (detecciones.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChileRed.copy(alpha = 0.85f))
                        .padding(12.dp)
                ) {
                    Text(
                        text     = "⚠ ${detecciones.last().label}",
                        color    = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

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
                                        if (!event.hasError()) {
                                            viewModel.onGrabacionGuardada(videoFile.absolutePath, context)
                                        } else {
                                            viewModel.onError("Error al guardar el video")
                                        }
                                    }
                                }
                            }
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
                text     = if (isRecording) "Toca para detener (máx. 30s)" else "Toca para grabar",
                color    = White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}