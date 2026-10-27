package com.nbradbury.snaptosite.site

import com.nbradbury.snaptosite.ai.Event
import com.nbradbury.snaptosite.ai.SiteModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A part of the site that changes as a unit, and the page it appears on. */
enum class Section(val page: PageKey) {
    BANNER(PageKey.HOME),
    NAME(PageKey.HOME),
    HERO(PageKey.HOME),
    ABOUT(PageKey.HOME),
    SPECIAL(PageKey.HOME),
    MENU(PageKey.MENU),
    EVENTS(PageKey.EVENTS),
    HOURS(PageKey.VISIT),
    CONTACT(PageKey.VISIT),
    ;

    /** Returns [into] with this section replaced by its value in [from]. */
    fun copy(from: SiteModel, into: SiteModel): SiteModel = when (this) {
        BANNER -> into.copy(banner = from.banner)
        NAME -> into.copy(
            businessName = from.businessName,
            tagline = from.tagline,
            accentColor = from.accentColor,
            logoPhoto = from.logoPhoto,
        )
        HERO -> into.copy(hero = from.hero)
        ABOUT -> into.copy(about = from.about)
        SPECIAL -> into.copy(special = from.special)
        MENU -> into.copy(menu = from.menu)
        EVENTS -> into.copy(events = from.events)
        HOURS -> into.copy(hours = from.hours)
        CONTACT -> into.copy(contact = from.contact)
    }
}

/** One readable before → after line, as shown in review and Activity. */
data class FieldChange(val section: Section, val field: String, val before: String, val after: String)

object SiteDiff {
    private const val NONE = "—"

    fun changes(before: SiteModel, after: SiteModel): List<FieldChange> = buildList {
        fun field(section: Section, name: String, b: String?, a: String?) {
            if (b != a) add(FieldChange(section, name, b ?: NONE, a ?: NONE))
        }

        field(Section.BANNER, "Banner · every page", before.banner ?: "None", after.banner ?: "None")
        field(Section.NAME, "Name", before.businessName, after.businessName)
        field(Section.NAME, "Tagline", before.tagline, after.tagline)
        field(Section.NAME, "Logo", photo(before.logoPhoto), photo(after.logoPhoto))
        field(Section.HERO, "Home · headline", before.hero.heading, after.hero.heading)
        field(Section.HERO, "Home · photo", photo(before.hero.photo), photo(after.hero.photo))
        field(Section.ABOUT, "Home · intro", before.about.text, after.about.text)
        field(Section.ABOUT, "Home · intro photo", photo(before.about.photo), photo(after.about.photo))
        val specialLabel = after.special?.label ?: before.special?.label ?: "special"
        field(Section.SPECIAL, "Home · $specialLabel", before.special?.text ?: "None", after.special?.text ?: "None")

        keyed(
            before.hours.associateBy { it.day }, after.hours.associateBy { it.day },
        ) { day, b, a -> field(Section.HOURS, "Hours · $day", b?.hours, a?.hours) }

        val beforeItems = before.menu.flatMap { c -> c.items.map { (c.name to it.name) to it } }.toMap()
        val afterItems = after.menu.flatMap { c -> c.items.map { (c.name to it.name) to it } }.toMap()
        keyed(beforeItems, afterItems) { (category, name), b, a ->
            val line = { price: String? -> name + price?.let { " · $it" }.orEmpty() }
            when {
                b == null -> field(Section.MENU, "Menu · $category", null, "+ " + line(a!!.price))
                a == null -> field(Section.MENU, "Menu · $category", line(b.price), "Removed")
                else -> {
                    field(Section.MENU, "Menu · $category", line(b.price), line(a.price))
                    field(Section.MENU, "Menu · $name", b.description, a.description)
                }
            }
        }

        keyed(
            before.events.associateBy { it.title }, after.events.associateBy { it.title },
        ) { _, b, a ->
            val line = { e: Event -> "${e.title} · ${EventDates.short(e.date)}" + e.time?.let { ", $it" }.orEmpty() }
            when {
                b == null -> field(Section.EVENTS, "Events", null, line(a!!))
                a == null -> field(Section.EVENTS, "Events", line(b), "Removed")
                b != a -> {
                    val full = { e: Event -> line(e) + e.description?.let { " — $it" }.orEmpty() }
                    field(Section.EVENTS, "Events · ${a.title}", full(b), full(a))
                }
            }
        }

        field(Section.CONTACT, "Visit · address", before.contact.address, after.contact.address)
        field(Section.CONTACT, "Visit · phone", before.contact.phone, after.contact.phone)
        field(Section.CONTACT, "Visit · email", before.contact.email, after.contact.email)
    }

    fun sections(changes: List<FieldChange>): Set<Section> = changes.mapTo(LinkedHashSet()) { it.section }

    private fun photo(index: Int?) = index?.let { "Photo ${it + 1}" } ?: "None"

    /** Calls [block] for every key in either map, in before-then-after order. */
    private inline fun <K, V> keyed(before: Map<K, V>, after: Map<K, V>, block: (K, V?, V?) -> Unit) {
        (before.keys + after.keys).forEach { block(it, before[it], after[it]) }
    }
}

/** Formats the `YYYY-MM-DD` dates Claude returns, passing through anything else unchanged. */
object EventDates {
    private fun parse(date: String) = runCatching { LocalDate.parse(date) }.getOrNull()

    /** e.g. Sat, Nov 14 */
    fun short(date: String) = parse(date)?.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)) ?: date

    /** e.g. Saturday */
    fun weekday(date: String) = parse(date)?.format(DateTimeFormatter.ofPattern("EEEE", Locale.US))

    /** e.g. NOV */
    fun month(date: String) = parse(date)?.format(DateTimeFormatter.ofPattern("MMM", Locale.US))?.uppercase()

    /** e.g. 14 */
    fun day(date: String) = parse(date)?.dayOfMonth?.toString()
}
