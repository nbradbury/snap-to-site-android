package com.nbradbury.snaptosite.site

import com.nbradbury.snaptosite.ai.SiteModel
import com.nbradbury.snaptosite.wp.PageRef
import com.nbradbury.snaptosite.wp.WpSite

/** A page the app wrote, so later edits update it and can tell if someone else changed it. */
data class PublishedPage(val id: Long, val link: String, val modifiedGmt: Long, val contentHash: Int)

data class PublishedState(
    val pages: Map<PageKey, PublishedPage> = emptyMap(),
    val media: Map<Int, UploadedMedia> = emptyMap(),
)

/** Writes a [SiteModel] to WordPress, touching only the pages whose content changed. */
class Publisher(private val wp: WpSite, private val photos: List<Photo>) {

    suspend fun publish(site: SiteModel, state: PublishedState): PublishedState {
        val media = state.media.toMutableMap()
        for (index in referencedPhotos(site)) {
            if (index !in media && index in photos.indices) {
                media[index] = wp.uploadPhoto(photos[index].file, site.businessName)
            }
        }

        val renderer = BlockRenderer(SiteLook.colors(site, photos), media)
        val pages = mutableMapOf<PageKey, PublishedPage>()
        for (key in BlockRenderer.writeOrder(SiteLook.pages(photos))) {
            val content = renderer.render(site, key, pages.mapValues { it.value.link })
            val existing = state.pages[key]
            val write = { ref: PageRef -> PublishedPage(ref.id, ref.link, ref.modifiedGmt, content.hashCode()) }
            pages[key] = when {
                existing == null -> write(wp.createPage(key.title, key.slug, content))
                existing.contentHash != content.hashCode() -> write(wp.updatePage(existing.id, content))
                else -> existing
            }
        }
        for ((key, page) in state.pages) {
            if (key !in pages) wp.trashPage(page.id)
        }

        wp.updateSettings(
            title = site.businessName,
            tagline = site.tagline,
            frontPageId = pages.getValue(PageKey.HOME).id,
            logoId = SiteLook.logoIndex(site, photos)?.let { media[it]?.id },
        )
        return PublishedState(pages, media)
    }

    /** Titles of pages someone changed outside the app since it last wrote them. */
    suspend fun externallyEdited(state: PublishedState): List<String> =
        state.pages.filter { (_, page) -> wp.pageModifiedGmt(page.id) != page.modifiedGmt }.keys.map { it.title }

    private fun referencedPhotos(site: SiteModel): Set<Int> = buildSet {
        SiteLook.logoIndex(site, photos)?.let(::add)
        site.hero.photo?.let(::add)
        site.showroom.photo?.let(::add)
        site.events.forEach { event -> event.photo?.let(::add) }
    }
}

/** Where a site's logo and colors come from: the photos the owner tagged, else Claude's picks. */
object SiteLook {
    /**
     * The site's pages, in preview order: Storefront always, since it's the front page, and
     * Showroom, Menu and Events only once the owner has given a photo for them.
     */
    fun pages(photos: List<Photo>): List<PageKey> {
        val kinds = photos.map { it.kind }.toSet()
        return PageKey.SHOWN.filter { page ->
            when (page) {
                PageKey.HOME -> true
                PageKey.SHOWROOM -> PhotoKind.SHOWROOM in kinds
                PageKey.MENU -> PhotoKind.MENU in kinds
                PageKey.EVENTS -> PhotoKind.FLYER in kinds
                PageKey.VISIT -> false
            }
        }
    }

    /** Points [site]'s photo references at the new indexes from Photos.remove; removed photos drop out. */
    fun remap(site: SiteModel, moved: Map<Int, Int>): SiteModel {
        val map = { index: Int? -> index?.let { moved[it] } }
        return site.copy(
            logoPhoto = map(site.logoPhoto),
            hero = site.hero.copy(photo = map(site.hero.photo)),
            about = site.about.copy(photo = map(site.about.photo)),
            showroom = site.showroom.copy(photo = map(site.showroom.photo)),
            events = site.events.map { it.copy(photo = map(it.photo)) },
        )
    }

    /** Photos Claude needs to read, keyed by index: menu boards and flyers. */
    fun toRead(photos: List<Photo>): Map<Int, Photo> = photos.withIndex()
        .filter { it.value.kind == PhotoKind.MENU || it.value.kind == PhotoKind.FLYER }
        .associate { it.index to it.value }

    /**
     * Puts the owner's latest storefront photo on the Storefront page, their showroom photo on the
     * Showroom page, and their logo in the header.
     */
    fun withTaggedPhotos(site: SiteModel, photos: List<Photo>): SiteModel {
        fun latest(kind: PhotoKind) = photos.indexOfLast { it.kind == kind }.takeIf { it >= 0 }
        return site.copy(
            hero = site.hero.copy(photo = latest(PhotoKind.STOREFRONT)),
            about = site.about.copy(photo = null),
            showroom = site.showroom.copy(photo = latest(PhotoKind.SHOWROOM)),
            logoPhoto = latest(PhotoKind.LOGO),
        )
    }

    fun logoIndex(site: SiteModel, photos: List<Photo>): Int? =
        photos.indexOfLast { it.kind == PhotoKind.LOGO }.takeIf { it >= 0 } ?: site.logoPhoto

    fun colors(site: SiteModel, photos: List<Photo>): SiteColors {
        val source = logoIndex(site, photos) ?: photos.indexOfLast { it.kind == PhotoKind.STOREFRONT }.takeIf { it >= 0 }
        return LogoColors.from(source?.let { photos.getOrNull(it)?.file }, site.accentColor)
    }
}
