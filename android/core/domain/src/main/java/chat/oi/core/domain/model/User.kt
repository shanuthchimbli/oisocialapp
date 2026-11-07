package chat.oi.core.domain.model

data class User(
    val id: String, // Immutable UUIDv7 internal identifier
    val phoneNumber: String, // E.164 international phone number
    val displayName: String,
    val username: String, // Alphanumeric unique handle
    val accountState: AccountState = AccountState.PROVISIONAL,
    val isVerified: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
