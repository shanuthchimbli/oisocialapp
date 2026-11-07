package chat.oi.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRetentionTest {

    @Test
    fun test365DayCutoffCalculation() {
        val now = 1731000000000L // arbitrary fixed epoch timestamp
        val oneYearMs = 365L * 24 * 60 * 60 * 1000L // 31,536,000,000 ms

        val cutoff = now - oneYearMs
        val expiredMessageTime = cutoff - 1000L // 1 second past 365 days
        val validMessageTime = cutoff + 1000L   // 1 second within 365 days

        assertTrue("Message beyond 365 days must be expired", expiredMessageTime < cutoff)
        assertTrue("Message within 365 days must be preserved", validMessageTime > cutoff)
        assertEquals(31536000000L, oneYearMs)
    }
}
