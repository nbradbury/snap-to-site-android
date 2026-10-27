package com.nbradbury.snaptosite.ai

import android.content.res.AssetManager
import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.messages.BetaBase64ImageSource
import com.anthropic.models.beta.messages.BetaContentBlockParam
import com.anthropic.models.beta.messages.BetaFallbacksParam
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nbradbury.snaptosite.site.Photo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.jvm.optionals.getOrNull

open class SiteAiException(message: String) : Exception(message)

/** The API rejected the key, e.g. because it was deleted; the owner needs to enter a new one. */
class InvalidApiKeyException : SiteAiException("Your Claude API key isn't valid anymore.")

/** Turns the owner's description and photos into a [SiteModel], and applies spoken edits to it. */
class SiteAi(apiKey: String, private val assets: AssetManager) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build()

    /**
     * [photos] are only the ones with something to read (menus, flyers), keyed by their index
     * among all the owner's photos; the app places storefront and logo photos itself.
     */
    fun buildSite(description: String, facts: HeardFacts?, photos: Map<Int, Photo>): SiteModel {
        val confirmed = facts?.let { "\n\nDetails the owner confirmed:\n${MAPPER.writeValueAsString(it)}" }.orEmpty()
        val content = photoBlocks(photos) + BetaContentBlockParam.ofText(
            "Today is ${today()}.\n\nThe owner describes their business:\n\n$description$confirmed",
        )
        return request(SiteModel::class.java, BUILD_PROMPT, content, BetaOutputConfig.Effort.HIGH)
    }

    /** [newPhotos] are photos added with this edit; their indexes continue after [photoCount] existing ones. */
    fun editSite(site: SiteModel, instruction: String, photoCount: Int, newPhotos: List<Photo>): SiteEdit {
        val labeled = newPhotos.withIndex().associate { (i, photo) -> photoCount + i to photo }
        val content = photoBlocks(labeled) + BetaContentBlockParam.ofText(
            "Today is ${today()}. The site has $photoCount existing photos (indexes 0–${photoCount - 1}).\n\n" +
                "Current site:\n${MAPPER.writeValueAsString(site)}\n\nThe owner says: $instruction",
        )
        return request(SiteEdit::class.java, EDIT_PROMPT, content, BetaOutputConfig.Effort.MEDIUM)
    }

    /** Cloud fallback for [OnDeviceFacts] on phones without Gemini Nano. */
    fun extractFacts(spoken: String): HeardFacts = request(
        HeardFacts::class.java,
        "Pick out the details a business owner gave while describing their business. Leave anything they didn't say null.",
        listOf(BetaContentBlockParam.ofText(spoken)),
        BetaOutputConfig.Effort.LOW,
    )

    private fun today() = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

    private fun photoBlocks(photos: Map<Int, Photo>) = photos.flatMap { (index, photo) ->
        listOf(
            BetaContentBlockParam.ofText("Photo $index (the owner says it's their ${photo.kind.label}):"),
            BetaContentBlockParam.ofImage(
                BetaBase64ImageSource.builder()
                    .mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG)
                    .data(Base64.getEncoder().encodeToString(photo.file.readBytes()))
                    .build(),
            ),
        )
    }

    private fun <T : Any> request(
        type: Class<T>,
        system: String,
        content: List<BetaContentBlockParam>,
        effort: BetaOutputConfig.Effort,
    ): T {
        val params = MessageCreateParams.builder()
            .model(Model.CLAUDE_OPUS_5_5)
            .maxTokens(16000)
            .system(system)
            // Retry on another model server-side if a safety classifier declines the request.
            .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
            .fallbacks(BetaFallbacksParam.ofDefault())
            .addUserMessageOfContentBlockParams(content)
            .outputConfig(BetaOutputConfig.builder().format(schemaFor(type)).effort(effort).build())
            .build()
        val message = try {
            client.beta().messages().create(params)
        } catch (e: UnauthorizedException) {
            throw InvalidApiKeyException()
        } catch (e: PermissionDeniedException) {
            throw SiteAiException("This API key doesn't have access to Claude Opus 5.5.")
        } catch (e: RateLimitException) {
            throw SiteAiException("Claude is busy right now. Try again in a minute.")
        } catch (e: AnthropicServiceException) {
            Log.w(TAG, "Claude request failed: ${e.message}", e)
            throw SiteAiException("Claude returned an error (${e.statusCode()}): ${apiMessage(e)}")
        } catch (e: AnthropicIoException) {
            throw SiteAiException("Couldn't reach Claude. Check your connection.")
        }
        when (message.stopReason().orElse(null)) {
            BetaStopReason.REFUSAL -> throw SiteAiException(
                message.stopDetails().flatMap { it.explanation() }.orElse("Claude declined this request."),
            )
            BetaStopReason.MAX_TOKENS -> throw SiteAiException("The response was too long. Try fewer photos.")
            else -> Unit
        }
        val json = message.content().firstNotNullOfOrNull { it.text().orElse(null)?.text() }
            ?: throw SiteAiException("Claude didn't return a site.")
        return MAPPER.readValue(json, type)
    }

    /** The API's own explanation from an error body, e.g. which parameter it rejected. */
    private fun apiMessage(e: AnthropicServiceException): String =
        e.body().asObject().getOrNull()?.get("error")?.asObject()?.getOrNull()?.get("message")?.asString()?.getOrNull()
            ?: "Try again."

    /**
     * The SDK can derive a schema from a class, but that needs reflection APIs Android lacks, so the
     * schemas are derived on the JVM by SchemaTest and shipped as assets.
     */
    private fun schemaFor(type: Class<*>): BetaJsonOutputFormat {
        val schema: Map<String, Any?> = assets.open(schemaAsset(type)).use { MAPPER.readValue(it) }
        return BetaJsonOutputFormat.builder()
            .schema(
                BetaJsonOutputFormat.Schema.builder()
                    .additionalProperties(schema.mapValues { JsonValue.from(it.value) })
                    .build(),
            )
            .build()
    }

    companion object {
        val MAPPER = jacksonObjectMapper()

        private const val TAG = "SiteAi"

        fun schemaAsset(type: Class<*>) = "schemas/${type.simpleName}.json"

        private val BUILD_PROMPT = """
            You build small-business websites from what an owner tells you and photos of their menu
            boards and flyers. Produce the site's content.

            - Use only facts from the description and photos. Leave a field null or a list empty
              rather than inventing prices, hours, addresses or events.
            - Transcribe menus and price lists from photos into the menu, keeping prices exactly as
              written. Read flyers into events, and give each event its flyer's photo index.
            - Leave hero.photo, about.photo and logoPhoto null; the app fills them in from the
              storefront and logo photos the owner took.
            - Group days with the same hours, e.g. "Tuesday – Sunday": "7am – 3pm" and "Monday":
              "Closed".
            - Event dates are YYYY-MM-DD. Work out the year from today's date when a flyer leaves it
              out.
            - Write warmly and plainly, in the owner's voice, the way a good local business sounds.
              Keep copy short: people read these sites on their phones.
            - The description is a speech transcript, so expect filler words and recognition errors.
        """.trimIndent()

        private val EDIT_PROMPT = """
            You maintain a small-business website on the owner's behalf. They tell you what changed,
            usually by voice, and you return the complete updated site plus a one-sentence summary.

            - Change only what the owner asked for and what follows directly from it. Leave
              everything else exactly as it is, including photo indexes.
            - A temporary change (a holiday closure, a sold-out item) should be reflected where it
              applies and announced in the banner, with the specific date. A closure on a particular
              day goes in the hours as a dated exception, e.g. "Thursday, Nov 26": "Closed ·
              Thanksgiving", rather than changing the regular hours.
            - When the owner says something is over or back to normal, remove the banner and restore
              the regular details.
            - Photos added with this edit are labeled with their indexes; use them where they fit. A
              photographed flyer usually means a new event.
            - Use only facts the owner gave you. The instruction is a speech transcript, so expect
              recognition errors.
        """.trimIndent()
    }
}
