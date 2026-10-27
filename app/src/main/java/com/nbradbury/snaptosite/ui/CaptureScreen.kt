package com.nbradbury.snaptosite.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.site.Photo
import com.nbradbury.snaptosite.site.PhotoKind
import kotlinx.coroutines.delay
import java.io.File

private val PhotoKind.icon
    get() = when (this) {
        PhotoKind.STOREFRONT -> "storefront"
        PhotoKind.MENU -> "restaurant_menu"
        PhotoKind.SHOWROOM -> "shopping_bag"
        PhotoKind.FLYER -> "event"
        PhotoKind.LOGO -> "palette"
    }

private val PhotoKind.title
    get() = when (this) {
        PhotoKind.STOREFRONT -> "Storefront"
        PhotoKind.MENU -> "Menu"
        PhotoKind.SHOWROOM -> "Showroom"
        PhotoKind.FLYER -> "Events"
        PhotoKind.LOGO -> "Logo"
    }

/** What to photograph, shown on an empty tile and in the camera. */
private val PhotoKind.hint
    get() = when (this) {
        PhotoKind.STOREFRONT -> "Step back so your sign is in frame"
        PhotoKind.MENU -> "Fit the whole menu or price list in the frame"
        PhotoKind.SHOWROOM -> "Your products on display, or the inside of your shop"
        PhotoKind.FLYER -> "A poster or flyer for an upcoming event"
        PhotoKind.LOGO -> "A cup, sign or card with your logo"
    }

/** What the app will do with the photo, shown once a tile has one. */
private val PhotoKind.use
    get() = when (this) {
        PhotoKind.STOREFRONT -> "Your Storefront page"
        PhotoKind.MENU -> "We'll read it for your Menu page"
        PhotoKind.SHOWROOM -> "Your Showroom page"
        PhotoKind.FLYER -> "We'll read it for your Events page"
        PhotoKind.LOGO -> "Your site's colors and header"
    }

/**
 * Step 2: one kind of photo at a time, picked with pills, showing a placeholder or the chosen
 * photo with buttons to take or choose one. Taking a photo opens the camera for that kind. After
 * launch, snapping a flyer from the Site tab goes straight to the camera.
 */
@Composable
fun CaptureScreen(state: UiState, viewModel: SnapViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhoto(uri = it) }
    }
    val choose = { kind: PhotoKind ->
        viewModel.choosePhoto(kind)
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    if (state.shooting) {
        Camera(state, viewModel) { choose(state.captureMode) }
    } else {
        Picker(state, viewModel, choose)
    }
}

