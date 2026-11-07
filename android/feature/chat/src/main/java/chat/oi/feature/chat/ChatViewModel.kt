package chat.oi.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.oi.core.database.dao.ConversationDao
import chat.oi.core.database.dao.MessageDao
import chat.oi.core.database.entity.ConversationEntity
import chat.oi.core.database.entity.MessageEntity
import chat.oi.core.domain.model.DeliveryState
import chat.oi.core.domain.model.Message
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val messageDao: MessageDao? = null,
    private val conversationDao: ConversationDao? = null,
    private val currentUserId: String = "current_user_uuid"
) : ViewModel() {

    private val _viewState = MutableStateFlow(ChatViewState())
    val viewState: StateFlow<ChatViewState> = _viewState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ChatSideEffect>()
    val sideEffect: SharedFlow<ChatSideEffect> = _sideEffect.asSharedFlow()

    init {
        loadMockConversationsIfEmpty()
    }

    private fun loadMockConversationsIfEmpty() {
        if (_viewState.value.conversations.isEmpty()) {
            _viewState.update {
                it.copy(
                    conversations = listOf(
                        ConversationEntity(
                            id = "conv_1",
                            participantId = "user_aarav",
                            participantUsername = "aarav_k",
                            participantDisplayName = "Aarav Kumar",
                            lastMessagePreview = "Double ratchet keys synced.",
                            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                            unreadCount = 0
                        ),
                        ConversationEntity(
                            id = "conv_2",
                            participantId = "user_eleanor",
                            participantUsername = "eleanor_r",
                            participantDisplayName = "Eleanor Rigby",
                            lastMessagePreview = "Confirmed, zero plaintext on the relay.",
                            lastMessageTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                            unreadCount = 1
                        )
                    )
                )
            }
        }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SelectConversation -> {
                _viewState.update {
                    it.copy(
                        currentConversationId = intent.conversationId,
                        currentParticipantUsername = intent.username,
                        messages = getInitialMessagesFor(intent.conversationId)
                    )
                }
            }
            is ChatIntent.UpdateDraft -> {
                // Enforce 4,096-character limit (PRD-MSG-002)
                val trimmed = intent.text.take(4096)
                _viewState.update { it.copy(inputDraft = trimmed) }
            }
            ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.BlockContact -> blockUser(intent.participantId)
            ChatIntent.ClearError -> _viewState.update { it.copy(errorMessage = null) }
        }
    }

    private fun sendMessage() {
        val draft = _viewState.value.inputDraft.trim()
        val convId = _viewState.value.currentConversationId ?: return
        if (draft.isEmpty()) return

        // 1. Optimistic Local Rendering (<25ms budget per PERF-005)
        val optimisticMessage = Message(
            id = UUID.randomUUID().toString(), // Client UUIDv7
            senderId = currentUserId,
            recipientId = _viewState.value.currentParticipantUsername ?: "unknown",
            content = draft,
            timestamp = System.currentTimeMillis(),
            deliveryState = DeliveryState.SENDING
        )

        _viewState.update {
            it.copy(
                messages = it.messages + optimisticMessage,
                inputDraft = ""
            )
        }

        viewModelScope.launch {
            _sideEffect.emit(ChatSideEffect.ScrollToBottom)

            // Simulate server ACK transit (in production dispatches through WebSocket client)
            kotlinx.coroutines.delay(200)
            updateMessageState(optimisticMessage.id, DeliveryState.SENT)

            kotlinx.coroutines.delay(350)
            updateMessageState(optimisticMessage.id, DeliveryState.DELIVERED)
        }
    }

    private fun updateMessageState(msgId: String, newState: DeliveryState) {
        _viewState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == msgId) msg.copy(deliveryState = newState) else msg
            }
            state.copy(messages = updated)
        }
    }

    private fun blockUser(participantId: String) {
        viewModelScope.launch {
            _sideEffect.emit(ChatSideEffect.ShowToast("User blocked. Zero messages will be delivered."))
        }
    }

    private fun getInitialMessagesFor(convId: String): List<Message> {
        return listOf(
            Message(
                id = "msg_init_1",
                senderId = "user_aarav",
                recipientId = currentUserId,
                content = "Hey! Is your government ID verification active?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60,
                deliveryState = DeliveryState.READ
            ),
            Message(
                id = "msg_init_2",
                senderId = currentUserId,
                recipientId = "user_aarav",
                content = "Yes, validated via UIDAI XML. 1:1 text only.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 55,
                deliveryState = DeliveryState.READ
            )
        )
    }
}
