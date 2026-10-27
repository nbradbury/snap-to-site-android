package com.nbradbury.snaptosite.ui

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nbradbury.snaptosite.ActivityEntry
import com.nbradbury.snaptosite.Screen
import com.nbradbury.snaptosite.SnapViewModel
import com.nbradbury.snaptosite.Tab
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.site.FieldChange
import com.nbradbury.snaptosite.site.PageKey

/** Preview before publishing, and the app's Site / Activity / Account tabs after. */
@Composable
fun ShellScreen(state: UiState, viewModel: SnapViewModel) {
    val site = state.site ?: return
    val context = LocalContext.current
    val review = state.screen == Screen.REVIEW
    val siteTab = review || state.tab == Tab.SITE

    Column(Modifier.fillMaxSize().background(Snap.Surface)) {
        when {
            review -> Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton("arrow_back", viewModel::back)
                Column(Modifier.weight(1f)) {
                    Text("Preview", style = Type.Title, color = Snap.OnSurface)
                    Text("Not published yet · only you can see this", style = Type.Caption, color = Snap.OnSurfaceVariant)
                }
            }
            state.tab == Tab.SITE -> Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SiteBadge(site, state.photos, state.colors, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(site.businessName, style = Type.Title.copy(fontSize = Type.Title.fontSize * 0.91f), color = Snap.OnSurface, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Snap.Live, CircleShape))
                        Text("Live · ${state.siteUrl?.let { host(it) }.orEmpty()}", style = Type.Caption, color = Snap.OnSurfaceVariant, maxLines = 1)
                    }
                }
                state.siteUrl?.let { url ->
                    CircleIconButton("open_in_new", { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }, tint = Snap.OnSurfaceVariant)
                }
            }
            else -> Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Text(if (state.tab == Tab.ACTIVITY) "Activity" else "Account", style = Type.Title, color = Snap.OnSurface)
            }
        }

        if (siteTab) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PageKey.entries.forEach { page ->
                    val selected = state.page == page
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setPage(page) },
                        label = { Text(page.title, style = Type.Label) },
                        leadingIcon = if (selected) ({ Symbol("check", size = 18.dp) }) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Snap.SecondaryContainer,
                            selectedLabelColor = Snap.OnSecondaryContainer,
                            selectedLeadingIconColor = Snap.OnSecondaryContainer,
                            labelColor = Snap.OnSurfaceVariant,
                        ),
                        border = FilterChipDefaults.filterChipBorder(true, selected, borderColor = Snap.Outline, selectedBorderColor = Snap.SecondaryContainer),
                    )
                }
            }
            SitePreview(state, site, viewModel::setPage, Modifier.weight(1f).padding(horizontal = 16.dp))
            if (review) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("Say a change", viewModel::openVoice, Modifier.weight(1f), kind = ButtonKind.TONAL, leadingIcon = "mic")
                    PillButton("Publish", viewModel::openPublish, Modifier.weight(1f))
                }
            } else {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = viewModel::openSnap,
                        shape = RoundedCornerShape(16.dp),
                        color = Snap.SecondaryContainer,
                        contentColor = Snap.OnSecondaryContainer,
                        modifier = Modifier.size(56.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Symbol("photo_camera") } }
                    Surface(
                        onClick = viewModel::openVoice,
                        shape = RoundedCornerShape(16.dp),
                        color = Snap.Primary,
                        contentColor = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Symbol("mic", fill = true)
                            Text("Say a change", style = Type.TitleMedium)
                        }
                    }
                }
            }
        } else if (state.tab == Tab.ACTIVITY) {
            ActivityTab(state, viewModel, Modifier.weight(1f))
        } else {
            AccountTab(state, viewModel, Modifier.weight(1f))
        }

        if (!review) NavBar(state.tab, viewModel::setTab)
    }
}

