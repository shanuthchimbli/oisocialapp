package chat.oi.feature.status.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.oi.feature.status.StatusIntent
import chat.oi.feature.status.StatusItem
import chat.oi.feature.status.StatusViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusViewState,
    onIntent: (StatusIntent) -> Unit,
    onNavigateChats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Oi",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Chats",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.clickable { onNavigateChats() }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // My Status Section
            item {
                Text(
                    text = "My Status",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (state.myStatus != null) {
                    StatusRow(
                        item = state.myStatus,
                        isSelf = true,
                        onEdit = { onIntent(StatusIntent.SetEditing(true)) }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(StatusIntent.SetEditing(true)) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = Color(0xFF38BDF8), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = "Add text status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Broadcast up to 140 characters", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                }

                Divider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 16.dp))
            }

            // Contacts Statuses Section
            item {
                Text(
                    text = "Recent Contact Updates",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(state.contactsStatuses, key = { it.id }) { item ->
                StatusRow(item = item, isSelf = false)
                Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
            }
        }
    }

    // Compose / Edit Status Dialog
    if (state.isEditing) {
        AlertDialog(
            onDismissRequest = { onIntent(StatusIntent.SetEditing(false)) },
            containerColor = Color(0xFF1E293B),
            title = {
                Text(
                    text = if (state.myStatus != null) "Update Status" else "New Status",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Text-only broadcast to mutual contacts. Zero photos or videos.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.statusDraft,
                        onValueChange = { onIntent(StatusIntent.UpdateDraft(it)) },
                        placeholder = { Text("What's on your mind? (max 140 chars)", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.statusDraft.length}/140",
                        color = if (state.statusDraft.length >= 130) Color(0xFFF87171) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onIntent(StatusIntent.SaveStatus) },
                    enabled = state.statusDraft.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text(text = "Post", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (state.myStatus != null) {
                        TextButton(onClick = { onIntent(StatusIntent.DeleteStatus) }) {
                            Text(text = "Clear", color = Color(0xFFF87171))
                        }
                    }
                    TextButton(onClick = { onIntent(StatusIntent.SetEditing(false)) }) {
                        Text(text = "Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        )
    }
}

@Composable
private fun StatusRow(
    item: StatusItem,
    isSelf: Boolean,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onEdit != null) { onEdit?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initial = item.displayName.take(1).uppercase()
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSelf) "You" else item.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (isSelf) {
                    Text(text = "Tap to edit", color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.text,
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp
            )
        }
    }
}
