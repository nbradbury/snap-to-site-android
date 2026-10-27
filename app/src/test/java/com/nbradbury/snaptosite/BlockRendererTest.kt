package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.ai.DayHours
import com.nbradbury.snaptosite.ai.MenuItem
import com.nbradbury.snaptosite.ai.Showroom
import com.nbradbury.snaptosite.site.BlockRenderer
import com.nbradbury.snaptosite.site.PageKey
import com.nbradbury.snaptosite.site.SiteColors
import com.nbradbury.snaptosite.site.UploadedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockRendererTest {
    private val media = mapOf(1 to UploadedMedia(11, "https://x.test/1.jpg"), 2 to UploadedMedia(12, "https://x.test/2.jpg"))
    private val renderer = BlockRenderer(SiteColors("#2e4b3c", "#b9603a", "#f5efe3"), media)

    @Test
    fun `publishes Menu, Showroom and Events only when they have content, Storefront last`() {
        assertEquals(listOf(PageKey.MENU, PageKey.EVENTS, PageKey.HOME), renderer.pageKeys(cafe))
        val withShowroom = cafe.copy(showroom = Showroom("Frames from local makers.", photo = 1))
        assertEquals(listOf(PageKey.MENU, PageKey.SHOWROOM, PageKey.EVENTS, PageKey.HOME), renderer.pageKeys(withShowroom))
        val bare = cafe.copy(menu = emptyList(), events = emptyList())
        assertEquals(listOf(PageKey.HOME), renderer.pageKeys(bare))
    }

    @Test
    fun `showroom page has its photo and text`() {
        val page = renderer.render(cafe.copy(showroom = Showroom("Frames from local makers.", photo = 2)), PageKey.SHOWROOM)
        assertTrue(page.contains("wp-image-12"))
        assertTrue(page.contains("<p>Frames from local makers.</p>"))
    }

    @Test
    fun `every opened block is closed`() {
        val site = cafe.copy(banner = "Closed Thursday")
        for (key in PageKey.entries) {
            val content = renderer.render(site, key, mapOf(PageKey.MENU to "https://x.test/menu/"))
            val opened = Regex("<!-- wp:([a-z-]+)").findAll(content).map { it.groupValues[1] }.toList()
            val closed = Regex("<!-- /wp:([a-z-]+)").findAll(content).map { it.groupValues[1] }.toList()
            assertEquals(key.name, opened.sorted(), closed.sorted())
        }
    }

    @Test
    fun `banner tops every page`() {
        val site = cafe.copy(banner = "Closed Thursday")
        for (key in PageKey.entries) {
            assertTrue(key.name, renderer.render(site, key).startsWith("<!-- wp:group {\"className\":\"snap-banner\""))
        }
        assertFalse(renderer.render(cafe.copy(banner = " "), PageKey.HOME).contains("snap-banner"))
    }

    @Test
    fun `home links to the menu once it's published`() {
        assertFalse(renderer.render(cafe, PageKey.HOME).contains("See the menu"))
        val home = renderer.render(cafe, PageKey.HOME, mapOf(PageKey.MENU to "https://x.test/menu/"))
        assertTrue(home.contains("""href="https://x.test/menu/""""))
        assertTrue(home.contains("wp-image-11"))
        assertTrue(home.contains("<strong>TODAY'S SOUP</strong>"))
    }

    @Test
    fun `storefront page has hours, highlighted exceptions and directions`() {
        val site = cafe.copy(hours = cafe.hours + DayHours("Thursday, Nov 26", "Closed · Thanksgiving", true))
        val visit = renderer.render(site, PageKey.HOME)
        assertTrue(visit.contains("<strong>Tuesday – Sunday</strong>: 7am – 3pm"))
        assertTrue(visit.contains("""color:#b9603a" class="has-inline-color"><strong>Thursday, Nov 26</strong>"""))
        assertTrue(visit.contains("""<a href="tel:5551234567">"""))
        assertTrue(visit.contains("query=418+Alder+Street"))
    }

    @Test
    fun `events show the weekday and date`() {
        val events = renderer.render(cafe, PageKey.EVENTS)
        assertTrue(events.contains("<strong>Friday, Nov 6 · 6 – 8pm</strong>"))
        assertTrue(events.contains("wp-image-12"))
    }

    @Test
    fun `text is escaped`() {
        val home = renderer.render(cafe, PageKey.HOME)
        assertTrue(home.contains("Espresso, morning pastries &amp; soup at lunch."))
        val site = cafe.copy(menu = listOf(cafe.menu[0].copy(items = listOf(MenuItem("<b>Mac</b> \"special\"", null, null)))))
        assertTrue(renderer.render(site, PageKey.MENU).contains("<strong>&lt;b&gt;Mac&lt;/b&gt; &quot;special&quot;</strong>"))
    }
}
