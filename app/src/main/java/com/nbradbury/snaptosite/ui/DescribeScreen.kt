package com.nbradbury.snaptosite.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.UiState

@Composable
fun DescribeScreen(state: UiState, viewModel: SnapViewModel) {
    val talk = rememberWithMicPermission(viewModel::toggleTalk)
    val done = !state.talking && !state.typing && state.transcript.isNotBlank()

    Column(Modifier.fillMaxSize().background(Snap.Surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton("arrow_back", viewModel::back)
            Text("Step 1 of 3", style = Type.Label, color = Snap.OnSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tell us about your business", style = Type.Headline, color = Snap.OnSurface)
                Text(
                    "Say it like you'd tell a new regular: your name, what you serve, when you're open.",
                    style = Type.BodyMedium,
                    color = Snap.OnSurfaceVariant,
                )
            }
            // Typed, or spoken and finished: either way the owner can correct it.
            if (state.typing || done) {
                OutlinedTextField(
                    value = state.transcript,
                    onValueChange = viewModel::editTranscript,
                    label = if (state.typing) null else ({ Text("Your description") }),
                    placeholder = { Text("We're a little café on Alder Street…") },
                    textStyle = Type.Title,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                )
            } else {
                Box(Modifier.heightIn(min = 150.dp)) {
                    if (!state.talking) {
                        Text("Tap the mic and start talking…", style = Type.Title.copy(lineHeight = Type.Title.fontSize * 1.36), color = Snap.Outline)
                    } else {
                        Transcript(state.transcript, listening = true, Type.Title.copy(lineHeight = Type.Title.fontSize * 1.36), Snap.OnSurface)
                    }
                }
            }
            state.facts?.let { facts ->
                val rows = listOfNotNull(
                    facts.name?.let { Triple("storefront", "Name", it) },
                    facts.business?.let { Triple("local_cafe", "Business", it) },
                    facts.where?.let { Triple("location_on", "Where", it) },
                    facts.serves?.let { Triple("restaurant_menu", "Serves", it) },
                    facts.hours?.let { Triple("schedule", "Hours", it) },
                )
                if (rows.isNotEmpty()) {
                    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("What we heard", Modifier.weight(1f))
                            state.factsFrom?.let { WhereTag(it) }
                        }
                        Column(Modifier.background(Snap.SurfaceContainer, RoundedCornerShape(16.dp)).padding(vertical = 4.dp)) {
                            rows.forEach { (icon, label, value) ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { viewModel.editFact(label, value) }.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Symbol(icon, tint = Snap.Primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(label, style = Type.Caption, color = Snap.OnSurfaceVariant)
                                        Text(value, style = Type.Body, color = Snap.OnSurface)
                                    }
                                    Symbol("edit", size = 20.dp, tint = Snap.Outline)
                                }
                            }
                        }
                    }
                }
            }
        }
        when {
            state.typing -> Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Speak instead", talk, kind = ButtonKind.OUTLINED)
                PillButton("Done", { viewModel.doneTalking() }, Modifier.weight(1f), enabled = state.transcript.isNotBlank())
            }
            done -> Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Say it again", rememberWithMicPermission(viewModel::sayAgain), kind = ButtonKind.OUTLINED)
                PillButton("Next: show us around", viewModel::goCapture, Modifier.weight(1f), trailingIcon = "arrow_forward")
            }
            else -> Column(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    if (state.talking) MicRing(80.dp)
                    Surface(onClick = talk, shape = CircleShape, color = Snap.Primary, shadowElevation = 4.dp, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Symbol(if (state.talking) "stop" else "mic", size = 36.dp, fill = true, tint = Color.White)
                        }
                    }
                }
                Text(
                    if (state.talking) "Listening… tap when you're done" else "Tap to talk",
                    style = Type.Label,
                    color = Snap.OnSurfaceVariant,
                )
                if (!state.talking) PillButton("Type instead", viewModel::typeInstead, kind = ButtonKind.TEXT, height = 36.dp)
            }
        }
    }
}

/** Wraps [action] so it first asks for the microphone if the app doesn't have it yet. */
@Composable
fun rememberWithMicPermission(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) action()
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
