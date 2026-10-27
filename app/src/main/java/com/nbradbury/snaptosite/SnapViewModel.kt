package com.nbradbury.snaptosite

import android.app.Application
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nbradbury.snaptosite.ai.HeardFacts
import com.nbradbury.snaptosite.ai.InvalidApiKeyException
import com.nbradbury.snaptosite.ai.OnDeviceFacts
import com.nbradbury.snaptosite.ai.SiteAi
import com.nbradbury.snaptosite.ai.SiteEdit
import com.nbradbury.snaptosite.ai.SiteModel
import com.nbradbury.snaptosite.site.EventDates
import com.nbradbury.snaptosite.site.FieldChange
import com.nbradbury.snaptosite.site.LogoColors
import com.nbradbury.snaptosite.site.PageKey
import com.nbradbury.snaptosite.site.Photo
import com.nbradbury.snaptosite.site.PhotoKind
import com.nbradbury.snaptosite.site.Photos
import com.nbradbury.snaptosite.site.Publisher
import com.nbradbury.snaptosite.site.PublishedState
import com.nbradbury.snaptosite.site.Section
import com.nbradbury.snaptosite.site.SiteColors
import com.nbradbury.snaptosite.site.SiteDiff
import com.nbradbury.snaptosite.site.SiteLook
import com.nbradbury.snaptosite.wp.AppPasswordAuth
import com.nbradbury.snaptosite.wp.WpSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { LOADING, WELCOME, DESCRIBE, CAPTURE, BUILDING, REVIEW, APP }

enum class Tab { SITE, ACTIVITY, ACCOUNT }

/** Where a piece of AI work ran, shown as a tag so the owner knows what left the phone. */
enum class Where { DEVICE, CLOUD }

enum class StepState { PENDING, ACTIVE, DONE, SKIPPED, FAILED }

data class BuildStep(
    val icon: String,
    val label: String,
    val detail: String,
    val where: Where?,
    val state: StepState = StepState.PENDING,
    val swatches: List<String>? = null,
)

enum class VoicePhase { IDLE, LISTENING, TYPING, THINKING, CONFIRM }

data class VoiceSheet(
    val phase: VoicePhase = VoicePhase.IDLE,
    val text: String = "",
    val edit: SiteEdit? = null,
    val changes: List<FieldChange> = emptyList(),
    /** Set when someone edited a page outside the app; forces a confirm before overwriting. */
    val warning: String? = null,
)

sealed interface Dialog {
    /** [invalid] when the saved key was rejected, so the dialog can say why it's asking again. */
    data class ApiKey(val thenDescribe: Boolean, val invalid: Boolean = false) : Dialog
    data object Publish : Dialog
    data class Connect(val publishAfter: Boolean) : Dialog
    data object Publishing : Dialog
    data class EditFact(val field: String, val value: String) : Dialog
}

data class Snack(val text: String, val undoId: Long? = null)

data class UiState(
    val screen: Screen = Screen.LOADING,
    val tab: Tab = Tab.SITE,
    val page: PageKey = PageKey.HOME,
    val busy: Boolean = false,
    val error: String? = null,

    val talking: Boolean = false,
    val typing: Boolean = false,
    val transcript: String = "",
    val facts: HeardFacts? = null,
    val factsFrom: Where? = null,

    val photos: List<Photo> = emptyList(),
    val captureMode: PhotoKind = PhotoKind.STOREFRONT,
    val captureForEdit: Boolean = false,
    /** The camera is open for [captureMode]; otherwise the capture screen shows its photo tiles. */
    val shooting: Boolean = false,
    val detect: String? = null,

    val steps: List<BuildStep> = emptyList(),
    val buildDone: Boolean = false,
    val buildError: String? = null,

    val site: SiteModel? = null,
    val colors: SiteColors = SiteColors.DEFAULT,
    val published: Boolean = false,
    val siteUrl: String? = null,
    val highlight: Set<Section> = emptySet(),
    val activity: List<ActivityEntry> = emptyList(),
    val expanded: Long? = null,

    val sheet: VoiceSheet? = null,
    val dialog: Dialog? = null,
    val snack: Snack? = null,

    val hasApiKey: Boolean = false,
    val confirmFirst: Boolean = false,
    val onDevice: Boolean = true,
    val nanoAvailable: Boolean = false,
) {
    val capturedKinds get() = photos.map { it.kind }.toSet()
}

