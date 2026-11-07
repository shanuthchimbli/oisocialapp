package chat.oi.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleRatchetEngineTest {

    @Test
    fun testBidirectionalDoubleRatchetSession() {
        // Shared master secret established out-of-band via X3DH key agreement
        val sharedMasterSecret = CryptoPrimitives.randomBytes(32)

        val aliceEngine = DoubleRatchetEngine.initializeSession(sharedMasterSecret, isInitiator = true)
        val bobEngine = DoubleRatchetEngine.initializeSession(sharedMasterSecret, isInitiator = false)

        // 1. Alice sends first message to Bob
        val alicePlaintext1 = "Hey Bob! 1:1 text only, verified human identity."
        val envelope1 = aliceEngine.encrypt("alice_uuid", alicePlaintext1.toByteArray(Charsets.UTF_8))

        assertNotNull(envelope1.ciphertextHex)
        assertNotNull(envelope1.ivHex)
        assertEquals(0L, envelope1.sequenceNumber)

        // Bob decrypts Alice's message
        val bobDecrypted1 = bobEngine.decrypt(envelope1)
        assertEquals(alicePlaintext1, String(bobDecrypted1, Charsets.UTF_8))

        // 2. Bob sends reply to Alice (Triggers DH Ratchet turn)
        val bobPlaintext = "Got it Alice. Zero plaintext on the relay."
        val envelope2 = bobEngine.encrypt("bob_uuid", bobPlaintext.toByteArray(Charsets.UTF_8))

        // Alice decrypts Bob's reply
        val aliceDecrypted = aliceEngine.decrypt(envelope2)
        assertEquals(bobPlaintext, String(aliceDecrypted, Charsets.UTF_8))

        // 3. Alice sends second message (Advances symmetric chain)
        val alicePlaintext2 = "Perfect! The ratchet is rolling."
        val envelope3 = aliceEngine.encrypt("alice_uuid", alicePlaintext2.toByteArray(Charsets.UTF_8))

        val bobDecrypted2 = bobEngine.decrypt(envelope3)
        assertEquals(alicePlaintext2, String(bobDecrypted2, Charsets.UTF_8))
    }

    @Test
    fun testAesGcmAuthenticatedEncryption() {
        val key = CryptoPrimitives.randomBytes(32)
        val original = "Test payload for AES-256-GCM authenticated encryption.".toByteArray(Charsets.UTF_8)
        val aad = "auth_data_123".toByteArray(Charsets.UTF_8)

        val (iv, ciphertext) = CryptoPrimitives.encryptAesGcm(key, original, aad)
        val decrypted = CryptoPrimitives.decryptAesGcm(key, iv, ciphertext, aad)

        assertTrue(original.contentEquals(decrypted))
    }
}
