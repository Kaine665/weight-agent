package com.weightagent.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weightagent.app.WeightAgentApp
class RefreshAndEnqueueWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val container = (appContext.applicationContext as WeightAgentApp).container

    override suspend fun doWork(): Result {
        container.mediaStoreScanner.refreshFromMediaStore()

        val settings = container.cosSettingsStore.read()
        if (settings == null || !settings.isComplete()) {
            return Result.success()
        }

        val wm = WorkManager.getInstance(applicationContext)
        val pending = container.recordingDao.listNeedingUpload()
        for (row in pending) {
            val req = UploadRecordingWorker.buildRequest(row.mediaStoreId)
            wm.enqueueUniqueWork(
                UploadRecordingWorker.uniqueWorkName(row.mediaStoreId),
                ExistingWorkPolicy.KEEP,
                req,
            )
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "refresh_and_enqueue"

        fun buildRequest(): androidx.work.OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<RefreshAndEnqueueWorker>().build()
    }
}
