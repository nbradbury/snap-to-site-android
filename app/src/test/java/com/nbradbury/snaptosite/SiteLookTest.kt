package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.site.Photo
import com.nbradbury.snaptosite.site.PhotoKind
import com.nbradbury.snaptosite.site.SiteLook
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SiteLookTest {
    private val photos = listOf(PhotoKind.STOREFRONT, PhotoKind.MENU, PhotoKind.LOGO, PhotoKind.FLYER, PhotoKind.STOREFRONT)
        .mapIndexed { i, kind -> Photo(File("$i-$kind.jpg"), kind) }

    @Test
    fun `only menus and flyers go to Claude, keeping their indexes`() {
        assertEquals(setOf(1, 3), SiteLook.toRead(photos).keys)
    }

    @Test
    fun `latest storefront goes on Home and the logo in the header`() {
        val placed = SiteLook.withTaggedPhotos(cafe.copy(logoPhoto = 1, about = cafe.about.copy(photo = 3)), photos)
        assertEquals(4, placed.hero.photo)
        assertEquals(2, placed.logoPhoto)
        assertEquals(null, placed.about.photo)
    }

    @Test
    fun `no tagged photos means no Home photo or logo`() {
        val placed = SiteLook.withTaggedPhotos(cafe, photos.filter { it.kind == PhotoKind.MENU })
        assertEquals(null, placed.hero.photo)
        assertEquals(null, placed.logoPhoto)
    }
}
