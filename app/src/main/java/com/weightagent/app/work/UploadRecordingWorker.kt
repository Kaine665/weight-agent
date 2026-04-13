package com.weightagent.app.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.weightagent.app.WeightAgentApp
import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.data.db.RecordingDao
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.data.mediastore.MediaStoreSizeReader
import com.weightagent.app.data.upload.UriToFile
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

class UploadRecordingWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters,
) : androidx.work.CoroutineWorker(appContext, params) {

    private val container = (appContext.applicationContext as WeightAgentApp).container

    override suspend fun doWork(): Result {
        val mediaStoreId = inputData.getLong(KEY_MEDIA_STORE_ID, -1L)
        if (mediaStoreId < 0) return Result.failure()

        val dao = container.recordingDao
        val gateway = container.cloudUploadGateway

        if (!gateway.isReadyToUpload()) {
            markWaitingForConfig(dao, mediaStoreId)
            return Result.success()
        }

        val entity = dao.getById(mediaStoreId) ?: return Result.success()
        if (entity.syncStatus == SyncStatus.SYNCED) {
            return Result.success()
        }

        val stable = awaitStableSize(applicationContext, entity.contentUri)
        if (!stable.ok) {
            enqueueDelayed(mediaStoreId, seconds = 3)
            return Result.success()
        }

        val working = entity.copy(
            syncStatus = SyncStatus.UPLOADING,
            sizeBytes = stable.sizeBytes,
            lastError = null,
        )
        dao.update(working)

        val kind = container.cloudStorageSelectionStore.readKindOrNull()
            ?: return Result.success()
        val cosSettings = container.cosSettingsStore.read()
        val aliyunSettings = container.aliyunDriveSettingsStore.read()
        val remotePath = UploadRemotePath.build(kind, cosSettings, aliyunSettings, entity)

        return try {
            val cacheName = "upload_${entity.recordingUuid}.bin"
            val localFile: File = UriToFile.copyToCache(
                applicationContext,
                android.net.Uri.parse(entity.contentUri),
                cacheName,
            )
            val outcome = gateway.uploadFile(localFile.absolutePath, remotePath)
            runCatching { localFile.delete() }

            val done = entity.copy(
                syncStatus = SyncStatus.SYNCED,
                objectKey = remotePath,
                etag = outcome.etag,
                remoteSizeBytes = stable.sizeBytes,
                lastError = null,
                sizeBytes = stable.sizeBytes,
            )
            dao.update(done)
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "upload failed for mediaStoreId=$mediaStoreId", t)
            val failed = entity.copy(
                syncStatus = SyncStatus.FAILED,
                objectKey = remotePath,
                lastError = humanMessage(t),
            )
            dao.update(failed)
            Result.retry()
        }
    }

    private suspend fun markWaitingForConfig(dao: RecordingDao, mediaStoreId: Long) {
        val e = dao.getById(mediaStoreId) ?: return
        if (e.syncStatus == SyncStatus.SYNCED) return
        val hint = when (container.cloudStorageSelectionStore.readKindOrNull()) {
            null -> "请先在「选择云端」中选定上传方式并完成配置"
            CloudStorageKind.OBJECT_COS -> "请先完成腾讯云 COS 配置并保存"
            CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> "请先完成阿里云盘配置并保存 refresh_token"
        }
        dao.update(
            e.copy(
                syncStatus = SyncStatus.PAUSED,
                lastError = hint,
            ),
        )
    }

    private data class StableSize(val ok: Boolean, val sizeBytes: Long)

    private suspend fun awaitStableSize(context: Context, contentUri: String): StableSize {
        val first = MediaStoreSizeReader.readSizeBytes(context, contentUri) ?: return StableSize(false, 0L)
        delay(2_000)
        val second = MediaStoreSizeReader.readSizeBytes(context, contentUri) ?: return StableSize(false, 0L)
        return if (first == second && first > 0L) {
            StableSize(true, first)
        } else {
            StableSize(false, 0L)
        }
    }

    private fun enqueueDelayed(mediaStoreId: Long, seconds: Long) {
        val req = OneTimeWorkRequestBuilder<UploadRecordingWorker>()
            .setInputData(workDataOf(KEY_MEDIA_STORE_ID to mediaStoreId))
            .setInitialDelay(seconds, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            uniqueWorkName(mediaStoreId),
            androidx.work.ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    companion object {
        const val KEY_MEDIA_STORE_ID = "media_store_id"
        private const val TAG = "UploadRecordingWorker"

        fun uniqueWorkName(mediaStoreId: Long) = "upload_recording_$mediaStoreId"

        fun buildRequest(mediaStoreId: Long): androidx.work.OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UploadRecordingWorker>()
                .setInputData(workDataOf(KEY_MEDIA_STORE_ID to mediaStoreId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        internal fun buildObjectKey(prefix: String, entity: RecordingEntity): String {
            val safeName = entity.displayName
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .ifBlank { "audio" }
            val ext = if ('.' in safeName) "" else ".m4a"
            return "${prefix}${entity.recordingUuid}_$safeName$ext"
        }

        private fun humanMessage(t: Throwable): String {
            val raw = (t.message ?: t.javaClass.simpleName).trim()
            return when {
                raw.contains("403", ignoreCase = true) -> "访问被拒绝（403），请检查子账号权限与桶策略"
                raw.contains("404", ignoreCase = true) -> "存储桶不存在或地域不匹配（404）"
                raw.contains("timeout", ignoreCase = true) -> "连接超时，请检查网络"
                raw.isNotBlank() -> "上传失败：$raw"
                else -> "上传失败，请稍后重试"
            }
        }
    }
}
