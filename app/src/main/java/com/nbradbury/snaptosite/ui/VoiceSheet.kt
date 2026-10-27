package com.nbradbury.snaptosite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.VoicePhase

/** "What's changed?": say or type a change, see it worked out, and apply or confirm it. */
@Composable
fun VoiceSheet(state: UiState, viewModel: SnapViewModel) {
    val sheet = state.sheet ?: return
    val listen = rememberWithMicPermission(viewModel::sheetListen)
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(remember { MutableInteractionSource() }, null) {
                    if (sheet.phase != VoicePhase.THINKING) viewModel.closeSheet()
                },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp)
                .background(Snap.SurfaceContainer, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(32.dp).height(4.dp).background(Snap.Outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
            when (sheet.phase) {
                VoicePhase.IDLE -> Idle(state, viewModel, listen)
                VoicePhase.LISTENING, VoicePhase.THINKING -> {
                    Text(
                        "“${sheet.text}${if (sheet.phase == VoicePhase.THINKING && sheet.text.isNotEmpty()) "”" else ""}",
                        style = TextStyle(fontSize = 26.sp, lineHeight = 34.sp),
                        color = Snap.OnSurface,
                        modifier = Modifier.heightIn(min = 136.dp).clickable(enabled = sheet.phase == VoicePhase.LISTENING) { viewModel.sheetListen() },
                    )
                    if (sheet.phase == VoicePhase.LISTENING) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Wave()
                            Text("Listening…", style = Type.Label, color = Snap.OnSurfaceVariant)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Spinner()
                            Text(if (state.published) "Updating your site…" else "Updating your preview…", style = Type.Label, color = Snap.OnSurfaceVariant)
                        }
                    }
                }
                VoicePhase.TYPING -> Typing(viewModel)
                VoicePhase.CONFIRM -> Confirm(state, viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.Idle(state: UiState, viewModel: SnapViewModel, listen: () -> Unit) {
    val site = state.site ?: return
    val suggestions = buildList {
        add("event_busy" to "We're closed Thursday for the holiday.")
        if (site.menu.isNotEmpty()) add("restaurant_menu" to "Add an oat milk latte to the menu, five twenty-five.")
        add(
            site.special?.let { "soup_kitchen" to "${it.label} is tomato basil." }
                ?: ("schedule" to "We're open until 5 on Fridays now."),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("What's changed?", style = Type.HeadlineSmall, color = Snap.OnSurface)
        Text("Hours, menu, specials, events. Just say it.", style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { (icon, said) ->
            Surface(
                onClick = { viewModel.submitEdit(said) },
                shape = RoundedCornerShape(8.dp),
                color = Snap.Surface,
                border = BorderStroke(1.dp, Snap.OutlineVariant),
            ) {
                Row(Modifier.padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Symbol(icon, size = 18.dp, tint = Snap.Primary)
                    Text("“$said”", style = Type.BodyMedium, color = Snap.OnSurface)
                }
            }
        }
    }
    Spacer(Modifier.weight(1f, fill = false))
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = listen, shape = CircleShape, color = Snap.Primary, shadowElevation = 4.dp, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Symbol("mic", size = 32.dp, fill = true, tint = Color.White) }
            }
            Text("Tap and talk", style = Type.Label, color = Snap.OnSurfaceVariant)
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            CircleIconButton("keyboard", viewModel::sheetTyping, tint = Snap.OnSurfaceVariant)
        }
    }
}

@Composable
private fun Typing(viewModel: SnapViewModel) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("What's changed?", style = Type.HeadlineSmall, color = Snap.OnSurface)
        Text("Hours, menu, specials, events. Write it like you'd say it.", style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("We're closed Thursday for the holiday.") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        PillButton("Cancel", viewModel::closeSheet, kind = ButtonKind.TEXT, height = 40.dp)
        PillButton("Update", { viewModel.submitEdit(text) }, height = 40.dp, enabled = text.isNotBlank())
    }
}

@Composable
private fun Confirm(state: UiState, viewModel: SnapViewModel) {
    val sheet = state.sheet ?: return
    val nothing = sheet.changes.isEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (nothing) "Nothing to change" else sheet.edit?.title.orEmpty(), style = Type.HeadlineSmall, color = Snap.OnSurface)
        Text(
            if (nothing) "That wouldn't change anything on your site. Try saying it another way." else "Here's what will change on your site.",
            style = Type.BodyMedium,
            color = Snap.OnSurfaceVariant,
        )
    }
    if (!nothing) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .background(Snap.Surface, RoundedCornerShape(16.dp))
                .border(1.dp, Snap.OutlineVariant, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { sheet.changes.forEach { DiffLine(it) } }
    }
    sheet.warning?.let { warning ->
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFFFDAD6), RoundedCornerShape(12.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Symbol("warning", size = 20.dp, tint = Color(0xFF93000A))
            Text(warning, style = Type.BodyMedium, color = Color(0xFF410002))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        PillButton(if (nothing) "OK" else "Cancel", viewModel::closeSheet, kind = ButtonKind.TEXT, height = 40.dp)
        if (!nothing) PillButton("Apply", viewModel::confirmApply, height = 40.dp)
    }
}
