package com.nbradbury.snaptosite.site

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.File

/**
 * What the owner said a photo shows, from the tile they took it for. Photo files are named by
 * these names, so renaming one would orphan saved photos.
 */
enum class PhotoKind(val label: String) {
    STOREFRONT("storefront"),
    MENU("menu"),
    SHOWROOM("showroom"),
    FLYER("event flyer or poster"),

    /** No longer offered, but kept so logo photos saved by earlier versions still load. */
    LOGO("logo"),
    ;

    companion object {
        /** The kinds the capture screen offers, in tile order. */
        val TILES = listOf(STOREFRONT, MENU, SHOWROOM, FLYER)
    }
}

data class Photo(val file: File, val kind: PhotoKind)

/**
 * The owner's captured photos, stored in app storage as downscaled JPEGs named `<index>-<kind>.jpg`.
 * A photo's index is how the site model refers to it.
 */
class Photos(context: Context) {
    private val dir = File(context.filesDir, "photos").apply { mkdirs() }

    fun all(): List<Photo> = dir.listFiles().orEmpty()
        .mapNotNull { file ->
            val parts = file.nameWithoutExtension.split("-")
            val index = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val kind = parts.getOrNull(1)?.let { name -> PhotoKind.entries.find { it.name == name } }
                ?: return@mapNotNull null
            index to Photo(file, kind)
        }
        .sortedBy { it.first }
        .map { it.second }

    /** Copies [source] in as the next photo, scaled so its long edge is at most [MAX_EDGE] pixels. */
    fun add(source: ImageDecoder.Source, kind: PhotoKind): Photo {
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longEdge = maxOf(info.size.width, info.size.height)
            if (longEdge > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longEdge
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val file = File(dir, "${all().size}-${kind.name}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return Photo(file, kind)
    }

    /**
     * Replaces the photo of [kind] with [source], renumbering the rest. Only for before a site is
     * built, since a built site refers to photos by index.
     */
    fun replace(source: ImageDecoder.Source, kind: PhotoKind): Photo {
        all().filter { it.kind == kind }.forEach { it.file.delete() }
        all().forEachIndexed { i, photo -> photo.file.renameTo(File(dir, "$i-${photo.kind.name}.jpg")) }
        return add(source, kind)
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        // Claude's recommended maximum before it downsamples images itself
        const val MAX_EDGE = 1568
    }
}
