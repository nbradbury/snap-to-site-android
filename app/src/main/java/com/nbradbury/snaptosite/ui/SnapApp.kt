package com.nbradbury.snaptosite.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nbradbury.snaptosite.Screen
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.Tab

@Composable
fun SnapApp(viewModel: SnapViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = state.screen == Screen.CAPTURE

    BackHandler(enabled = state.sheet != null) { viewModel.closeSheet() }
    BackHandler(enabled = state.sheet == null && state.screen in setOf(Screen.DESCRIBE, Screen.CAPTURE, Screen.BUILDING, Screen.REVIEW)) { viewModel.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Snap.CameraBackground else Snap.Surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (state.screen) {
            Screen.LOADING -> Unit
            Screen.WELCOME -> WelcomeScreen(viewModel)
            Screen.DESCRIBE -> DescribeScreen(state, viewModel)
            Screen.CAPTURE -> CaptureScreen(state, viewModel)
            Screen.BUILDING -> BuildingScreen(state, viewModel)
            Screen.REVIEW, Screen.APP -> ShellScreen(state, viewModel)
        }

        state.snack?.let { snack ->
            val bottom = when {
                state.screen == Screen.REVIEW -> 92.dp
                state.screen == Screen.APP && state.tab == Tab.SITE -> 172.dp
                else -> 96.dp
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = bottom)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .shadow(6.dp, RoundedCornerShape(4.dp))
                    .background(Snap.InverseSurface, RoundedCornerShape(4.dp))
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(snack.text, style = Type.BodyMedium, color = Snap.InverseOnSurface, modifier = Modifier.weight(1f))
                if (snack.undoId != null) {
                    TextButton(onClick = viewModel::snackUndo) { Text("Undo", style = Type.Label, color = Snap.InversePrimary) }
                }
            }
        }

        VoiceSheet(state, viewModel)
    }
    SnapDialogs(state, viewModel)
}
