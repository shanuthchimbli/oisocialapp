package chat.oi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chat.oi.core.database.entity.BlockedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedUserDao {

    @Query("SELECT blocked_user_id FROM blocked_users")
    fun getBlockedUserIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_users WHERE blocked_user_id = :userId)")
    suspend fun isUserBlocked(userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockUser(blockedUser: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blocked_user_id = :userId")
    suspend fun unblockUser(userId: String)
}
