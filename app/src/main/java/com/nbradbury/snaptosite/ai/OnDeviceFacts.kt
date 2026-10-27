package com.nbradbury.snaptosite.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation

/**
 * Picks the basic facts out of what the owner says using Gemini Nano, so they can appear while
 * the owner is still talking without a network round trip. Only some phones have it; callers fall
 * back to [SiteAi.extractFacts].
 */
class OnDeviceFacts {
    private val model by lazy { Generation.getClient() }

    suspend fun isAvailable(): Boolean =
        runCatching { model.checkStatus() == FeatureStatus.AVAILABLE }.getOrDefault(false)

    suspend fun extract(spoken: String): HeardFacts? = runCatching {
        val reply = model.generateContent(PROMPT + spoken).candidates.firstOrNull()?.text ?: return null
        parse(reply)
    }.getOrNull()

    companion object {
        private val PROMPT = """
            Pick out these details from a business owner describing their business. Answer with
            exactly these five lines, writing "none" for anything they didn't say:
            Name: <business name>
            Business: <kind of business, e.g. Café>
            Where: <street or neighborhood>
            Serves: <what they sell, briefly>
            Hours: <opening hours, briefly>

            What they said:
        """.trimIndent() + "\n"

        /** Reads the five `Label: value` lines; small models don't reliably produce JSON. */
        fun parse(reply: String): HeardFacts {
            val values = reply.lines()
                .mapNotNull { line ->
                    val (label, value) = line.split(":", limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                    label.trim().trimStart('-', '*', ' ').lowercase() to value.trim()
                }
                .filter { (_, value) -> value.isNotEmpty() && !value.equals("none", ignoreCase = true) }
                .toMap()
            return HeardFacts(
                name = values["name"],
                business = values["business"],
                where = values["where"],
                serves = values["serves"],
                hours = values["hours"],
            )
        }
    }
}
