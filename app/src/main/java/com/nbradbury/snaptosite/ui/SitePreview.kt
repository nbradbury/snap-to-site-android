package com.nbradbury.snaptosite.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.nbradbury.snaptosite.UiState
import com.nbradbury.snaptosite.ai.SiteModel
import com.nbradbury.snaptosite.site.EventDates
import com.nbradbury.snaptosite.site.PageKey
import com.nbradbury.snaptosite.site.Photo
import com.nbradbury.snaptosite.site.Section
import com.nbradbury.snaptosite.site.SiteColors
import com.nbradbury.snaptosite.site.SiteLook
import java.net.URLEncoder

private val Ink = Color(0xFF1F2A24)
private val InkMuted = Color(0xFF4A5650)
private val InkFaint = Color(0xFF7A6E5C)

/** The site's look, derived from its colors. */
private class Look(colors: SiteColors) {
    val primary = color(colors.primary)
    val accent = color(colors.accent)
    val background = color(colors.background)
    val card = lerp(background, primary, 0.07f)
    val onPrimary = background
}

/**
 * A phone-sized rendering of the site as it will be published, page by page. It reads the same
 * [SiteModel] that BlockRenderer turns into WordPress blocks.
 */
@Composable
fun SitePreview(state: UiState, site: SiteModel, onPage: (PageKey) -> Unit, modifier: Modifier = Modifier) {
    val look = remember(state.colors) { Look(state.colors) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.page) { scroll.scrollTo(0) }
    val ring = { section: Section -> if (section in state.highlight) Modifier.border(2.dp, Snap.Primary, RoundedCornerShape(4.dp)) else Modifier }

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Snap.OutlineVariant, RoundedCornerShape(20.dp))
            .background(look.background),
    ) {
        Row(
            Modifier.fillMaxWidth().height(32.dp).background(Snap.SurfaceContainerHigh),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("lock", size = 14.dp, tint = Snap.OnSurfaceVariant)
            Text(state.siteUrl?.let { host(it) } ?: "Preview", style = Type.Caption, color = Snap.OnSurfaceVariant)
        }
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(Ink.copy(alpha = 0.12f), Offset(0f, size.height), Offset(size.width, size.height)) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Badge(site, state.photos, look, 30.dp)
                Text(site.businessName, style = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 19.sp), color = Ink, modifier = Modifier.weight(1f))
                Symbol("menu", size = 22.dp, tint = look.primary)
            }
            site.banner?.takeIf { it.isNotBlank() }?.let { banner ->
                Text(
                    banner,
                    style = Type.Label.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = Color(0xFFFFF8F0),
                    modifier = Modifier.fillMaxWidth().then(ring(Section.BANNER)).background(look.accent).padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
            when (state.page) {
                PageKey.HOME -> Home(site, state.photos, look, ring, onPage)
                PageKey.MENU -> Menu(site, look, ring)
                PageKey.EVENTS -> Events(site, look, ring)
                PageKey.VISIT -> Visit(site, look, ring)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .drawBehind { drawLine(Ink.copy(alpha = 0.12f), Offset.Zero, Offset(size.width, 0f)) }
                    .padding(18.dp),
            ) {
                val small = TextStyle(fontSize = 11.sp, lineHeight = 16.sp)
                Text(listOfNotNull(site.businessName, site.contact.address).joinToString(" · "), style = small, color = Color(0xFF6B756F), modifier = Modifier.weight(1f))
                Text("Built with WordPress", style = small, color = Color(0xFF6B756F))
            }
        }
    }
}

