package com.nbradbury.snaptosite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nbradbury.snaptosite.SnapViewModel

@Composable
fun WelcomeScreen(viewModel: SnapViewModel) {
    Column(Modifier.fillMaxSize().background(Snap.Surface).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier.size(64.dp).background(Snap.Primary, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Symbol("photo_camera", size = 34.dp, fill = true, tint = Color.White) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Snap-to-Site", style = Type.Label, color = Snap.Primary)
                Text("Your website, built from your phone.", style = Type.Display, color = Snap.OnSurface)
                Text(
                    "Describe your business, show us around, and get a WordPress site you can update just by saying what changed.",
                    style = Type.Body,
                    color = Snap.OnSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Feature("mic", "Tell us about your business")
                Feature("photo_camera", "Point the camera at your place")
                Feature("web", "Review your site and publish")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Get started", viewModel::getStarted, Modifier.fillMaxWidth(), height = 56.dp, textStyle = Type.TitleMedium)
            Text(
                "Publishes to your own WordPress site",
                style = Type.Caption,
                color = Snap.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Feature(icon: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Snap.PrimaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Symbol(icon, size = 22.dp, tint = Snap.OnPrimaryContainer)
        }
        Text(text, style = Type.Body, color = Snap.OnSurface)
    }
}
