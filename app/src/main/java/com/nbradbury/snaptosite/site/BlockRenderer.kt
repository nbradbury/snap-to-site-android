package com.nbradbury.snaptosite.site

import com.nbradbury.snaptosite.ai.SiteModel
import java.net.URLEncoder

/** A photo that has been uploaded to the site's media library. */
data class UploadedMedia(val id: Long, val url: String)

/**
 * The site's pages, matching the kinds of photo the owner takes. Stored by name in the published
 * state, so HOME (the front page, titled Storefront) and VISIT keep their old names.
 */
enum class PageKey(val title: String, val slug: String) {
    HOME("Storefront", "storefront"),
    MENU("Menu", "menu"),
    SHOWROOM("Showroom", "showroom"),
    EVENTS("Events", "events"),

    /** No longer published; its hours and contact details are on the Storefront page now. */
    VISIT("Visit", "visit"),
    ;

    companion object {
        /** Pages shown in the app's preview, in the same order as the photo pills. */
        val SHOWN = listOf(HOME, MENU, SHOWROOM, EVENTS)
    }
}

data class RenderedPage(val key: PageKey, val content: String)

/**
 * Turns a [SiteModel] into core-block markup. Each section is wrapped in a group with a stable
 * `snap-*` class so the site's structure is recognizable in the editor. Menu, Showroom and Events
 * pages are only published when there's something to put on them.
 */
