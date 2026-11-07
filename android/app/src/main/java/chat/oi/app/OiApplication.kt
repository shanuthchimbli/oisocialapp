package chat.oi.app

import android.app.Application
import chat.oi.core.database.worker.MessageRetentionPurgeWorker

class OiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule daily automated 365-day message retention purge worker (PRD-007, FUNC-RET-002)
        MessageRetentionPurgeWorker.schedule(this)
    }
}
