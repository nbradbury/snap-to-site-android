package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.ai.DayHours
import com.nbradbury.snaptosite.ai.Event
import com.nbradbury.snaptosite.ai.MenuItem
import com.nbradbury.snaptosite.site.FieldChange
import com.nbradbury.snaptosite.site.Section
import com.nbradbury.snaptosite.site.SiteDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteDiffTest {
    private val holiday = cafe.copy(
        banner = "Closed Thursday, Nov 26 for Thanksgiving.",
        hours = cafe.hours + DayHours("Thursday, Nov 26", "Closed · Thanksgiving", true),
    )

    @Test
    fun `identical sites have no changes`() {
        assertTrue(SiteDiff.changes(cafe, cafe.copy()).isEmpty())
    }

    @Test
    fun `holiday closure shows the banner and the new hours row`() {
        assertEquals(
            listOf(
                FieldChange(Section.BANNER, "Banner · every page", "None", "Closed Thursday, Nov 26 for Thanksgiving."),
                FieldChange(Section.HOURS, "Hours · Thursday, Nov 26", "—", "Closed · Thanksgiving"),
            ),
            SiteDiff.changes(cafe, holiday),
        )
    }

    @Test
    fun `menu changes are per item`() {
        val coffee = cafe.menu[0]
        val after = cafe.copy(menu = listOf(coffee.copy(items = coffee.items.drop(1) + MenuItem("Oat milk latte", "\$5.25", null))))
        assertEquals(
            listOf(
                FieldChange(Section.MENU, "Menu · Coffee", "Latte · \$4.75", "Removed"),
                FieldChange(Section.MENU, "Menu · Coffee", "—", "+ Oat milk latte · \$5.25"),
            ),
            SiteDiff.changes(cafe, after),
        )
    }

    @Test
    fun `new events read as a line`() {
        val after = cafe.copy(events = cafe.events + Event("Latte art class", "2026-11-14", "9 – 10:30am", null, null))
        assertEquals(
            listOf(FieldChange(Section.EVENTS, "Events", "—", "Latte art class · Sat, Nov 14, 9 – 10:30am")),
            SiteDiff.changes(cafe, after),
        )
    }

    @Test
    fun `undoing an older change keeps a later one`() {
        val soup = holiday.copy(special = cafe.special!!.copy(text = "Tomato basil"))
        val undone = SiteDiff.sections(SiteDiff.changes(cafe, holiday)).fold(soup) { site, section -> section.copy(from = cafe, into = site) }
        assertEquals(cafe.copy(special = soup.special), undone)
    }
}
