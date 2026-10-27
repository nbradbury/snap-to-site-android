package com.nbradbury.snaptosite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.nbradbury.snaptosite.R

/** Colors from the Snap-to-Site design: Material 3 tones around WordPress blue. */
object Snap {
    val Primary = Color(0xFF3858E9)
    val PrimaryPressed = Color(0xFF2C47C4)
    val PrimaryContainer = Color(0xFFDEE1FF)
    val OnPrimaryContainer = Color(0xFF00115A)
    val SecondaryContainer = Color(0xFFDFE1F9)
    val OnSecondaryContainer = Color(0xFF181B2C)
    val Surface = Color(0xFFFBF8FF)
    val SurfaceContainer = Color(0xFFF5F2FA)
    val SurfaceContainerHigh = Color(0xFFEFEDF4)
    val SurfaceContainerHighest = Color(0xFFE9E7EF)
    val SurfaceVariant = Color(0xFFE4E1E9)
    val OnSurface = Color(0xFF1B1B21)
    val OnSurfaceVariant = Color(0xFF46464F)
    val Outline = Color(0xFF767680)
    val OutlineVariant = Color(0xFFC7C5D0)
    val InverseSurface = Color(0xFF303036)
    val InverseOnSurface = Color(0xFFF2EFF7)
    val InversePrimary = Color(0xFFBAC3FF)
    val Live = Color(0xFF2E7D32)

    // Camera screen
    val CameraBackground = Color(0xFF121216)
    val OnCamera = Color(0xFFE4E1E9)
    val OnCameraVariant = Color(0xFFC7C5D0)
    val CameraChipBorder = Color(0xFF5B5B66)
    val CameraButton = Color(0xFF2A2A31)
}

val Newsreader = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal),
    Font(R.font.newsreader, FontWeight.Medium),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)

private val Colors = lightColorScheme(
    primary = Snap.Primary,
    onPrimary = Color.White,
    primaryContainer = Snap.PrimaryContainer,
    onPrimaryContainer = Snap.OnPrimaryContainer,
    secondaryContainer = Snap.SecondaryContainer,
    onSecondaryContainer = Snap.OnSecondaryContainer,
    background = Snap.Surface,
    surface = Snap.Surface,
    surfaceContainer = Snap.SurfaceContainer,
    surfaceContainerHigh = Snap.SurfaceContainerHigh,
    surfaceContainerHighest = Snap.SurfaceContainerHighest,
    surfaceVariant = Snap.SurfaceVariant,
    onSurface = Snap.OnSurface,
    onSurfaceVariant = Snap.OnSurfaceVariant,
    outline = Snap.Outline,
    outlineVariant = Snap.OutlineVariant,
    inverseSurface = Snap.InverseSurface,
    inverseOnSurface = Snap.InverseOnSurface,
    inversePrimary = Snap.InversePrimary,
)

@Composable
fun SnapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
