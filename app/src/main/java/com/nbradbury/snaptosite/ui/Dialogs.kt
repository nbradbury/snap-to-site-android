package com.nbradbury.snaptosite.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.nbradbury.snaptosite.Dialog as SnapDialog
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.UiState

@Composable
fun SnapDialogs(state: UiState, viewModel: SnapViewModel) {
    when (val dialog = state.dialog) {
        is SnapDialog.ApiKey -> ApiKeyDialog(viewModel)
        SnapDialog.Publish -> PublishDialog(state, viewModel)
        is SnapDialog.Connect -> ConnectDialog(dialog.publishAfter, state.busy, viewModel)
        SnapDialog.Publishing -> DialogCard({}, dismissible = false) {
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Spinner()
                Text("Publishing ${state.site?.businessName.orEmpty()}…", style = Type.Body, color = Snap.OnSurface)
            }
        }
        is SnapDialog.EditFact -> EditFactDialog(dialog.field, dialog.value, viewModel)
        null -> Unit
    }
    state.error?.let { error ->
        DialogCard(viewModel::dismissError) {
            Symbol("info", tint = Snap.Primary, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(error, style = Type.BodyMedium, color = Snap.OnSurface)
            Buttons { PillButton("OK", viewModel::dismissError, kind = ButtonKind.TEXT, height = 40.dp) }
        }
    }
}

@Composable
private fun DialogCard(onDismiss: () -> Unit, dismissible: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismiss, DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)) {
        Column(
            Modifier.width(312.dp).background(Snap.SurfaceContainerHighest, RoundedCornerShape(28.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Buttons(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) { content() }
}

@Composable
private fun Title(icon: String, text: String) {
    Symbol(icon, tint = Snap.Primary, modifier = Modifier.fillMaxWidth())
    Text(text, style = Type.HeadlineSmall, color = Snap.OnSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ApiKeyDialog(viewModel: SnapViewModel) {
    var key by remember { mutableStateOf("") }
    DialogCard(viewModel::dismissDialog) {
        Title("key", "Add your Claude API key")
        Text(
            "Claude reads what you say and the photos you take, and writes your pages. The key stays on this phone.",
            style = Type.BodyMedium,
            color = Snap.OnSurfaceVariant,
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Buttons {
            PillButton("Cancel", viewModel::dismissDialog, kind = ButtonKind.TEXT, height = 40.dp)
            PillButton("Save", { viewModel.saveApiKey(key) }, kind = ButtonKind.TEXT, height = 40.dp, enabled = key.isNotBlank())
        }
    }
}

@Composable
private fun PublishDialog(state: UiState, viewModel: SnapViewModel) {
    var other by remember { mutableStateOf(false) }
    DialogCard(viewModel::dismissDialog) {
        Title("public", "Publish your site?")
        Text("${state.site?.businessName.orEmpty()} will go live at:", style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Radio(state.siteUrl?.let { host(it) }.orEmpty(), "Connected", selected = !other) { other = false }
            Radio("A different WordPress site", "Connect it in your browser", selected = other) { other = true }
        }
        Buttons {
            PillButton("Cancel", viewModel::dismissDialog, kind = ButtonKind.TEXT, height = 40.dp)
            PillButton("Publish", { if (other) viewModel.connectAnother() else viewModel.publish() }, kind = ButtonKind.TEXT, height = 40.dp)
        }
    }
}

@Composable
private fun ConnectDialog(publishAfter: Boolean, busy: Boolean, viewModel: SnapViewModel) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    DialogCard(viewModel::dismissDialog) {
        Title("language", "Connect your WordPress site")
        Text(
            "You'll approve Snap-to-Site in your browser. It gets an application password you can revoke from your WordPress profile any time.",
            style = Type.BodyMedium,
            color = Snap.OnSurfaceVariant,
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Site address") },
            placeholder = { Text("example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Buttons {
            if (busy) Spinner()
            PillButton("Cancel", viewModel::dismissDialog, kind = ButtonKind.TEXT, height = 40.dp)
            PillButton(
                "Continue",
                { viewModel.connect(url, publishAfter) { context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) } },
                kind = ButtonKind.TEXT,
                height = 40.dp,
                enabled = url.isNotBlank() && !busy,
            )
        }
    }
}

@Composable
private fun EditFactDialog(field: String, value: String, viewModel: SnapViewModel) {
    var text by remember { mutableStateOf(value) }
    DialogCard(viewModel::dismissDialog) {
        Text(field, style = Type.HeadlineSmall, color = Snap.OnSurface)
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        Buttons {
            PillButton("Cancel", viewModel::dismissDialog, kind = ButtonKind.TEXT, height = 40.dp)
            PillButton("Save", { viewModel.saveFact(field, text) }, kind = ButtonKind.TEXT, height = 40.dp)
        }
    }
}

@Composable
private fun Radio(label: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).border(2.dp, if (selected) Snap.Primary else Snap.OnSurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(10.dp).background(Snap.Primary, CircleShape)) }
        Column {
            Text(label, style = Type.BodyMedium, color = Snap.OnSurface)
            Text(sub, style = Type.Caption, color = Snap.OnSurfaceVariant)
        }
    }
}
