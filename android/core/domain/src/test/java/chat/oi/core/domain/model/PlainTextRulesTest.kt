package chat.oi.core.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainTextRulesTest {
    @Test
    fun statusUsesUnicodeCodePointsAndRejectsMarkupAndLinks() {
        assertTrue(PlainTextRules.isValidStatus("🙂".repeat(140)))
        assertFalse(PlainTextRules.isValidStatus("🙂".repeat(141)))
        assertFalse(PlainTextRules.isValidStatus("   "))
        assertFalse(PlainTextRules.isValidStatus("<script>alert(1)</script>"))
        assertFalse(PlainTextRules.isValidStatus("[site](https://example.com)"))
        assertFalse(PlainTextRules.isValidStatus("https://example.com"))
    }

    @Test
    fun messagesAllowAtMost4096UnicodeCodePoints() {
        assertTrue(PlainTextRules.isValidMessage("🙂".repeat(4096)))
        assertFalse(PlainTextRules.isValidMessage("🙂".repeat(4097)))
        assertFalse(PlainTextRules.isValidMessage(" \n "))
    }
}
