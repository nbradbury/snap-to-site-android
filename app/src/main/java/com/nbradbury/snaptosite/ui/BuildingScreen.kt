package com.nbradbury.snaptosite.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nbradbury.snaptosite.BuildStep
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.StepState
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.site.SiteColors

@Composable
fun BuildingScreen(state: UiState, viewModel: SnapViewModel) {
    val name = state.site?.businessName ?: state.facts?.name ?: "your site"
    val finished = state.steps.count { it.state == StepState.DONE || it.state == StepState.SKIPPED }
    val progress by animateFloatAsState(
        if (state.buildDone) 1f else finished.toFloat() / state.steps.size.coerceAtLeast(1),
        tween(600),
        label = "progress",
    )

    Column(Modifier.fillMaxSize().background(Snap.Surface)) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step 3 of 3", style = Type.Label, color = Snap.Primary)
            Text(if (state.buildDone) "$name is ready" else "Building $name", style = Type.Headline, color = Snap.OnSurface)
            Text(
                when {
                    state.buildError != null -> state.buildError
                    state.buildDone -> "${viewModel.pageKeys(state.site!!).size} pages, your details, your colors. Take a look before anyone else does."
                    else -> "This takes about a minute. Nothing goes live until you say so."
                },
                style = Type.BodyMedium,
                color = if (state.buildError != null) Color(0xFFBA1A1A) else Snap.OnSurfaceVariant,
            )
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(4.dp).background(Snap.PrimaryContainer, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Snap.Primary, RoundedCornerShape(2.dp)))
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
            items(state.steps) { step -> StepRow(step) }
        }
        when {
            state.buildError != null -> PillButton(
                "Try again", viewModel::startBuild,
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                height = 56.dp, textStyle = Type.TitleMedium,
            )
            state.buildDone -> PillButton(
                "Preview your site", viewModel::goReview,
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                height = 56.dp, textStyle = Type.TitleMedium,
            )
        }
    }
}

@Composable
private fun StepRow(step: BuildStep) {
    val pending = step.state == StepState.PENDING
    Row(
        Modifier.fillMaxWidth().alpha(if (pending) 0.45f else 1f).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            when (step.state) {
                StepState.DONE -> Box(Modifier.size(32.dp).background(Snap.Primary, CircleShape), contentAlignment = Alignment.Center) {
                    Symbol("check", size = 20.dp, tint = Color.White)
                }
                StepState.SKIPPED -> Box(Modifier.size(32.dp).background(Snap.SurfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Symbol("remove", size = 20.dp, tint = Snap.OnSurfaceVariant)
                }
                StepState.ACTIVE -> Spinner()
                StepState.PENDING -> Symbol(step.icon, tint = Snap.Outline)
            }
        }
        Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(step.label, style = Type.Body, color = Snap.OnSurface)
            val sub = when (step.state) {
                StepState.DONE, StepState.SKIPPED -> step.detail
                StepState.ACTIVE -> "Working…"
                StepState.PENDING -> ""
            }
            if (sub.isNotEmpty()) Text(sub, style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
            if (step.state == StepState.DONE) step.swatches?.let { Swatches(it) }
        }
        if (step.where != null && step.state != StepState.SKIPPED) {
            Box(Modifier.padding(top = 4.dp)) { WhereTag(step.where) }
        }
    }
}

@Composable
fun Swatches(colors: List<String>) {
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { hex ->
            Box(
                Modifier
                    .size(24.dp)
                    .background(color(hex), CircleShape)
                    .border(1.dp, Snap.OutlineVariant, CircleShape),
            )
        }
    }
}

fun color(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
    .getOrDefault(Color(android.graphics.Color.parseColor(SiteColors.DEFAULT.primary)))
