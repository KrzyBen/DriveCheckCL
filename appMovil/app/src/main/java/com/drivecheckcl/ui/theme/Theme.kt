package com.drivecheckcl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DriveCheckColorScheme = lightColorScheme(
    primary        = ChileBlue,
    onPrimary      = White,
    secondary      = ChileRed,
    onSecondary    = White,
    background     = BackgroundGray,
    onBackground   = TextPrimary,
    surface        = SurfaceWhite,
    onSurface      = TextPrimary,
    error          = ErrorRed,
    onError        = White,
)

@Composable
fun DriveCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriveCheckColorScheme,
        content     = content
    )
}