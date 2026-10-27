package com.nbradbury.snaptosite

import com.anthropic.core.jsonMapper
import com.anthropic.models.beta.messages.StructuredOutputConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.nbradbury.snaptosite.ai.HeardFacts
import com.nbradbury.snaptosite.ai.SiteAi
import com.nbradbury.snaptosite.ai.SiteEditResponse
import com.nbradbury.snaptosite.ai.SiteModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SchemaTest {
    /**
     * Derives each structured-output schema with the SDK (which validates it against the API's
     * rules) and checks it matches the asset the app ships. If a model class changed, this rewrites
     * the asset and fails; rerun and commit the updated file.
     */
    @Test
    fun `schema assets match the model classes`() {
        val stale = mutableListOf<String>()
        for (type in listOf(SiteModel::class.java, SiteEditResponse::class.java, HeardFacts::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val format = StructuredOutputConfig.builder<Any>().format(type as Class<Any>).build().format().get()
            val schema = jsonMapper().valueToTree<JsonNode>(format).get("schema")
            val expected = jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema) + "\n"
            val asset = File("src/main/assets", SiteAi.schemaAsset(type))
            if (asset.takeIf { it.exists() }?.readText() != expected) {
                asset.parentFile.mkdirs()
                asset.writeText(expected)
                stale += asset.path
            }
        }
        assertEquals("Regenerated stale schema assets", emptyList<String>(), stale)
    }

    @Test
    fun `site model round-trips through JSON`() {
        val json = SiteAi.MAPPER.writeValueAsString(cafe)
        assertEquals(cafe, SiteAi.MAPPER.readValue<SiteModel>(json))
    }
}
