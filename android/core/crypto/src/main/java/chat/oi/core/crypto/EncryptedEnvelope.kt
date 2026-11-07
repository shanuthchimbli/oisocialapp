package chat.oi.core.crypto

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedEnvelope(
    val senderId: String,
    val ephemeralPublicKeyHex: String,
    val sequenceNumber: Long,
    val previousChainLength: Long,
    val ivHex: String,
    val ciphertextHex: String
)
