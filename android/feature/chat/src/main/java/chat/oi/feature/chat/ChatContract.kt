package chat.oi.feature.chat

import chat.oi.core.database.entity.ConversationEntity
import chat.oi.core.domain.model.Message

data class ChatViewState(
    val conversations: List<ConversationEntity> = emptyList(),
    val currentConversationId: String? = null,
    val currentParticipantUsername: String? = null,
    val messages: List<Message> = emptyList(),
    val inputDraft: String = "",
    val isSending: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ChatIntent {
    data class SelectConversation(val conversationId: String, val username: String) : ChatIntent
    data class UpdateDraft(val text: String) : ChatIntent
    data object SendMessage : ChatIntent
    data class BlockContact(val participantId: String) : ChatIntent
    data object ClearError : ChatIntent
}

sealed interface ChatSideEffect {
    data class ShowToast(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
}
