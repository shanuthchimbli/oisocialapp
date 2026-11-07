package chat.oi.core.network.model

import chat.oi.core.crypto.EncryptedEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientWsFrame {

    @Serializable
    @SerialName("AUTH")
    data class Auth(val token: String) : ClientWsFrame

    @Serializable
    @SerialName("MSG_SUBMIT")
    data class MessageSubmit(
        val msgId: String,
        val recipientId: String,
        val envelope: EncryptedEnvelope,
        val clientTimestamp: Long = System.currentTimeMillis()
    ) : ClientWsFrame

    @Serializable
    @SerialName("MSG_DELIVERED_ACK")
    data class DeliveredAck(val msgId: String) : ClientWsFrame

    @Serializable
    @SerialName("MSG_READ_ACK")
    data class ReadAck(val msgId: String) : ClientWsFrame

    @Serializable
    @SerialName("PING")
    data object Ping : ClientWsFrame
}

@Serializable
sealed interface ServerWsFrame {

    @Serializable
    @SerialName("AUTH_ACK")
    data class AuthAck(val status: String) : ServerWsFrame

    @Serializable
    @SerialName("MSG_ACK")
    data class MessageAck(
        val msgId: String,
        val serverSeq: Long,
        val timestamp: Long
    ) : ServerWsFrame

    @Serializable
    @SerialName("MSG_DELIVER")
    data class MessageDeliver(
        val msgId: String,
        val senderId: String,
        val envelope: EncryptedEnvelope,
        val serverSeq: Long,
        val timestamp: Long
    ) : ServerWsFrame

    @Serializable
    @SerialName("MSG_DELIVERED_RECEIPT")
    data class DeliveredReceipt(
        val msgId: String,
        val timestamp: Long
    ) : ServerWsFrame

    @Serializable
    @SerialName("MSG_READ_RECEIPT")
    data class ReadReceipt(
        val msgId: String,
        val timestamp: Long
    ) : ServerWsFrame

    @Serializable
    @SerialName("SESSION_TERMINATED")
    data class SessionTerminated(val reason: String) : ServerWsFrame

    @Serializable
    @SerialName("PONG")
    data object Pong : ServerWsFrame
}