class SnapViewModel(app: Application) : AndroidViewModel(app) {
    private val store = AppStore(app)
    private val photoStore = Photos(app)
    private val nano = OnDeviceFacts()
    val voice = VoiceInput(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var snackJob: Job? = null
    private var factsJob: Job? = null

    init {
        // Jackson's first use is slow on Android, so read the stored site off the main thread.
        viewModelScope.launch(Dispatchers.IO) {
            val site = store.site
            val photos = photoStore.all()
            val published = store.published != null
            _state.update {
                it.copy(
                    screen = when {
                        site == null -> Screen.WELCOME
                        published -> Screen.APP
                        else -> Screen.REVIEW
                    },
                    transcript = store.description,
                    facts = store.facts,
                    photos = photos,
                    site = site,
                    colors = site?.let { s -> SiteLook.colors(s, photos) } ?: previewColors(photos),
                    published = published,
                    siteUrl = store.credentials?.siteUrl,
                    activity = store.activity,
                    hasApiKey = store.apiKey.isNotBlank(),
                    confirmFirst = store.confirmFirst,
                    onDevice = store.onDevice,
                )
            }
            val available = nano.isAvailable()
            _state.update { it.copy(nanoAvailable = available) }
        }
        // Live transcript goes to whichever surface is listening: the voice sheet or Describe.
        viewModelScope.launch {
            voice.state.collect { v ->
                if (!v.listening) return@collect
                _state.update {
                    if (it.sheet?.phase == VoicePhase.LISTENING) it.copy(sheet = it.sheet.copy(text = v.text))
                    else it.copy(transcript = v.text)
                }
            }
        }
    }

    private fun ai() = SiteAi(store.apiKey, getApplication<Application>().assets)

    private fun publisher() = Publisher(WpSite(store.credentials!!), photoStore.all())

    private fun useNano() = _state.value.nanoAvailable && _state.value.onDevice

    fun dismissError() = _state.update { it.copy(error = null) }

    fun dismissDialog() {
        retryAfterKey = null
        _state.update { it.copy(dialog = null) }
    }

    /** What to run again once the owner replaces a rejected API key. */
    private var retryAfterKey: (() -> Unit)? = null

    /** Asks for a new key if [e] says the current one was rejected, then reruns [retry]. */
    private fun askForNewKey(e: Exception, retry: () -> Unit): Boolean {
        if (e !is InvalidApiKeyException) return false
        retryAfterKey = retry
        _state.update { it.copy(dialog = Dialog.ApiKey(thenDescribe = false, invalid = true)) }
        return true
    }

    // Welcome

    fun getStarted() {
        if (store.apiKey.isBlank()) {
            _state.update { it.copy(dialog = Dialog.ApiKey(thenDescribe = true)) }
        } else {
            _state.update { it.copy(screen = Screen.DESCRIBE) }
        }
    }

    fun showApiKey() = _state.update { it.copy(dialog = Dialog.ApiKey(thenDescribe = false)) }

    fun saveApiKey(key: String) {
        store.apiKey = key.trim()
        val thenDescribe = (_state.value.dialog as? Dialog.ApiKey)?.thenDescribe == true
        _state.update {
            it.copy(
                hasApiKey = store.apiKey.isNotBlank(),
                dialog = null,
                screen = if (thenDescribe) Screen.DESCRIBE else it.screen,
            )
        }
        retryAfterKey?.also { retryAfterKey = null }?.invoke()
    }

    fun back() = _state.update {
        when (it.screen) {
            Screen.DESCRIBE -> {
                voice.cancel()
                it.copy(screen = Screen.WELCOME, talking = false)
            }
            Screen.CAPTURE -> when {
                it.shooting && !it.captureForEdit -> it.copy(shooting = false)
                else -> it.copy(screen = if (it.captureForEdit) Screen.APP else Screen.DESCRIBE, shooting = false, detect = null)
            }
            Screen.REVIEW -> if (it.buildDone) it.copy(screen = Screen.BUILDING) else it
            else -> it
        }
    }

    // Describe

    /** Starts or stops listening. The UI makes sure the microphone permission is granted first. */
    fun toggleTalk() {
        if (voice.state.value.listening) {
            voice.stop()
            return
        }
        if (!voice.isAvailable) {
            _state.update { it.copy(typing = true, error = "Speech recognition isn't available on this phone, so type instead.") }
            return
        }
        _state.update { it.copy(talking = true, typing = false) }
        voice.start(
            continuous = true,
            initial = _state.value.transcript,
            onSegment = { text -> if (useNano()) refreshFacts(text, Where.DEVICE) },
            onError = { message -> _state.update { it.copy(error = message) } },
            onDone = { text -> doneTalking(text) },
        )
    }

    fun sayAgain() {
        setTranscript("")
        _state.update { it.copy(facts = null, factsFrom = null) }
        store.facts = null
        toggleTalk()
    }

    fun typeInstead() {
        voice.cancel()
        _state.update { it.copy(talking = false, typing = true) }
    }

    fun setTranscript(text: String) {
        store.description = text
        _state.update { it.copy(transcript = text) }
    }

    /** Called when the owner stops talking or finishes typing. */
    fun doneTalking(text: String = _state.value.transcript) {
        setTranscript(text)
        _state.update { it.copy(talking = false, typing = false) }
        if (text.isBlank()) return
        // Nano already ran on each pause; without it, ask Claude once now.
        refreshFacts(text, if (useNano()) Where.DEVICE else Where.CLOUD)
    }

    private fun refreshFacts(text: String, where: Where) {
        factsJob?.cancel()
        factsJob = viewModelScope.launch(Dispatchers.IO) {
            val facts = try {
                if (where == Where.DEVICE) nano.extract(text) else ai().extractFacts(text)
            } catch (e: Exception) {
                // The facts are a convenience; the build reads the transcript itself.
                askForNewKey(e) { refreshFacts(text, where) }
                null
            } ?: return@launch
            store.facts = facts
            _state.update { it.copy(facts = facts, factsFrom = where) }
        }
    }

    fun editFact(field: String, value: String) = _state.update { it.copy(dialog = Dialog.EditFact(field, value)) }

    fun saveFact(field: String, value: String) {
        val facts = _state.value.facts ?: HeardFacts(null, null, null, null, null)
        val v = value.trim().ifBlank { null }
        val updated = when (field) {
            "Name" -> facts.copy(name = v)
            "Business" -> facts.copy(business = v)
            "Where" -> facts.copy(where = v)
            "Serves" -> facts.copy(serves = v)
            else -> facts.copy(hours = v)
        }
        store.facts = updated
        _state.update { it.copy(facts = updated, dialog = null) }
    }

    fun goCapture() = _state.update { it.copy(screen = Screen.CAPTURE, captureForEdit = false, shooting = false, detect = null) }

    // Capture

    /** Opens the camera for one of the capture screen's tiles. */
    fun takePhoto(kind: PhotoKind) = _state.update { it.copy(captureMode = kind, shooting = true) }

    /** Sets which tile a photo from the picker is for; the UI then opens the picker. */
    fun choosePhoto(kind: PhotoKind) = _state.update { it.copy(captureMode = kind) }

    fun openSnap() = _state.update {
        it.copy(screen = Screen.CAPTURE, captureForEdit = true, shooting = true, captureMode = PhotoKind.FLYER, detect = null)
    }

    /** A photo from the camera ([file]) or the picker ([uri]) in the current capture mode. */
    fun onPhoto(file: File? = null, uri: Uri? = null) = work(showBusy = false) {
        val resolver = getApplication<Application>().contentResolver
        val source = file?.let { ImageDecoder.createSource(it) } ?: ImageDecoder.createSource(resolver, uri!!)
        val kind = _state.value.captureMode
        if (_state.value.captureForEdit) {
            // After launch the site refers to photos by index, so add rather than replace.
            val photo = photoStore.add(source, kind)
            file?.delete()
            val photos = photoStore.all()
            _state.update { it.copy(photos = photos, detect = "Got it · adding it to your site") }
            delay(900)
            _state.update { it.copy(screen = Screen.APP, tab = Tab.SITE, shooting = false, detect = null) }
            submitEdit(
                "Here's a photo of a flyer or sign (photo ${photos.size - 1}). Add what it announces to the site.",
                newPhotos = listOf(photo),
            )
            return@work
        }
        // One photo per tile: a new one replaces what was there.
        photoStore.replace(source, kind)
        file?.delete()
        val photos = photoStore.all()
        _state.update { it.copy(photos = photos, shooting = false, colors = previewColors(photos)) }
    }

    /** Colors from the logo (or storefront) photo, shown on the Logo tile before there's a site. */
    private fun previewColors(photos: List<Photo>): SiteColors {
        val source = photos.lastOrNull { it.kind == PhotoKind.LOGO } ?: photos.lastOrNull { it.kind == PhotoKind.STOREFRONT }
        return LogoColors.from(source?.file, null)
    }

    // Build

    fun startBuild() {
        val s = _state.value
        val kinds = s.capturedKinds
        val logoOrStorefront = photoStore.all().lastOrNull { it.kind == PhotoKind.LOGO }
            ?: photoStore.all().lastOrNull { it.kind == PhotoKind.STOREFRONT }
        val colors = LogoColors.from(logoOrStorefront?.file, null)
        val factCount = s.facts?.let { f -> listOfNotNull(f.name, f.business, f.where, f.serves, f.hours).size } ?: 0
        val steps = buildList {
            add(
                if (PhotoKind.LOGO in kinds) {
                    BuildStep("palette", "Pulling colors from your logo", "Your logo's colors, ready for headings and buttons", Where.DEVICE, StepState.DONE, colors.swatches)
                } else {
                    BuildStep(
                        "palette", "Choosing colors",
                        if (PhotoKind.STOREFRONT in kinds) "Taken from your storefront photo" else "A warm default palette",
                        Where.DEVICE, StepState.DONE, colors.swatches,
                    )
                },
            )
            if (PhotoKind.STOREFRONT in kinds) {
                add(BuildStep("image", "Preparing your storefront photo", "Sized for your Home page", Where.DEVICE, StepState.DONE))
            }
            add(
                BuildStep(
                    "record_voice_over", "Understanding your business",
                    s.facts?.business?.let { b -> "$b · $factCount details from what you said" } ?: "From what you said",
                    Where.CLOUD, StepState.ACTIVE,
                ),
            )
            add(
                if (PhotoKind.MENU in kinds) {
                    BuildStep("restaurant_menu", "Reading your menu board", "", Where.CLOUD)
                } else {
                    BuildStep("restaurant_menu", "No menu photo", "Skipped. Snap it any time.", null, StepState.SKIPPED)
                },
            )
            add(
                if (PhotoKind.FLYER in kinds) {
                    BuildStep("event", "Turning your flyer into an event", "", Where.CLOUD)
                } else {
                    BuildStep("event", "No flyer", "Events page starts empty", null, StepState.SKIPPED)
                },
            )
            add(BuildStep("edit_note", "Writing your pages", "", Where.CLOUD))
        }
        _state.update { it.copy(screen = Screen.BUILDING, steps = steps, buildDone = false, buildError = null, colors = colors) }

        viewModelScope.launch {
            // One Claude call does all the cloud steps; move the spinner along while it runs, but
            // never past the last step before the result is in.
            val ticker = launch {
                while (isActive) {
                    delay(7000)
                    _state.update { st ->
                        val active = st.steps.indexOfFirst { it.state == StepState.ACTIVE }
                        val next = st.steps.withIndex().indexOfFirst { (i, step) -> i > active && step.state == StepState.PENDING }
                        if (active < 0 || next < 0) st else st.copy(
                            steps = st.steps.mapIndexed { i, step ->
                                when (i) {
                                    active -> step.copy(state = StepState.DONE)
                                    next -> step.copy(state = StepState.ACTIVE)
                                    else -> step
                                }
                            },
                        )
                    }
                }
            }
            try {
                val site = withContext(Dispatchers.IO) {
                    val photos = photoStore.all()
                    SiteLook.withTaggedPhotos(ai().buildSite(store.description, store.facts, SiteLook.toRead(photos)), photos)
                }
                ticker.cancel()
                store.site = site
                store.activity = emptyList()
                val photos = photoStore.all()
                _state.update { st ->
                    st.copy(
                        site = site,
                        photos = photos,
                        colors = SiteLook.colors(site, photos),
                        activity = emptyList(),
                        buildDone = true,
                        steps = st.steps.map { step -> finishStep(step, site) },
                    )
                }
            } catch (e: Exception) {
                ticker.cancel()
                _state.update { st ->
                    st.copy(
                        buildError = e.message ?: e.toString(),
                        // Stop the spinner on the step that was running; the ones after it never started.
                        steps = st.steps.map { step ->
                            if (step.state == StepState.ACTIVE) step.copy(state = StepState.FAILED, detail = "Didn't finish") else step
                        },
                    )
                }
                askForNewKey(e, ::startBuild)
            }
        }
    }

    private fun finishStep(step: BuildStep, site: SiteModel): BuildStep {
        if (step.state == StepState.SKIPPED || step.where == Where.DEVICE) return step
        val detail = when (step.icon) {
            "restaurant_menu" -> site.menu.sumOf { it.items.size }.let { n -> "$n items in ${site.menu.size} sections → Menu page" }
            "event" -> site.events.firstOrNull()?.let { "${it.title} · ${EventDates.short(it.date)}" } ?: "No event found on the flyer"
            "edit_note" -> pageKeys(site).joinToString(" · ") { it.title }
            else -> step.detail
        }
        return step.copy(state = StepState.DONE, detail = detail)
    }

    fun pageKeys(site: SiteModel) = PageKey.entries.filter { key ->
        when (key) {
            PageKey.MENU -> site.menu.isNotEmpty()
            PageKey.EVENTS -> site.events.isNotEmpty()
            else -> true
        }
    }

    fun goReview() = _state.update { it.copy(screen = Screen.REVIEW, page = PageKey.HOME, highlight = emptySet()) }

    // Site

    fun setPage(page: PageKey) = _state.update { it.copy(page = page, highlight = emptySet()) }

    fun setTab(tab: Tab) = _state.update { it.copy(tab = tab, sheet = null) }

    fun toggleExpanded(id: Long) = _state.update { it.copy(expanded = if (it.expanded == id) null else id) }

    // Publish

    fun openPublish() = _state.update {
        it.copy(dialog = if (store.credentials != null) Dialog.Publish else Dialog.Connect(publishAfter = true))
    }

    fun connectAnother() = _state.update { it.copy(dialog = Dialog.Connect(publishAfter = it.screen == Screen.REVIEW)) }

    /** Discovers the site and opens the browser so the owner can approve access. */
    fun connect(siteUrl: String, publishAfter: Boolean, openBrowser: (String) -> Unit) = work {
        val discovery = AppPasswordAuth.discover(siteUrl.trim())
        store.pendingLogin = PendingLogin(discovery.siteUrl, discovery.apiRoot, publishAfter)
        withContext(Dispatchers.Main) { openBrowser(discovery.authorizeUrl) }
    }

    fun onAuthCallback(uri: Uri) = work {
        val pending = store.pendingLogin ?: return@work
        val credentials = AppPasswordAuth.credentials(uri.toString(), pending.siteUrl, pending.apiRoot)
        // Also verifies the credentials before they're saved.
        val blockTheme = WpSite(credentials).hasBlockTheme()
        store.credentials = credentials
        store.pendingLogin = null
        // A different site means the pages need to be created there from scratch.
        store.published = null
        _state.update {
            it.copy(
                siteUrl = credentials.siteUrl,
                published = false,
                dialog = null,
                error = if (blockTheme) null else "Your site's theme isn't a block theme, so pages may look plain. " +
                    "Switching to Twenty Twenty-Five in wp-admin gives the best result.",
            )
        }
        if (pending.publish) publish()
    }

    fun publish() {
        val site = store.site ?: return
        _state.update { it.copy(dialog = Dialog.Publishing) }
        viewModelScope.launch {
            try {
                store.published = withContext(Dispatchers.IO) {
                    publisher().publish(site, store.published ?: PublishedState())
                }
                val entry = ActivityEntry(
                    id = System.currentTimeMillis(),
                    time = System.currentTimeMillis(),
                    title = "Site published",
                    summary = host(store.credentials?.siteUrl),
                    changes = emptyList(),
                    before = null,
                    after = null,
                )
                store.activity = listOf(entry) + store.activity
                _state.update {
                    it.copy(
                        dialog = null, published = true, screen = Screen.APP, tab = Tab.SITE, page = PageKey.HOME,
                        highlight = emptySet(), activity = store.activity,
                    )
                }
                showSnack(Snack("${site.businessName} is live"))
            } catch (e: Exception) {
                _state.update { it.copy(dialog = null, error = e.message ?: e.toString()) }
            }
        }
    }

    // Voice edits

    fun openVoice() = _state.update { it.copy(sheet = VoiceSheet()) }

    fun closeSheet() {
        voice.cancel()
        _state.update { it.copy(sheet = null) }
    }

    fun sheetTyping() {
        voice.cancel()
        _state.update { it.copy(sheet = VoiceSheet(VoicePhase.TYPING)) }
    }

    fun sheetListen() {
        if (voice.state.value.listening) {
            voice.stop()
            return
        }
        if (!voice.isAvailable) {
            sheetTyping()
            return
        }
        _state.update { it.copy(sheet = VoiceSheet(VoicePhase.LISTENING)) }
        voice.start(
            continuous = false,
            onError = { message -> _state.update { it.copy(sheet = VoiceSheet(VoicePhase.TYPING), error = message) } },
            onDone = { text ->
                if (text.isBlank()) _state.update { it.copy(sheet = VoiceSheet()) } else submitEdit(text)
            },
        )
    }

    /** Sends a spoken or typed change to Claude, then applies it or asks for confirmation. */
    fun submitEdit(instruction: String, newPhotos: List<Photo> = emptyList()) {
        val site = store.site ?: return
        _state.update { it.copy(sheet = VoiceSheet(VoicePhase.THINKING, text = instruction)) }
        viewModelScope.launch {
            try {
                val (edit, changes, edited) = withContext(Dispatchers.IO) {
                    val all = photoStore.all()
                    val edit = ai().editSite(site, instruction, all.size - newPhotos.size, newPhotos)
                    val changes = SiteDiff.changes(site, edit.site)
                    val edited = if (changes.isNotEmpty() && store.published != null) {
                        publisher().externallyEdited(store.published!!)
                    } else {
                        emptyList()
                    }
                    Triple(edit, changes, edited)
                }
                val warning = edited.takeIf { it.isNotEmpty() }?.let { pages ->
                    "Someone edited ${pages.joinToString()} outside the app. Applying will replace those edits."
                }
                if (changes.isEmpty() || store.confirmFirst || warning != null) {
                    _state.update {
                        it.copy(sheet = VoiceSheet(VoicePhase.CONFIRM, instruction, edit, changes, warning))
                    }
                } else {
                    apply(edit, changes)
                }
            } catch (e: Exception) {
                if (askForNewKey(e) { submitEdit(instruction, newPhotos) }) {
                    _state.update { it.copy(sheet = null) }
                } else {
                    _state.update { it.copy(sheet = null, error = e.message ?: e.toString()) }
                }
            }
        }
    }

    fun confirmApply() {
        val sheet = _state.value.sheet ?: return
        apply(sheet.edit ?: return, sheet.changes)
    }

    private fun apply(edit: SiteEdit, changes: List<FieldChange>) {
        val before = store.site ?: return
        val entry = ActivityEntry(
            id = System.currentTimeMillis(),
            time = System.currentTimeMillis(),
            title = edit.title,
            summary = edit.summary,
            changes = changes,
            before = before,
            after = edit.site,
        )
        write(edit.site) {
            store.activity = listOf(entry) + store.activity
            val sections = SiteDiff.sections(changes)
            _state.update {
                it.copy(
                    sheet = null,
                    activity = store.activity,
                    highlight = sections,
                    page = sections.firstOrNull()?.page ?: it.page,
                    tab = Tab.SITE,
                )
            }
            showSnack(Snack(edit.summary, undoId = entry.id))
        }
    }

    /** Undo restores the sections a change touched; later changes to other sections survive. */
    fun setUndone(id: Long, undone: Boolean) {
        val entry = store.activity.find { it.id == id } ?: return
        val source = (if (undone) entry.before else entry.after) ?: return
        val current = store.site ?: return
        val sections = SiteDiff.sections(entry.changes)
        val restored = sections.fold(current) { site, section -> section.copy(from = source, into = site) }
        write(restored) {
            store.activity = store.activity.map { if (it.id == id) it.copy(undone = undone) else it }
            _state.update {
                it.copy(activity = store.activity, highlight = if (undone) emptySet() else sections)
            }
            showSnack(Snack(if (undone) "Change undone" else "Change restored"))
        }
    }

    /** Saves [site] and, once published, writes it to WordPress before calling [after]. */
    private fun write(site: SiteModel, after: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, sheet = it.sheet?.copy(phase = VoicePhase.THINKING)) }
            try {
                withContext(Dispatchers.IO) {
                    store.published?.let { store.published = publisher().publish(site, it) }
                }
                store.site = site
                _state.update { it.copy(site = site, colors = SiteLook.colors(site, photoStore.all()), busy = false) }
                after()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, sheet = null, error = e.message ?: e.toString()) }
            }
        }
    }

    fun snackUndo() {
        _state.value.snack?.undoId?.let { setUndone(it, true) }
    }

    private fun showSnack(snack: Snack) {
        snackJob?.cancel()
        _state.update { it.copy(snack = snack) }
        snackJob = viewModelScope.launch {
            delay(6000)
            _state.update { it.copy(snack = null) }
        }
    }

    // Account

    fun setConfirmFirst(on: Boolean) {
        store.confirmFirst = on
        _state.update { it.copy(confirmFirst = on) }
    }

    fun setOnDevice(on: Boolean) {
        store.onDevice = on
        _state.update { it.copy(onDevice = on) }
    }

    fun startOver() {
        voice.cancel()
        store.site = null
        store.published = null
        store.description = ""
        store.facts = null
        store.activity = emptyList()
        photoStore.clear()
        _state.update {
            UiState(
                screen = Screen.WELCOME,
                siteUrl = it.siteUrl,
                hasApiKey = it.hasApiKey,
                confirmFirst = it.confirmFirst,
                onDevice = it.onDevice,
                nanoAvailable = it.nanoAvailable,
            )
        }
    }

    private fun work(showBusy: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (showBusy) _state.update { it.copy(busy = true) }
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.toString()) }
            } finally {
                if (showBusy) _state.update { it.copy(busy = false) }
            }
        }
    }

    override fun onCleared() {
        voice.cancel()
    }

    companion object {
        fun host(url: String?) = url?.substringAfter("://")?.trimEnd('/').orEmpty()
    }
}
