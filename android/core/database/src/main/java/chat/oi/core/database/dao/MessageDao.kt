package chat.oi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chat.oi.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /**
     * Retrieves messages within the 365-day retention lifecycle.
     * Enforces the mathematical exclusion barrier defined in PRD-007 / FUNC-RET-003.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversation_id = :conversationId 
          AND created_at >= :retentionCutoff
        ORDER BY created_at ASC
    """)
    fun getMessages(conversationId: String, retentionCutoff: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET delivery_state = :state WHERE id = :messageId")
    suspend fun updateDeliveryState(messageId: String, state: String)

    @Query("UPDATE messages SET delivery_state = :state, server_seq = :seq WHERE id = :messageId")
    suspend fun markSent(messageId: String, state: String, seq: Long)

    @Query("SELECT * FROM messages WHERE delivery_state = 'SENDING' ORDER BY created_at ASC")
    suspend fun getPendingMessages(): List<MessageEntity>

    /**
     * Permanently purges messages older than 365 days.
     * Executed by MessageRetentionPurgeWorker daily.
     */
    @Query("DELETE FROM messages WHERE created_at < :retentionCutoff")
    suspend fun deleteExpiredMessages(retentionCutoff: Long): Int

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)
}
