package chat.oi.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["participant_id"], unique = true),
        Index(value = ["last_message_timestamp"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "participant_id")
    val participantId: String,

    @ColumnInfo(name = "participant_username")
    val participantUsername: String,

    @ColumnInfo(name = "participant_display_name")
    val participantDisplayName: String,

    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String,

    @ColumnInfo(name = "last_message_timestamp")
    val lastMessageTimestamp: Long,

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0
)
