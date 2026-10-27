package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.ai.DayHours
import com.nbradbury.snaptosite.ai.SitePatch
import com.nbradbury.snaptosite.ai.Special
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SitePatchTest {
    private val none = SitePatch(null, null, null, null, null, null, null, null, null, null)

    @Test
    fun `an empty patch changes nothing`() {
        assertEquals(cafe, none.applyTo(cafe))
    }

    @Test
    fun `holiday patch sets the banner and replaces the hours`() {
        val hours = cafe.hours + DayHours("Thursday, Nov 26", "Closed · Thanksgiving", true)
        val patched = none.copy(banner = "Closed Thursday", hours = hours).applyTo(cafe)
        assertEquals(cafe.copy(banner = "Closed Thursday", hours = hours), patched)
    }

    @Test
    fun `empty banner and blank special remove them`() {
        val withBanner = cafe.copy(banner = "Closed Thursday")
        val patched = none.copy(banner = "", special = Special("", "")).applyTo(withBanner)
        assertNull(patched.banner)
        assertNull(patched.special)
        assertEquals(cafe.menu, patched.menu)
    }
}
