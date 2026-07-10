package com.urg.edge

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class DisasterCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (JmaAlertChecker.isDisasterOccurring()) {
            DisasterModeManager(applicationContext).setDisasterMode(true)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DisasterCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "disaster_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}