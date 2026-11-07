package chat.oi.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoPrimitives {

    private val secureRandom = SecureRandom()
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun randomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /**
     * HKDF Extract and Expand (RFC 5869) using HMAC-SHA256
     */
    fun hkdf(salt: ByteArray, ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
        // Step 1: Extract
        val prk = hmacSha256(if (salt.isEmpty()) ByteArray(32) else salt, ikm)

        // Step 2: Expand
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        val okm = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var i = 1

        while (offset < length) {
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()

            val bytesToCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, bytesToCopy)
            offset += bytesToCopy
            i++
        }

        return okm
    }

    /**
     * AES-256-GCM Authenticated Encryption
     */
    fun encryptAesGcm(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): Pair<ByteArray, ByteArray> {
        val iv = randomBytes(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        val ciphertext = cipher.doFinal(plaintext)
        return Pair(iv, ciphertext)
    }

    /**
     * AES-256-GCM Authenticated Decryption
     */
    fun decryptAesGcm(key: ByteArray, iv: ByteArray, ciphertext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        if (associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        return cipher.doFinal(ciphertext)
    }

    /**
     * Zeroize memory buffer to prevent forensic memory dumps (MASVS-CRYPTO-1)
     */
    fun zeroize(bytes: ByteArray) {
        bytes.fill(0)
    }
}
