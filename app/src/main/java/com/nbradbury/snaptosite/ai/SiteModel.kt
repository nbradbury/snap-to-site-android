package com.nbradbury.snaptosite.ai

import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Everything the app knows about the business's site. Claude produces and edits this; the app
 * renders it to block markup, so the structured data (menu, hours, events) stays editable.
 * Photo fields are indexes into the photos the owner has captured, in capture order.
 */
data class SiteModel(
    val businessName: String,
    @JsonPropertyDescription("One short line under the business name")
    val tagline: String,
    @JsonPropertyDescription("Hex color like #2a6f4e suiting the business, used when there's no logo or storefront photo")
    val accentColor: String,
    @JsonPropertyDescription("Index of the photo showing the business's logo, if any")
    val logoPhoto: Int?,
    @JsonPropertyDescription("Short announcement shown at the top of every page, e.g. a holiday closure")
    val banner: String?,
    val hero: Hero,
    val about: About,
    @JsonPropertyDescription("A daily special or similar changing item, if the owner mentions one")
    val special: Special?,
    @JsonPropertyDescription("Menu or price list; empty if the business doesn't have one")
    val menu: List<MenuCategory>,
    @JsonPropertyDescription("Regular opening hours in week order, plus dated exceptions such as holiday closures; empty if unknown")
    val hours: List<DayHours>,
    val events: List<Event>,
    val contact: Contact,
)

data class Hero(
    @JsonPropertyDescription("Short headline for the home page saying what the business is known for, e.g. Espresso, morning pastries & soup at lunch.")
    val heading: String,
    @JsonPropertyDescription("Index of the best wide photo for the top of the home page, usually the storefront")
    val photo: Int?,
)

data class About(
    @JsonPropertyDescription("A short intro paragraph under the headline: where the business is and what makes it worth a visit")
    val text: String,
    val photo: Int?,
)

data class Special(
    @JsonPropertyDescription("e.g. Today's soup")
    val label: String,
    @JsonPropertyDescription("e.g. Tomato basil, or Ask at the counter")
    val text: String,
)

data class MenuCategory(
    val name: String,
    val items: List<MenuItem>,
)

data class MenuItem(
    val name: String,
    @JsonPropertyDescription("Price exactly as shown, e.g. \$4.50")
    val price: String?,
    val description: String?,
)

data class DayHours(
    @JsonPropertyDescription("A weekday name or range like Tuesday – Sunday, or a specific date like Thursday, Nov 26")
    val day: String,
    @JsonPropertyDescription("e.g. 7am – 3pm, or Closed, or Closed · Thanksgiving")
    val hours: String,
    @JsonPropertyDescription("True for a one-off date that overrides the regular hours")
    val exception: Boolean,
)

data class Event(
    val title: String,
    @JsonPropertyDescription("Date in YYYY-MM-DD form")
    val date: String,
    @JsonPropertyDescription("e.g. 6 – 8pm")
    val time: String?,
    val description: String?,
    val photo: Int?,
)

data class Contact(
    val address: String?,
    val phone: String?,
    val email: String?,
)

/** A post-launch edit, applied: the updated site plus a short description of the change. */
data class SiteEdit(val title: String, val summary: String, val site: SiteModel)

/**
 * What Claude returns for an edit: only the sections that change, so a small change doesn't cost
 * a whole site's worth of output tokens. Null means unchanged.
 */
data class SiteEditResponse(
    @JsonPropertyDescription("Two to four words naming the change, e.g. Closed for Thanksgiving")
    val title: String,
    @JsonPropertyDescription("One short sentence saying what changed, e.g. Updated hours and added a banner")
    val summary: String,
    val changes: SitePatch,
)

data class SitePatch(
    @JsonPropertyDescription("New banner text, an empty string to remove the banner, or null to leave it")
    val banner: String?,
    val businessName: String?,
    val tagline: String?,
    val hero: Hero?,
    val about: About?,
    @JsonPropertyDescription("New special; one with an empty label removes the special; null leaves it")
    val special: Special?,
    @JsonPropertyDescription("The complete new menu if any of it changes, else null")
    val menu: List<MenuCategory>?,
    @JsonPropertyDescription("The complete new hours list if any of it changes, else null")
    val hours: List<DayHours>?,
    @JsonPropertyDescription("The complete new events list if any of it changes, else null")
    val events: List<Event>?,
    val contact: Contact?,
) {
    fun applyTo(site: SiteModel) = site.copy(
        banner = if (banner == null) site.banner else banner.ifBlank { null },
        businessName = businessName ?: site.businessName,
        tagline = tagline ?: site.tagline,
        hero = hero ?: site.hero,
        about = about ?: site.about,
        special = if (special == null) site.special else special.takeIf { it.label.isNotBlank() },
        menu = menu ?: site.menu,
        hours = hours ?: site.hours,
        events = events ?: site.events,
        contact = contact ?: site.contact,
    )
}

/** Details picked out of what the owner said, shown back while they talk. */
data class HeardFacts(
    val name: String?,
    @JsonPropertyDescription("Kind of business, e.g. Café")
    val business: String?,
    @JsonPropertyDescription("Street or neighborhood")
    val where: String?,
    @JsonPropertyDescription("What they sell or serve, briefly")
    val serves: String?,
    @JsonPropertyDescription("Opening hours, briefly")
    val hours: String?,
)
