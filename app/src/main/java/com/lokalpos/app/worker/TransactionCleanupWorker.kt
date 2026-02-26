package com.lokalpos.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.lokalpos.app.data.AppDatabase
import com.lokalpos.app.data.repository.TransactionRepository
import com.lokalpos.app.util.SettingsManager
import java.util.concurrent.TimeUnit

class TransactionCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsManager(applicationContext)
            val days = settings.autoDeleteDays

            if (days <= 0) return Result.success()

            val cutoffTime = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
            val db = AppDatabase.getInstance(applicationContext)
            val repo = TransactionRepository(db.transactionDao())
            val deleted = repo.deleteOldTransactions(cutoffTime)
            Log.i("CleanupWorker", "Deleted $deleted transactions older than $days days")
            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "Cleanup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "transaction_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TransactionCleanupWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