@Composable
private fun Picker(state: UiState, viewModel: SnapViewModel, choose: (PhotoKind) -> Unit) {
    val byKind = state.photos.filter { it.kind in PhotoKind.OFFERED }.associateBy { it.kind }
    val kind = state.captureMode.takeIf { it in PhotoKind.OFFERED } ?: PhotoKind.OFFERED.first()
    // Site colors come from the logo, or the storefront when there's no logo.
    val colorSource = if (PhotoKind.LOGO in byKind) PhotoKind.LOGO else PhotoKind.STOREFRONT
    Column(Modifier.fillMaxSize().background(Snap.CameraBackground)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircleIconButton("close", viewModel::back, tint = Snap.OnCamera)
            Column(Modifier.weight(1f)) {
                Text("Show us your place", style = Type.TitleMedium, color = Snap.OnCamera)
                Text("Step 2 of 3 · ${byKind.size} of ${PhotoKind.OFFERED.size} photos", style = Type.Caption, color = Snap.OnCameraVariant)
            }
            val ready = byKind.isNotEmpty()
            Surface(
                onClick = { if (ready) viewModel.startBuild() },
                shape = RoundedCornerShape(20.dp),
                color = if (ready) Snap.InversePrimary else Snap.OnCamera.copy(alpha = 0.12f),
                contentColor = if (ready) Snap.OnPrimaryContainer else Snap.OnCamera.copy(alpha = 0.38f),
            ) { Text("Done", style = Type.Label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhotoKind.OFFERED.forEach { k ->
                KindPill(k, selected = k == kind, done = k in byKind) { viewModel.selectKind(k) }
            }
        }
        Tile(
            kind = kind,
            photo = byKind[kind],
            swatches = state.colors.swatches.takeIf { kind == colorSource },
            onTake = { viewModel.takePhoto(kind) },
            onChoose = { choose(kind) },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun KindPill(kind: PhotoKind, selected: Boolean, done: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Snap.PrimaryContainer else Color.Transparent,
        contentColor = if (selected) Snap.OnPrimaryContainer else Snap.OnCamera,
        border = BorderStroke(1.dp, if (selected) Snap.PrimaryContainer else Snap.CameraChipBorder),
    ) {
        Row(
            Modifier.heightIn(min = 32.dp).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol(if (done) "check_circle" else kind.icon, size = 18.dp, fill = done)
            Text(kind.title, style = Type.Label)
        }
    }
}

/** One kind's photo, or a placeholder saying what to photograph, with take and choose buttons. */
@Composable
private fun Tile(
    kind: PhotoKind,
    photo: Photo?,
    swatches: List<String>?,
    onTake: () -> Unit,
    onChoose: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF1C1C22))) {
        val bitmap = remember(photo?.file?.path, photo?.file?.lastModified()) {
            photo?.let { BitmapFactory.decodeFile(it.file.path, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap() }
        }
        if (bitmap != null) {
            // Fit, so a whole menu or flyer stays visible.
            Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            // Keeps the label and buttons readable over any photo.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.25f to Color.Transparent, 0.75f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f)),
                ),
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Symbol(kind.icon, size = 48.dp, tint = Snap.CameraChipBorder)
                Text(
                    kind.hint,
                    style = Type.BodyMedium,
                    color = Snap.OnCameraVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (photo != null) {
            Column(Modifier.align(Alignment.TopStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(kind.use, style = Type.Label, color = Color.White)
                swatches?.let { Swatches(it) }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { TileButton("photo_camera", "Take photo", primary = true, onClick = onTake) }
            Box(Modifier.weight(1f)) { TileButton("photo_library", "Choose photo", primary = false, onClick = onChoose) }
        }
    }
}

@Composable
private fun TileButton(icon: String, label: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (primary) Snap.PrimaryContainer else Color.Black.copy(alpha = 0.45f),
        contentColor = if (primary) Snap.OnPrimaryContainer else Color.White,
        border = if (primary) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol(icon, size = 18.dp)
            Text(label, style = Type.Label.copy(fontSize = 13.sp), textAlign = TextAlign.Center)
        }
    }
}

/** The viewfinder for one kind of photo. [onChoose] picks an existing photo instead. */
@Composable
private fun Camera(state: UiState, viewModel: SnapViewModel, onChoose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val kind = state.captureMode
    val edit = state.captureForEdit
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
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CircleIconButton("close", viewModel::back, tint = Snap.OnCamera)
            Column(Modifier.weight(1f)) {
                Text(if (edit) "Snap a flyer or sign" else kind.title, style = Type.TitleMedium, color = Snap.OnCamera)
                Text(if (edit) "We'll add it to your site" else "Step 2 of 3", style = Type.Caption, color = Snap.OnCameraVariant)
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
            Text(
                if (edit) "Point at a flyer or poster" else kind.hint,
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

        Row(Modifier.fillMaxWidth().height(116.dp).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                RoundCameraButton("photo_library", "Choose from your photos", onChoose)
            }
            Surface(onClick = shoot, shape = CircleShape, color = Color.Transparent, border = BorderStroke(4.dp, Color.White), modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Box(Modifier.size(60.dp).background(Color.White, CircleShape)) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                RoundCameraButton("cameraswitch", "Switch camera") {
                    controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundCameraButton(icon: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Snap.CameraButton,
        contentColor = Snap.OnCamera,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) { Box(contentAlignment = Alignment.Center) { Symbol(icon) } }
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