class BlockRenderer(
    private val colors: SiteColors,
    private val media: Map<Int, UploadedMedia>,
) {
    /** Pages [site] publishes, in the order they should be written (Storefront last, so it can link to the rest). */
    fun pageKeys(site: SiteModel) = buildList {
        if (site.menu.isNotEmpty()) add(PageKey.MENU)
        if (hasShowroom(site)) add(PageKey.SHOWROOM)
        if (site.events.isNotEmpty()) add(PageKey.EVENTS)
        add(PageKey.HOME)
    }

    /** Renders one page; [links] holds the URLs of pages already published, for cross-links. */
    fun render(site: SiteModel, key: PageKey, links: Map<PageKey, String> = emptyMap()): String {
        val body = when (key) {
            PageKey.HOME -> home(site, links)
            PageKey.MENU -> menu(site)
            PageKey.SHOWROOM -> showroom(site)
            PageKey.EVENTS -> events(site)
            PageKey.VISIT -> ""
        }
        return site.banner?.takeIf { it.isNotBlank() }?.let { banner(it) }.orEmpty() + body
    }

    fun renderAll(site: SiteModel, links: Map<PageKey, String> = emptyMap()) =
        pageKeys(site).map { RenderedPage(it, render(site, it, links)) }

    private fun home(site: SiteModel, links: Map<PageKey, String>) = buildString {
        append(image(site.hero.photo))
        append(section("snap-intro", heading(site.hero.heading) + paragraphs(site.about.text)))
        site.special?.let { special ->
            append(card("snap-special", paragraph("<strong>${esc(special.label.uppercase())}</strong>") + paragraph(esc(special.text))))
        }
        links[PageKey.MENU]?.let { append(button("See the menu", it)) }
        append(visit(site))
    }

    private fun showroom(site: SiteModel) =
        image(site.showroom.photo) + section("snap-showroom", paragraphs(site.showroom.text))

    private fun banner(text: String): String {
        val attrs = """{"className":"snap-banner","style":{"color":{"background":"${colors.accent}","text":"#ffffff"}},"layout":{"type":"constrained"}}"""
        return "<!-- wp:group $attrs -->\n" +
            """<div class="wp-block-group snap-banner has-text-color has-background" style="color:#ffffff;background-color:${colors.accent}">""" +
            "\n" + paragraph("<strong>${esc(text)}</strong>", center = true) +
            "</div>\n<!-- /wp:group -->\n\n"
    }

    private fun menu(site: SiteModel) = section(
        "snap-menu",
        site.menu.joinToString("") { category ->
            heading(category.name) + category.items.joinToString("") { item ->
                val price = item.price?.let { " — ${esc(it)}" }.orEmpty()
                val description = item.description?.let { "<br>${esc(it)}" }.orEmpty()
                paragraph("<strong>${esc(item.name)}</strong>$price$description")
            }
        },
    )

    private fun events(site: SiteModel) = site.events.sortedBy { it.date }.joinToString("") { event ->
        val time = event.time?.let { " · ${esc(it)}" }.orEmpty()
        val weekday = EventDates.weekday(event.date)?.let { "$it, " }.orEmpty()
        card(
            "snap-event",
            paragraph("<strong>${esc(weekday + EventDates.short(event.date).substringAfter(", "))}$time</strong>") +
                heading(event.title, level = 3) +
                (event.description?.let { paragraphs(it) }.orEmpty()) +
                image(event.photo),
        )
    }

    private fun visit(site: SiteModel) = buildString {
        val c = site.contact
        c.address?.let { address ->
            append(section("snap-address", heading(address)))
        }
        if (site.hours.isNotEmpty()) {
            val items = site.hours.map {
                val line = "<strong>${esc(it.day)}</strong>: ${esc(it.hours)}"
                if (it.exception) """<mark style="background-color:rgba(0,0,0,0);color:${colors.accent}" class="has-inline-color">$line</mark>""" else line
            }
            append(section("snap-hours", heading("Hours") + list(items)))
        }
        val lines = listOfNotNull(
            c.phone?.let { """<a href="tel:${esc(it.filter { ch -> ch.isDigit() || ch == '+' })}">${esc(it)}</a>""" },
            c.email?.let { """<a href="mailto:${esc(it)}">${esc(it)}</a>""" },
        )
        if (lines.isNotEmpty()) append(section("snap-contact", lines.joinToString("") { paragraph(it) }))
        c.address?.let {
            append(button("Get directions", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(it, "UTF-8")))
        }
    }

    private fun section(className: String, inner: String) =
        "<!-- wp:group {\"className\":\"$className\",\"layout\":{\"type\":\"constrained\"}} -->\n" +
            """<div class="wp-block-group $className">""" + "\n" + inner + "</div>\n<!-- /wp:group -->\n\n"

    private fun card(className: String, inner: String): String {
        val attrs = """{"className":"$className","style":{"color":{"background":"${colors.background}"},"spacing":{"padding":{"top":"1rem","bottom":"1rem","left":"1rem","right":"1rem"}}},"layout":{"type":"constrained"}}"""
        return "<!-- wp:group $attrs -->\n" +
            """<div class="wp-block-group $className has-background" style="background-color:${colors.background};padding-top:1rem;padding-right:1rem;padding-bottom:1rem;padding-left:1rem">""" +
            "\n" + inner + "</div>\n<!-- /wp:group -->\n\n"
    }

    private fun heading(text: String, level: Int = 2): String {
        val levelAttr = if (level == 2) "" else "\"level\":$level,"
        return "<!-- wp:heading {$levelAttr\"style\":{\"color\":{\"text\":\"${colors.primary}\"}}} -->\n" +
            """<h$level class="wp-block-heading has-text-color" style="color:${colors.primary}">${esc(text)}</h$level>""" +
            "\n<!-- /wp:heading -->\n\n"
    }

    private fun button(label: String, url: String) =
        "<!-- wp:buttons -->\n<div class=\"wp-block-buttons\">" +
            "<!-- wp:button {\"style\":{\"color\":{\"background\":\"${colors.primary}\",\"text\":\"#ffffff\"}}} -->\n" +
            """<div class="wp-block-button"><a class="wp-block-button__link has-text-color has-background wp-element-button" href="${esc(url)}" style="color:#ffffff;background-color:${colors.primary}">${esc(label)}</a></div>""" +
            "\n<!-- /wp:button --></div>\n<!-- /wp:buttons -->\n\n"

    private fun paragraph(html: String, center: Boolean = false) =
        if (center) {
            "<!-- wp:paragraph {\"align\":\"center\"} -->\n<p class=\"has-text-align-center\">$html</p>\n<!-- /wp:paragraph -->\n\n"
        } else {
            "<!-- wp:paragraph -->\n<p>$html</p>\n<!-- /wp:paragraph -->\n\n"
        }

    private fun paragraphs(text: String) = text.split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("") { paragraph(esc(it).replace("\n", "<br>")) }

    private fun list(itemsHtml: List<String>) =
        "<!-- wp:list -->\n<ul class=\"wp-block-list\">" +
            itemsHtml.joinToString("") { "<!-- wp:list-item -->\n<li>$it</li>\n<!-- /wp:list-item -->\n" } +
            "</ul>\n<!-- /wp:list -->\n\n"

    private fun image(index: Int?) = index?.let { media[it] }?.let { imageBlock(it) }.orEmpty()

    private fun imageBlock(m: UploadedMedia) =
        "<!-- wp:image {\"id\":${m.id},\"sizeSlug\":\"large\",\"linkDestination\":\"none\"} -->\n" +
            """<figure class="wp-block-image size-large"><img src="${m.url}" alt="" class="wp-image-${m.id}"/></figure>""" +
            "\n<!-- /wp:image -->\n\n"

    companion object {
        fun hasShowroom(site: SiteModel) = site.showroom.photo != null || site.showroom.text.isNotBlank()

        fun esc(text: String) = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
