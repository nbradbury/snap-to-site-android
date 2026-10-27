package com.nbradbury.snaptosite

import android.content.Context
import androidx.core.content.edit
import com.fasterxml.jackson.module.kotlin.readValue
import com.nbradbury.snaptosite.ai.HeardFacts
import com.nbradbury.snaptosite.ai.SiteAi
import com.nbradbury.snaptosite.ai.SiteModel
import com.nbradbury.snaptosite.site.FieldChange
import com.nbradbury.snaptosite.site.PublishedState
import com.nbradbury.snaptosite.wp.Credentials

/**
 * Persists keys, site credentials and the site itself in app-private storage. Backup is disabled
 * for the app so the credentials never leave the device.
 */
class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("snap", Context.MODE_PRIVATE)
    private val mapper = SiteAi.MAPPER

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(value) = prefs.edit { putString("api_key", value) }

    var credentials: Credentials?
        get() = read("credentials")
        set(value) = write("credentials", value)

    var pendingLogin: PendingLogin?
        get() = read("pending_login")
        set(value) = write("pending_login", value)

    var site: SiteModel?
        get() = read("site")
        set(value) = write("site", value)

    var published: PublishedState?
        get() = read("published")
        set(value) = write("published", value)

    var facts: HeardFacts?
        get() = read("facts")
        set(value) = write("facts", value)

    var activity: List<ActivityEntry>
        get() = read("activity") ?: emptyList()
        set(value) = write("activity", value)

    /** Show a diff and wait for Apply before writing a spoken change, instead of applying with Undo. */
    var confirmFirst: Boolean
        get() = prefs.getBoolean("confirm_first", false)
        set(value) = prefs.edit { putBoolean("confirm_first", value) }

    /** Use Gemini Nano for quick tasks when the phone has it. */
    var onDevice: Boolean
        get() = prefs.getBoolean("on_device", true)
        set(value) = prefs.edit { putBoolean("on_device", value) }

    var description: String
        get() = prefs.getString("description", "").orEmpty()
        set(value) = prefs.edit { putString("description", value) }

    private inline fun <reified T> read(key: String): T? =
        prefs.getString(key, null)?.let { runCatching { mapper.readValue<T>(it) }.getOrNull() }

    private fun write(key: String, value: Any?) = prefs.edit {
        if (value == null) remove(key) else putString(key, mapper.writeValueAsString(value))
    }
}

/** Discovery result kept while the user authorizes in the browser; [publish] continues to publishing after. */
data class PendingLogin(val siteUrl: String, val apiRoot: String, val publish: Boolean)

/**
 * One change to the site, kept so it can be reviewed and undone. Undo restores the changed
 * sections from [before]; redo restores them from [after].
 */
data class ActivityEntry(
    val id: Long,
    val time: Long,
    val title: String,
    val summary: String,
    val changes: List<FieldChange>,
    val before: SiteModel?,
    val after: SiteModel?,
    val undone: Boolean = false,
)
