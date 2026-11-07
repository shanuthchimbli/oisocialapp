package chat.oi.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey
    @ColumnInfo(name = "blocked_user_id")
    val blockedUserId: String,

    @ColumnInfo(name = "blocked_at")
    val blockedAt: Long = System.currentTimeMillis()
)
