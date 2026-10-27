package com.nbradbury.snaptosite.site

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.io.File

/**
 * Site colors: [primary] for headings and buttons, [accent] for the banner and highlights, and a
 * light [background] for cards. All are `#rrggbb`.
 */
data class SiteColors(val primary: String, val accent: String, val background: String) {
    val swatches get() = listOf(primary, accent, background)

    companion object {
        /** A warm default for when there's no photo to take colors from. */
        val DEFAULT = SiteColors(primary = "#2e4b3c", accent = "#b9603a", background = "#f5efe3")
    }
}

/** Picks site colors on-device from the logo (or storefront) photo, falling back to Claude's suggestion. */
object LogoColors {
    private val HEX = Regex("#[0-9a-fA-F]{6}")

    fun from(photo: File?, fallbackHex: String?): SiteColors {
        photo?.let(::fromPhoto)?.let { return it }
        val fallback = fallbackHex?.takeIf { HEX.matches(it) } ?: return SiteColors.DEFAULT
        val primary = readable(Color.parseColor(fallback))
        return SiteColors(hex(primary), SiteColors.DEFAULT.accent, hex(tint(primary)))
    }

    private fun fromPhoto(file: File): SiteColors? {
        val bitmap = BitmapFactory.decodeFile(file.path) ?: return null
        val palette = Palette.from(bitmap).generate()
        val primary = (palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch)?.rgb ?: return null
        val accent = (palette.vibrantSwatch ?: palette.mutedSwatch)?.rgb?.takeIf { it != primary }
            ?: Color.parseColor(SiteColors.DEFAULT.accent)
        val background = palette.lightMutedSwatch?.rgb?.let { blend(it, Color.WHITE, 0.6f) } ?: tint(primary)
        return SiteColors(hex(readable(primary)), hex(readable(accent)), hex(background))
    }

    /** Darkens [color] until white text on it (and it on white) reads well. */
    private fun readable(color: Int): Int {
        var c = color
        while (ColorUtils.calculateContrast(c, Color.WHITE) < 4.5) c = blend(c, Color.BLACK, 0.15f)
        return c
    }

    private fun tint(color: Int) = blend(color, Color.WHITE, 0.9f)

    private fun blend(a: Int, b: Int, ratio: Float) = ColorUtils.blendARGB(a, b, ratio) or 0xff000000.toInt()

    private fun hex(color: Int) = String.format("#%06x", color and 0xffffff)
}
