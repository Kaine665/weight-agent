package com.weightagent.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.weightagent.app.data.db.RecordingDao
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MediaStoreAudioScanner(
    private val context: Context,
    private val recordingDao: RecordingDao,
) {

    /**
     * 扫描系统媒体库中的音频条目。
     *
     * 说明：不少系统录音机写入的条目 MIME 为 null、application/octet-stream 或非 audio 前缀，
     * 若用 SQL 过滤 `audio%` 会漏掉。此处不按 MIME 在 SQL 里过滤，仅排除仍在写入的 pending。
     * 多卷（SD 等）在 API 30+ 上逐个查询。
     */
    suspend fun refreshFromMediaStore(): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.IS_PENDING} = 0"
        } else {
            null
        }
        var total = 0
        for (volume in externalVolumeNames()) {
            val collection = MediaStore.Audio.Media.getContentUri(volume)
            resolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                while (cursor.moveToNext()) {
                    val mime = cursor.getString(mimeCol)
                    if (mime != null && mime.startsWith("video/", ignoreCase = true)) {
                        continue
                    }
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "recording"
                    val duration = cursor.getLong(durCol)
                    val modifiedSec = cursor.getLong(modCol)
                    val size = cursor.getLong(sizeCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val existing = recordingDao.getById(id)
                    val uuid = existing?.recordingUuid ?: UUID.randomUUID().toString()
                    val status = existing?.syncStatus ?: SyncStatus.PENDING
                    val entity = RecordingEntity(
                        mediaStoreId = id,
                        recordingUuid = uuid,
                        displayName = name,
                        durationMs = duration,
                        dateModifiedMs = modifiedSec * 1000L,
                        sizeBytes = size,
                        contentUri = uri.toString(),
                        syncStatus = status,
                        objectKey = existing?.objectKey,
                        etag = existing?.etag,
                        remoteSizeBytes = existing?.remoteSizeBytes,
                        lastError = existing?.lastError,
                        lastStableSizeBytes = existing?.lastStableSizeBytes,
                        lastStableCheckMs = existing?.lastStableCheckMs,
                        contentSha256 = existing?.contentSha256,
                    )
                    recordingDao.upsertScanRow(entity)
                    total++
                }
            }
        }
        total
    }

    private fun externalVolumeNames(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (sequenceOf(MediaStore.VOLUME_EXTERNAL_PRIMARY) +
                MediaStore.getExternalVolumeNames(context).asSequence())
                .distinct()
                .toList()
        } else {
            listOf(MediaStore.VOLUME_EXTERNAL)
        }
    }
}