@Composable
private fun NavBar(tab: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Snap.SurfaceContainerHigh).heightIn(min = 80.dp)) {
        listOf(Triple(Tab.SITE, "Site", "web"), Triple(Tab.ACTIVITY, "Activity", "history"), Triple(Tab.ACCOUNT, "Account", "account_circle")).forEach { (t, label, icon) ->
            val selected = t == tab
            Column(
                Modifier.weight(1f).clickable { onTab(t) }.padding(top = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.width(64.dp).height(32.dp).background(if (selected) Snap.SecondaryContainer else Color.Transparent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Symbol(icon, fill = selected, tint = if (selected) Snap.OnSurface else Snap.OnSurfaceVariant) }
                Text(label, style = Type.LabelSmall, color = if (selected) Snap.OnSurface else Snap.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActivityTab(state: UiState, viewModel: SnapViewModel, modifier: Modifier) {
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "Every change is saved as a revision on your site. Undo any of them, any time.",
                style = Type.BodyMedium,
                color = Snap.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.activity, key = { it.id }) { entry -> ActivityCard(entry, state.expanded == entry.id, viewModel) }
    }
}

@Composable
private fun ActivityCard(entry: ActivityEntry, open: Boolean, viewModel: SnapViewModel) {
    val canUndo = entry.changes.isNotEmpty()
    Column(Modifier.fillMaxWidth().alpha(if (entry.undone) 0.6f else 1f).background(Snap.SurfaceContainer, RoundedCornerShape(16.dp))) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = canUndo) { viewModel.toggleExpanded(entry.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(Snap.PrimaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Symbol(iconFor(entry), size = 22.dp, tint = Snap.OnPrimaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = Type.Body, color = Snap.OnSurface)
                Text(entry.summary, style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
            }
            Text(
                DateUtils.getRelativeTimeSpanString(entry.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                style = Type.Caption,
                color = Snap.OnSurfaceVariant,
            )
        }
        if (canUndo && open) {
            Column(Modifier.padding(start = 72.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entry.changes.forEach { DiffLine(it) }
            }
        }
        if (canUndo) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.undone) {
                    Text("Undone", style = Type.LabelSmall, color = Snap.OnSurfaceVariant)
                    PillButton("Redo", { viewModel.setUndone(entry.id, false) }, kind = ButtonKind.TEXT, height = 40.dp)
                } else {
                    PillButton(if (open) "Hide change" else "See change", { viewModel.toggleExpanded(entry.id) }, kind = ButtonKind.TEXT, height = 40.dp)
                    PillButton("Undo", { viewModel.setUndone(entry.id, true) }, kind = ButtonKind.OUTLINED, height = 40.dp, leadingIcon = "undo")
                }
            }
        }
    }
}

private fun iconFor(entry: ActivityEntry): String {
    if (entry.changes.isEmpty()) return "public"
    val text = (entry.title + " " + entry.summary).lowercase()
    return when {
        "closed" in text || "holiday" in text -> "event_busy"
        entry.changes.any { it.section == com.nbradbury.snaptosite.site.Section.EVENTS } -> "event"
        entry.changes.any { it.section == com.nbradbury.snaptosite.site.Section.MENU } -> "restaurant_menu"
        entry.changes.any { it.section == com.nbradbury.snaptosite.site.Section.SPECIAL } -> "soup_kitchen"
        entry.changes.any { it.section == com.nbradbury.snaptosite.site.Section.HOURS } -> "schedule"
        else -> "edit_note"
    }
}

/** One field's before → after, with the old value struck through. */
@Composable
fun DiffLine(change: FieldChange) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(change.field, style = Type.LabelSmall.copy(letterSpacing = Type.Caption.letterSpacing), color = Snap.OnSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(change.before, style = Type.BodyMedium.copy(textDecoration = TextDecoration.LineThrough), color = Snap.Outline, modifier = Modifier.weight(1f, fill = false))
            Symbol("arrow_forward", size = 16.dp, tint = Snap.Outline)
            Text(change.after, style = Type.BodyMedium, color = Snap.OnSurface, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun AccountTab(state: UiState, viewModel: SnapViewModel, modifier: Modifier) {
    val context = LocalContext.current
    Column(modifier.verticalScroll(rememberScrollState())) {
        AccountRow(
            "language", state.siteUrl?.let { host(it) } ?: "No site connected", "Connected with an application password",
            onClick = viewModel::connectAnother,
        )
        Divider()
        AccountRow("key", "Claude API key", if (state.hasApiKey) "Saved on this phone" else "Needed to build and edit your site", onClick = viewModel::showApiKey)
        Divider()
        AccountRow(
            "rate_review", "Review changes before applying",
            "Show what will change and wait for Apply, instead of applying with Undo",
            onClick = { viewModel.setConfirmFirst(!state.confirmFirst) },
        ) { SnapSwitch(state.confirmFirst) }
        Divider()
        AccountRow(
            "smartphone", "Quick tasks on this phone",
            if (state.nanoAvailable) "Picking out details while you talk stays on-device. Writing pages uses the cloud."
            else "This phone doesn't have Gemini Nano, so quick tasks use the cloud too.",
            enabled = state.nanoAvailable,
            onClick = { viewModel.setOnDevice(!state.onDevice) },
        ) { SnapSwitch(state.onDevice && state.nanoAvailable, enabled = state.nanoAvailable) }
        Divider()
        state.siteUrl?.let { url ->
            AccountRow(
                "open_in_new", "Open the full WordPress editor", "For anything the app can't do yet",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, (url.trimEnd('/') + "/wp-admin/").toUri())) },
            )
            Divider()
        }
        AccountRow("restart_alt", "Start a new site", "Your current pages stay on WordPress", onClick = viewModel::startOver)
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Snap.OutlineVariant)

@Composable
private fun AccountRow(
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(enabled = enabled, onClick = onClick).padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Symbol(icon, tint = Snap.OnSurfaceVariant, modifier = Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.Body, color = Snap.OnSurface)
            Text(subtitle, style = Type.BodyMedium, color = Snap.OnSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SnapSwitch(checked: Boolean, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Snap.Primary,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = Snap.SurfaceVariant,
            uncheckedBorderColor = Snap.Outline,
            uncheckedThumbColor = Snap.Outline,
        ),
    )
}