@Composable
private fun Home(site: SiteModel, photos: List<Photo>, look: Look, ring: (Section) -> Modifier, onPage: (PageKey) -> Unit) {
    PageColumn(gap = 16) {
        site.hero.photo?.let { photos.getOrNull(it) }?.let { photo ->
            PhotoBox(photo, Modifier.fillMaxWidth().height(180.dp).then(ring(Section.HERO)))
        }
        Text(site.hero.heading, style = PageTitle, color = Ink, modifier = ring(Section.HERO))
        Column(ring(Section.ABOUT), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            site.about.text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEach {
                Text(it.trim(), style = Type.BodyMedium.copy(lineHeight = 21.sp), color = InkMuted)
            }
            site.about.photo?.let { photos.getOrNull(it) }?.let { PhotoBox(it, Modifier.fillMaxWidth().height(160.dp)) }
        }
        site.special?.let { special ->
            Column(
                Modifier.fillMaxWidth().then(ring(Section.SPECIAL)).background(look.card, RoundedCornerShape(6.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(special.label.uppercase(), style = Overline, color = InkFaint)
                Text(special.text, style = TextStyle(fontFamily = Newsreader, fontSize = 20.sp, lineHeight = 26.sp), color = Ink)
            }
        }
        if (site.menu.isNotEmpty()) SiteButton("See the menu", look) { onPage(PageKey.MENU) }
    }
}

@Composable
private fun Menu(site: SiteModel, look: Look, ring: (Section) -> Modifier) {
    PageColumn(gap = 20) {
        Text("Menu", style = PageTitle, color = Ink)
        if (site.menu.isEmpty()) {
            Text("No menu yet. Snap your menu board and it'll appear here.", style = Type.BodyMedium.copy(lineHeight = 21.sp), color = InkMuted)
        }
        Column(ring(Section.MENU), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            site.menu.forEach { category ->
                Column {
                    Text(
                        category.name,
                        style = TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 28.sp),
                        color = look.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    category.items.forEach { item ->
                        DottedRow {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = Type.BodyMedium, color = Ink)
                                item.description?.let { Text(it, style = Type.Caption, color = InkMuted) }
                            }
                            item.price?.let { Text(it, style = Type.BodyMedium, color = Ink) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Events(site: SiteModel, look: Look, ring: (Section) -> Modifier) {
    PageColumn(gap = 12) {
        Text("Events", style = PageTitle, color = Ink, modifier = Modifier.padding(bottom = 8.dp))
        if (site.events.isEmpty()) {
            Text("Nothing on the calendar right now.", style = Type.BodyMedium.copy(lineHeight = 21.sp), color = InkMuted)
        }
        Column(ring(Section.EVENTS), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            site.events.sortedBy { it.date }.forEach { event ->
                Row(
                    Modifier.fillMaxWidth().background(look.card, RoundedCornerShape(6.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(EventDates.month(event.date).orEmpty(), style = Overline, color = look.accent)
                        Text(EventDates.day(event.date) ?: event.date, style = TextStyle(fontFamily = Newsreader, fontSize = 28.sp, lineHeight = 30.sp), color = Ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(event.title, style = TextStyle(fontFamily = Newsreader, fontSize = 19.sp, lineHeight = 24.sp), color = Ink)
                        Text(
                            listOfNotNull(EventDates.weekday(event.date), event.time).joinToString(" · "),
                            style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                            color = InkMuted,
                        )
                        event.description?.let { Text(it, style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp), color = InkMuted, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Visit(site: SiteModel, look: Look, ring: (Section) -> Modifier) {
    val context = LocalContext.current
    PageColumn(gap = 16) {
        Text("Visit", style = PageTitle, color = Ink)
        site.contact.address?.let {
            Text(it, style = TextStyle(fontFamily = Newsreader, fontSize = 20.sp, lineHeight = 26.sp), color = Ink, modifier = ring(Section.CONTACT))
        }
        if (site.hours.isNotEmpty()) {
            Column(ring(Section.HOURS)) {
                Text("HOURS", style = Overline, color = InkFaint, modifier = Modifier.padding(bottom = 4.dp))
                site.hours.forEach { h ->
                    DottedRow {
                        Text(h.day, style = Type.BodyMedium, color = Ink, modifier = Modifier.weight(1f))
                        Text(h.hours, style = Type.BodyMedium, color = if (h.exception) look.accent else Ink)
                    }
                }
            }
        }
        val contact = listOfNotNull(site.contact.phone, site.contact.email)
        if (contact.isNotEmpty()) {
            Column(ring(Section.CONTACT)) {
                contact.forEach { Text(it, style = Type.BodyMedium, color = InkMuted) }
            }
        }
        site.contact.address?.let { address ->
            SiteButton("Get directions", look) {
                val url = "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(address, "UTF-8")
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }
    }
}

private val PageTitle = TextStyle(fontFamily = Newsreader, fontSize = 30.sp, lineHeight = 34.sp)
private val Overline = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)

@Composable
private fun PageColumn(gap: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(gap.dp), content = content)
}

@Composable
private fun DottedRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                drawLine(
                    Ink.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 2.dp.toPx())),
                )
            }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SiteButton(label: String, look: Look, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(2.dp), color = look.primary, contentColor = look.onPrimary) {
        Text(label, style = Type.Label, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
    }
}

@Composable
private fun PhotoBox(photo: Photo, modifier: Modifier) {
    val bitmap = remember(photo.file) {
        BitmapFactory.decodeFile(photo.file.path, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap, null, modifier.clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
    }
}

/** The site's logo in a circle, or its initial when there's no logo, in the app and preview. */
@Composable
fun SiteBadge(site: SiteModel, photos: List<Photo>, colors: SiteColors, size: androidx.compose.ui.unit.Dp) =
    Badge(site, photos, Look(colors), size)

@Composable
private fun Badge(site: SiteModel, photos: List<Photo>, look: Look, size: androidx.compose.ui.unit.Dp) {
    val logo = SiteLook.logoIndex(site, photos)?.let { photos.getOrNull(it) }
    val bitmap = remember(logo?.file?.path, logo?.file?.lastModified()) {
        logo?.let { BitmapFactory.decodeFile(it.file.path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }
    }
    if (bitmap != null) {
        Image(bitmap, "Logo", Modifier.size(size).clip(CircleShape).border(1.dp, Ink.copy(alpha = 0.12f), CircleShape), contentScale = ContentScale.Crop)
        return
    }
    Box(Modifier.size(size).background(look.primary, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            site.businessName.firstOrNull { it.isLetterOrDigit() }?.uppercase().orEmpty(),
            style = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = (size.value * 0.48f).sp),
            color = look.onPrimary,
        )
    }
}

fun host(url: String) = url.substringAfter("://").trimEnd('/')
