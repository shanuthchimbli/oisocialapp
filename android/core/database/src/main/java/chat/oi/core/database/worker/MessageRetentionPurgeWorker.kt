package chat.oi.core.database.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import chat.oi.core.database.OiDatabase
import java.util.concurrent.TimeUnit

/**
 * Enforces the 365-Day Message Retention Lifecycle (PRD-007, FUNC-RET-002).
 * Runs every 24 hours to mathematically purge expired messages and zeroize storage via VACUUM.
 */
class MessageRetentionPurgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 365 Solar Days in milliseconds = 365 * 24 * 60 * 60 * 1000L
            val retentionDeltaMs = 365L * 24 * 60 * 60 * 1000L
            val cutoffTimestamp = System.currentTimeMillis() - retentionDeltaMs

            val database = OiDatabase.getInstance(applicationContext)
            
            val purgedRows = database.messageDao().deleteExpiredMessages(cutoffTimestamp)
            
            if (purgedRows > 0) {
                database.vacuum()
            }

            Result.success()
        } catch (_: Exception) {
            Log.e(TAG, "Retention purge failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "OiMessageRetentionPurgeWorker"
        private const val TAG = "OiRetentionPurge"

        fun schedule(context: Context) {
            val purgeRequest = PeriodicWorkRequestBuilder<MessageRetentionPurgeWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                purgeRequest
            )
        }
    }
}
