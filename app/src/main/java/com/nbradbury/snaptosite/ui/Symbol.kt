package com.nbradbury.snaptosite.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nbradbury.snaptosite.R

/**
 * Material Symbols Rounded, from a font subset to the icons below by `tools/subset_symbols.py`.
 * To add an icon, add it to that script's list, rerun it, and add its codepoint here.
 */
private val CODEPOINTS = mapOf(
    "photo_camera" to '',
    "mic" to '',
    "web" to '',
    "arrow_back" to '',
    "smartphone" to '',
    "edit" to '',
    "stop" to '',
    "arrow_forward" to '',
    "close" to '',
    "check_circle" to '',
    "storefront" to '',
    "restaurant_menu" to '',
    "palette" to '',
    "event" to '',
    "cameraswitch" to '',
    "photo_library" to '',
    "image" to '',
    "check" to '',
    "remove" to '',
    "record_voice_over" to '',
    "edit_note" to '',
    "cloud" to '',
    "open_in_new" to '',
    "lock" to '',
    "menu" to '',
    "history" to '',
    "account_circle" to '',
    "undo" to '',
    "public" to '',
    "language" to '',
    "keyboard" to '',
    "event_busy" to '',
    "soup_kitchen" to '',
    "local_cafe" to '',
    "location_on" to '',
    "schedule" to '',
    "restart_alt" to '',
    "key" to '',
    "logout" to '',
    "rate_review" to '',
    "chevron_right" to '',
    "error" to '',
    "call" to '',
    "mail" to '',
    "directions" to '',
    "info" to '',
    "send" to '',
    "auto_awesome" to '',
    "warning" to '',
)

private val outlined = FontFamily(
    Font(R.font.material_symbols_rounded, variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", 0f))),
)
private val filled = FontFamily(
    Font(R.font.material_symbols_rounded, variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", 1f))),
)

@Composable
fun Symbol(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    fill: Boolean = false,
    tint: Color = LocalContentColor.current,
) {
    val glyph = remember(name) { CODEPOINTS[name]?.toString() ?: error("Icon $name isn't in the subset font") }
    // Icons are sized in dp, so they don't grow with the user's font scale.
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Text(
        text = glyph,
        modifier = modifier,
        color = tint,
        style = TextStyle(
            fontFamily = if (fill) filled else outlined,
            fontSize = fontSize,
            lineHeight = fontSize,
            textAlign = TextAlign.Center,
        ),
    )
}
