package chat.oi.core.domain.model

enum class DeliveryState {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class Message(
    val id: String, // Client-generated UUIDv7
    val senderId: String,
    val recipientId: String,
    val content: String, // Strict text-only, max 4,096 chars
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Long = 0L,
    val deliveryState: DeliveryState = DeliveryState.SENDING
)
