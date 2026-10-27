package com.nbradbury.snaptosite.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbradbury.snaptosite.Where

object Type {
    val Display = TextStyle(fontSize = 36.sp, lineHeight = 44.sp)
    val Headline = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)
    val HeadlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp)
    val Title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
    val TitleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val BodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val Label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)
    val LabelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
}

enum class ButtonKind { FILLED, TONAL, OUTLINED, TEXT }

/** The design's pill buttons: filled blue, tonal lavender, outlined, or text. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.FILLED,
    height: Dp = 48.dp,
    enabled: Boolean = true,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
    iconFill: Boolean = false,
    textStyle: TextStyle = Type.Label,
) {
    val shape = RoundedCornerShape(height / 2)
    val padding = PaddingValues(horizontal = if (kind == ButtonKind.TEXT) 12.dp else 20.dp)
    val content: @Composable RowScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.let { Symbol(it, size = 20.dp, fill = iconFill) }
            Text(text, style = textStyle)
            trailingIcon?.let { Symbol(it, size = 18.dp) }
        }
    }
    val mod = modifier.height(height)
    when (kind) {
        ButtonKind.FILLED -> Button(onClick, mod, enabled, shape, contentPadding = padding, content = content)
        ButtonKind.TONAL -> Button(
            onClick, mod, enabled, shape,
            colors = ButtonDefaults.buttonColors(Snap.SecondaryContainer, Snap.OnSecondaryContainer),
            contentPadding = padding, content = content,
        )
        ButtonKind.OUTLINED -> OutlinedButton(
            onClick, mod, enabled, shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Snap.Primary),
            border = BorderStroke(1.dp, Snap.Outline),
            contentPadding = padding, content = content,
        )
        ButtonKind.TEXT -> TextButton(onClick, mod, enabled, shape, contentPadding = padding, content = content)
    }
}

@Composable
fun CircleIconButton(icon: String, onClick: () -> Unit, tint: Color = Snap.OnSurface, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) { Symbol(icon, tint = tint) }
}

@Composable
fun Spinner(size: Dp = 24.dp) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = Snap.Primary,
        trackColor = Snap.PrimaryContainer,
        strokeWidth = 3.dp,
    )
}

/** "On device" or "Cloud", so the owner can see what left the phone. */
@Composable
fun WhereTag(where: Where) {
    Tag(if (where == Where.DEVICE) "smartphone" else "cloud", if (where == Where.DEVICE) "On device" else "Cloud")
}

@Composable
fun Tag(icon: String, text: String) {
    Row(
        modifier = Modifier
            .border(1.dp, Snap.OutlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Symbol(icon, size = 14.dp, tint = Snap.OnSurfaceVariant)
        Text(text, style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium), color = Snap.OnSurfaceVariant)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Type.LabelSmall, color = Snap.OnSurfaceVariant, modifier = modifier)
}

/** Animated bars shown while listening. */
@Composable
fun Wave() {
    val transition = rememberInfiniteTransition(label = "wave")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
        listOf(18, 30, 40, 26, 36, 22, 14).forEachIndexed { i, h ->
            val scale by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse, StartOffset(i * 110)),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(h.dp)
                    .graphicsLayer { scaleY = scale }
                    .background(Snap.Primary, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** The blinking text cursor that follows a live transcript. */
@Composable
fun Caret(height: Dp = 22.dp) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(keyframes { durationMillis = 1000; 1f at 499; 0f at 500 }),
        label = "blink",
    )
    Box(Modifier.padding(start = 3.dp).width(2.dp).height(height).alpha(alpha).background(Snap.Primary))
}

/** A live transcript with a blinking caret at the end of the last line while [listening]. */
@Composable
fun Transcript(text: String, listening: Boolean, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val caretId = "caret"
    val annotated = buildAnnotatedString {
        append(text)
        if (listening) appendInlineContent(caretId, "|")
    }
    val inline = mapOf(
        caretId to InlineTextContent(Placeholder(8.sp, style.fontSize, PlaceholderVerticalAlign.TextCenter)) {
            Caret(height = with(LocalDensity.current) { style.fontSize.toDp() })
        },
    )
    Text(annotated, modifier = modifier, style = style, color = color, inlineContent = inline)
}

/** The expanding ring behind the mic button while it's listening. */
@Composable
fun MicRing(size: Dp) {
    val transition = rememberInfiniteTransition(label = "ring")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "ring")
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = 1f + 0.7f * progress
                scaleY = 1f + 0.7f * progress
                alpha = 0.45f * (1 - progress)
            }
            .background(Snap.Primary, RoundedCornerShape(50)),
    )
}
