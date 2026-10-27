package com.nbradbury.snaptosite

import com.nbradbury.snaptosite.ai.HeardFacts
import com.nbradbury.snaptosite.ai.OnDeviceFacts
import org.junit.Assert.assertEquals
import org.junit.Test

class OnDeviceFactsTest {
    @Test
    fun `parses the five lines and drops none`() {
        val reply = """
            Name: Juniper & Rye
            - Business: Café
            Where: Alder Street
            Serves: none
            Hours: 7am – 3pm, closed Mondays
        """.trimIndent()
        assertEquals(
            HeardFacts("Juniper & Rye", "Café", "Alder Street", null, "7am – 3pm, closed Mondays"),
            OnDeviceFacts.parse(reply),
        )
    }
}
