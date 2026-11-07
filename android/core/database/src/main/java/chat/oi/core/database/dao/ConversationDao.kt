package chat.oi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chat.oi.core.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY last_message_timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE participant_id = :participantId LIMIT 1")
    suspend fun getConversationByParticipant(participantId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("""
        UPDATE conversations 
        SET last_message_preview = :preview, 
            last_message_timestamp = :timestamp,
            unread_count = unread_count + :unreadDelta
        WHERE id = :conversationId
    """)
    suspend fun updateLastMessage(
        conversationId: String,
        preview: String,
        timestamp: Long,
        unreadDelta: Int = 0
    )

    @Query("UPDATE conversations SET unread_count = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
}
