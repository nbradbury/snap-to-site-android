package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.site.PageKey
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
    fun `showroom photo goes on the Showroom page`() {
        val withShowroom = photos + Photo(File("5-SHOWROOM.jpg"), PhotoKind.SHOWROOM)
        assertEquals(5, SiteLook.withTaggedPhotos(cafe, withShowroom).showroom.photo)
        assertEquals(setOf(1, 3), SiteLook.toRead(withShowroom).keys)
    }

    @Test
    fun `pages follow the photos given, with Storefront always`() {
        // photos: storefront, menu, logo, flyer, storefront
        assertEquals(listOf(PageKey.HOME, PageKey.MENU, PageKey.EVENTS), SiteLook.pages(photos))
        assertEquals(listOf(PageKey.HOME), SiteLook.pages(emptyList()))
        val all = photos + Photo(File("5-SHOWROOM.jpg"), PhotoKind.SHOWROOM)
        assertEquals(listOf(PageKey.HOME, PageKey.SHOWROOM, PageKey.MENU, PageKey.EVENTS), SiteLook.pages(all))
    }

    @Test
    fun `remap follows renumbered photos and drops removed ones`() {
        val site = cafe.copy(hero = cafe.hero.copy(photo = 4), logoPhoto = 2, events = cafe.events.map { it.copy(photo = 3) })
        // Removing the menu (index 1) moves 2→1, 3→2, 4→3.
        val remapped = SiteLook.remap(site, mapOf(0 to 0, 2 to 1, 3 to 2, 4 to 3))
        assertEquals(3, remapped.hero.photo)
        assertEquals(1, remapped.logoPhoto)
        assertEquals(2, remapped.events.single().photo)
        // Removing the flyer drops the event's photo.
        assertEquals(null, SiteLook.remap(site, mapOf(0 to 0, 1 to 1, 2 to 2, 4 to 3)).events.single().photo)
    }

    @Test
    fun `no tagged photos means no Home photo or logo`() {
        val placed = SiteLook.withTaggedPhotos(cafe, photos.filter { it.kind == PhotoKind.MENU })
        assertEquals(null, placed.hero.photo)
        assertEquals(null, placed.logoPhoto)
    }
}
