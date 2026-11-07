package chat.oi.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chat.oi.core.database.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {

    @Query("SELECT * FROM status_updates WHERE user_id = :userId LIMIT 1")
    fun observeUserStatus(userId: String): Flow<StatusEntity?>

    @Query("SELECT * FROM status_updates ORDER BY updated_at DESC")
    fun observeAllStatuses(): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: StatusEntity)

    @Query("DELETE FROM status_updates WHERE user_id = :userId")
    suspend fun deleteStatus(userId: String)
}
