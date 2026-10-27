package com.nbradbury.snaptosite.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.site.PhotoKind
import kotlinx.coroutines.delay
import java.io.File

private val PhotoKind.icon
    get() = when (this) {
        PhotoKind.STOREFRONT -> "storefront"
        PhotoKind.MENU -> "restaurant_menu"
        PhotoKind.LOGO -> "palette"
        PhotoKind.FLYER -> "event"
    }

private val PhotoKind.chip
    get() = when (this) {
        PhotoKind.STOREFRONT -> "Storefront"
        PhotoKind.MENU -> "Menu board"
        PhotoKind.LOGO -> "Logo"
        PhotoKind.FLYER -> "Flyer"
    }

private val PhotoKind.hint
    get() = when (this) {
        PhotoKind.STOREFRONT -> "Step back so your sign is in frame"
        PhotoKind.MENU -> "Fit the whole board in the frame"
        PhotoKind.LOGO -> "A cup, sign or card with your logo"
        PhotoKind.FLYER -> "Any poster or flyer for an event"
    }

@Composable
fun CaptureScreen(state: UiState, viewModel: SnapViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val edit = state.captureForEdit
    val captured = state.capturedKinds
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permission.launch(Manifest.permission.CAMERA) }

    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE) }
    }
    DisposableEffect(hasCamera) {
        if (hasCamera) controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(flash) { if (flash) { delay(140); flash = false } }
    val flashAlpha by animateFloatAsState(if (flash) 0.85f else 0f, tween(140), label = "flash")

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhoto(uri = it) }
    }
    val shoot = {
        if (hasCamera && state.detect == null) {
            flash = true
            val file = File(context.cacheDir, "shot-${System.currentTimeMillis()}.jpg")
            controller.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = viewModel.onPhoto(file = file)
                    override fun onError(exception: ImageCaptureException) = Unit
                },
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Snap.CameraBackground)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircleIconButton("close", viewModel::back, tint = Snap.OnCamera)
            Column(Modifier.weight(1f)) {
                Text(if (edit) "Snap a flyer or sign" else "Show us your place", style = Type.TitleMedium, color = Snap.OnCamera)
                Text(
                    if (edit) "We'll add it to your site" else "Step 2 of 3 · ${captured.size} of 4 shots",
                    style = Type.Caption,
                    color = Snap.OnCameraVariant,
                )
            }
            if (!edit) {
                val ready = state.photos.isNotEmpty()
                Surface(
                    onClick = { if (ready) viewModel.startBuild() },
                    shape = RoundedCornerShape(20.dp),
                    color = if (ready) Snap.InversePrimary else Snap.OnCamera.copy(alpha = 0.12f),
                    contentColor = if (ready) Snap.OnPrimaryContainer else Snap.OnCamera.copy(alpha = 0.38f),
                ) { Text("Done", style = Type.Label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C22)),
        ) {
            if (hasCamera) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { this.controller = controller } },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Snap-to-Site needs the camera to photograph your place.", style = Type.BodyMedium, color = Snap.OnCameraVariant)
                    PillButton("Allow camera", { permission.launch(Manifest.permission.CAMERA) }, kind = ButtonKind.TEXT)
                }
            }
            val mode = if (edit) PhotoKind.FLYER else state.captureMode
            Text(
                if (edit) "Point at a flyer or poster" else mode.hint,
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                color = Color(0xFFF2EFF7),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Brackets()
            state.detect?.let { text ->
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                        .background(Snap.Surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Symbol("check_circle", size = 20.dp, fill = true, tint = Snap.Primary)
                    Text(text, style = Type.Label, color = Snap.OnSurface)
                }
            }
            Box(Modifier.fillMaxSize().alpha(flashAlpha).background(Color.White))
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (edit) {
                ModeChip("event", "Flyer or poster", selected = true, done = false) {}
            } else {
                PhotoKind.entries.forEach { kind ->
                    ModeChip(kind.icon, kind.chip, selected = kind == state.captureMode, done = kind in captured) {
                        viewModel.setCaptureMode(kind)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(116.dp).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            // Pick an existing photo for the current mode, in place of the design's last-shot slot;
            // the header already shows how many shots there are.
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Surface(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = CircleShape,
                    color = Snap.CameraButton,
                    contentColor = Snap.OnCamera,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Choose from your photos" },
                ) { Box(contentAlignment = Alignment.Center) { Symbol("photo_library") } }
            }
            Surface(onClick = shoot, shape = CircleShape, color = Color.Transparent, border = BorderStroke(4.dp, Color.White), modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Box(Modifier.size(60.dp).background(Color.White, CircleShape)) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Surface(
                    onClick = {
                        controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    shape = CircleShape,
                    color = Snap.CameraButton,
                    contentColor = Snap.OnCamera,
                    modifier = Modifier.size(48.dp),
                ) { Box(contentAlignment = Alignment.Center) { Symbol("cameraswitch") } }
            }
        }
    }
}

@Composable
private fun ModeChip(icon: String, label: String, selected: Boolean, done: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Snap.PrimaryContainer else Color.Transparent,
        contentColor = if (selected) Snap.OnPrimaryContainer else Snap.OnCamera,
        border = BorderStroke(1.dp, if (selected) Snap.PrimaryContainer else Snap.CameraChipBorder),
    ) {
        Row(
            Modifier.height(32.dp).padding(start = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol(if (done) "check_circle" else icon, size = 18.dp, fill = done)
            Text(label, style = Type.Label)
        }
    }
}

/** The four white corner brackets framing the viewfinder. */
@Composable
private fun BoxScope.Brackets() {
    val corner = Modifier.size(28.dp)
    val stroke = 3.dp
    Box(Modifier.matchParentSize().padding(horizontal = 36.dp, vertical = 72.dp)) {
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEach { align ->
            Box(
                corner.align(align).drawBehind {
                    val w = stroke.toPx()
                    val left = align == Alignment.TopStart || align == Alignment.BottomStart
                    val top = align == Alignment.TopStart || align == Alignment.TopEnd
                    val x = if (left) w / 2 else size.width - w / 2
                    val y = if (top) w / 2 else size.height - w / 2
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(x, if (top) size.height else 0f)
                        lineTo(x, y + (if (top) 10.dp.toPx() else -10.dp.toPx()))
                        quadraticTo(x, y, x + (if (left) 10.dp.toPx() else -10.dp.toPx()), y)
                        lineTo(if (left) size.width else 0f, y)
                    }
                    drawPath(path, Color.White, style = Stroke(w))
                },
            )
        }
    }
}
