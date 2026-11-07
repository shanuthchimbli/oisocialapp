package chat.oi.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.oi.core.domain.model.DeliveryState
import chat.oi.core.domain.model.Message
import chat.oi.feature.chat.ChatIntent
import chat.oi.feature.chat.ChatViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    participantUsername: String,
    state: ChatViewState,
    onIntent: (ChatIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "@$participantUsername",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "End-to-End Encrypted (Signal Ratchet)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { onNavigateBack() }
                            .padding(16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        bottomBar = {
            ChatInputBar(
                draft = state.inputDraft,
                errorMessage = state.errorMessage,
                onDraftChanged = { onIntent(ChatIntent.UpdateDraft(it)) },
                onSend = { onIntent(ChatIntent.SendMessage) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                val isMe = message.senderId != participantUsername
                MessageBubble(message = message, isOutgoing = isMe)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOutgoing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isOutgoing) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isOutgoing) 12.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 12.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOutgoing) {
                        DeliveryTickIndicator(state = message.deliveryState)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryTickIndicator(state: DeliveryState) {
    val (symbol, color) = when (state) {
        DeliveryState.SENDING -> "🕒" to Color(0xFF64748B)
        DeliveryState.SENT -> "✓" to Color(0xFF94A3B8)
        DeliveryState.DELIVERED -> "✓✓" to Color(0xFF94A3B8)
        DeliveryState.READ -> "✓✓" to Color(0xFF38BDF8) // Solid cyan read receipt (PRD-MSG-002)
    }

    Text(
        text = symbol,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ChatInputBar(
    draft: String,
    errorMessage: String?,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFF87171),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Strict text-only input: zero attachment clips, zero camera icons, zero audio mics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0F172A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Text message (max 4,096 chars)...",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChanged,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (draft.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF334155),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = draft.isNotBlank()) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (draft.isNotBlank()) Color(0xFF0F172A) else Color(0xFF64748B)
                )
            }
        }
    }
}
