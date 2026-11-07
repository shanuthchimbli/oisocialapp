package chat.oi.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import chat.oi.core.database.dao.BlockedUserDao
import chat.oi.core.database.dao.ConversationDao
import chat.oi.core.database.dao.MessageDao
import chat.oi.core.database.dao.StatusDao
import chat.oi.core.database.entity.BlockedUserEntity
import chat.oi.core.database.entity.ConversationEntity
import chat.oi.core.database.entity.MessageEntity
import chat.oi.core.database.entity.StatusEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        StatusEntity::class,
        BlockedUserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OiDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun statusDao(): StatusDao
    abstract fun blockedUserDao(): BlockedUserDao

    fun vacuum() {
        openHelper.writableDatabase.execSQL("VACUUM;")
    }

    companion object {
        private const val DB_NAME = "oi_encrypted.db"

        @Volatile
        private var INSTANCE: OiDatabase? = null

        fun getInstance(context: Context): OiDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = DatabasePassphraseProvider.getOrCreate(context)
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OiDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
