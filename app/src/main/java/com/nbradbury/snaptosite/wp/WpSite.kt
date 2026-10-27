package com.nbradbury.snaptosite.wp

import com.nbradbury.snaptosite.site.UploadedMedia
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpLoginClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DiscoveredAuthenticationMechanism
import uniffi.wp_api.MediaCreateParams
import uniffi.wp_api.PostCreateParams
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostRetrieveParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.SiteSettingsUpdateParams
import uniffi.wp_api.ThemeListParams
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.createApplicationPasswordAuthenticationUrl
import uniffi.wp_api.extractLoginDetailsFromUrl
import uniffi.wp_api.localizeAutoDiscoveryAttemptFailure
import java.io.File
import java.net.URL

class WpException(message: String) : Exception(message)

data class Credentials(val siteUrl: String, val apiRoot: String, val user: String, val password: String)

/** A page the app wrote, with its permalink and the modified time WordPress recorded for that write. */
data class PageRef(val id: Long, val link: String, val modifiedGmt: Long)

/** Application-password login: discover the site's API, then authorize in the browser. */
object AppPasswordAuth {
    const val CALLBACK = "snaptosite://auth"

    data class Discovery(val siteUrl: String, val apiRoot: String, val authorizeUrl: String)

    suspend fun discover(siteUrl: String): Discovery {
        val client = WpLoginClient(networkAvailabilityProvider = { true })
        val success = when (val result = client.apiDiscovery(siteUrl)) {
            is ApiDiscoveryResult.Success -> result.success
            is ApiDiscoveryResult.Failure -> throw WpException(localizeAutoDiscoveryAttemptFailure(result.failure, null))
        }
        val auth = success.authentication as? DiscoveredAuthenticationMechanism.ApplicationPasswords
            ?: throw WpException("This site doesn't allow application passwords, which Snap-to-Site needs.")
        val authorizeUrl = createApplicationPasswordAuthenticationUrl(
            loginUrl = auth.authenticationUrl,
            appName = "Snap-to-Site",
            appId = null,
            successUrl = CALLBACK,
            rejectUrl = CALLBACK,
        )
        return Discovery(
            siteUrl = success.parsedSiteUrl.url(),
            apiRoot = success.apiRootUrl.url(),
            authorizeUrl = authorizeUrl.url(),
        )
    }

    /** Reads the credentials WordPress appends to [CALLBACK]; throws if the user declined. */
    fun credentials(callbackUrl: String, siteUrl: String, apiRoot: String): Credentials {
        val details = try {
            extractLoginDetailsFromUrl(callbackUrl)
        } catch (e: Exception) {
            throw WpException("WordPress didn't grant access.")
        }
        return Credentials(siteUrl, apiRoot, user = details.userLogin, password = details.password)
    }
}

/** The handful of REST calls Snap-to-Site makes, via wordpress-rs. */
class WpSite(credentials: Credentials) {
    private val client = WpApiClient(
        wpOrgSiteApiRootUrl = URL(credentials.apiRoot),
        authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(credentials.user, credentials.password),
        interceptors = emptyList(),
        networkAvailabilityProvider = { true },
    )

    suspend fun hasBlockTheme(): Boolean = client.request {
        it.themes().listWithEditContext(ThemeListParams(status = ThemeStatus.Active))
    }.orThrow().data.any { it.isBlockTheme }

    suspend fun uploadPhoto(file: File, title: String): UploadedMedia {
        val media = client.request {
            it.media().create(MediaCreateParams(filePath = file.path, title = title))
        }.orThrow().data
        return UploadedMedia(id = media.id, url = media.sourceUrl)
    }

    suspend fun createPage(title: String, slug: String, content: String): PageRef {
        val page = client.request {
            it.posts().create(
                PostEndpointType.Pages,
                PostCreateParams(title = title, slug = slug, content = content, status = PostStatus.Publish),
            )
        }.orThrow().data
        return PageRef(page.id, page.link, page.modifiedGmt.time)
    }

    suspend fun updatePage(id: Long, content: String): PageRef {
        val page = client.request {
            it.posts().update(PostEndpointType.Pages, id, PostUpdateParams(content = content))
        }.orThrow().data
        return PageRef(page.id, page.link, page.modifiedGmt.time)
    }

    suspend fun pageModifiedGmt(id: Long): Long = client.request {
        it.posts().retrieveWithEditContext(PostEndpointType.Pages, id, PostRetrieveParams())
    }.orThrow().data.modifiedGmt.time

    suspend fun trashPage(id: Long) {
        client.request { it.posts().trash(PostEndpointType.Pages, id) }.orThrow()
    }

    suspend fun updateSettings(title: String, tagline: String, frontPageId: Long, logoId: Long?) {
        client.request {
            it.siteSettings().update(
                SiteSettingsUpdateParams(
                    title = title,
                    description = tagline,
                    showOnFront = "page",
                    pageOnFront = frontPageId.toULong(),
                    siteLogo = logoId?.toULong(),
                ),
            )
        }.orThrow()
    }

    private fun <T> WpRequestResult<T>.orThrow(): T = when (this) {
        is WpRequestResult.Success -> response
        is WpRequestResult.WpError -> throw WpException(errorMessage)
        is WpRequestResult.RequestExecutionFailed -> throw WpException("Couldn't reach the site.")
        is WpRequestResult.InvalidHttpStatusCode -> throw WpException("The site returned HTTP $statusCode.")
        else -> throw WpException("WordPress request failed: $this")
    }
}
