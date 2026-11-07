package chat.oi.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import chat.oi.core.domain.model.DeliveryState
import chat.oi.core.domain.model.Message

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversation_id"]),
        Index(value = ["created_at"]),
        Index(value = ["delivery_state"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUIDv7

    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "recipient_id")
    val recipientId: String,

    @ColumnInfo(name = "ciphertext")
    val ciphertext: String,

    @ColumnInfo(name = "plaintext_content")
    val plaintextContent: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "server_seq")
    val serverSeq: Long = 0L,

    @ColumnInfo(name = "delivery_state")
    val deliveryState: String = DeliveryState.SENDING.name
) {
    fun toDomain(): Message {
        return Message(
            id = id,
            senderId = senderId,
            recipientId = recipientId,
            content = plaintextContent,
            timestamp = createdAt,
            sequenceNumber = serverSeq,
            deliveryState = DeliveryState.valueOf(deliveryState)
        )
    }

    companion object {
        fun fromDomain(conversationId: String, message: Message, ciphertext: String): MessageEntity {
            return MessageEntity(
                id = message.id,
                conversationId = conversationId,
                senderId = message.senderId,
                recipientId = message.recipientId,
                ciphertext = ciphertext,
                plaintextContent = message.content,
                createdAt = message.timestamp,
                serverSeq = message.sequenceNumber,
                deliveryState = message.deliveryState.name
            )
        }
    }
}
