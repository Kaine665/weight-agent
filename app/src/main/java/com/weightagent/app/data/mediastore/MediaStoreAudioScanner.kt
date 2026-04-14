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
     * - **IS_PENDING**：在 SQL 里写 `IS_PENDING = 0` 会把 **NULL** 行排除（三值逻辑），部分 OEM 上录音行
     *   `IS_PENDING` 为 NULL，导致整表为空；改为 `(IS_PENDING IS NULL OR IS_PENDING = 0)`。
     * - **MediaStore.Files**：小米等机型上录音有时只出现在 **Files** 索引、不在 **Audio.Media**；一并扫描
     *   `MEDIA_TYPE_AUDIO`。Files 行使用合成主键 `-(fileId + FILES_ID_BIAS)` 写入 Room，避免与 `Audio.Media._ID` 冲突。
     */
    suspend fun refreshFromMediaStore(): Int = withContext(Dispatchers.IO) {
        var total = 0
        for (volume in externalVolumeNames()) {
            total += scanAudioMedia(volume)
            total += scanFilesAudio(volume)
        }
        total
    }

    private suspend fun scanAudioMedia(volume: String): Int {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(volume)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "(${MediaStore.Audio.Media.IS_PENDING} IS NULL OR ${MediaStore.Audio.Media.IS_PENDING} = 0)"
        } else {
            null
        }
        var count = 0
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
                upsertRow(
                    mediaStoreId = id,
                    displayName = name,
                    durationMs = duration,
                    dateModifiedMs = modifiedSec * 1000L,
                    sizeBytes = size,
                    contentUri = uri.toString(),
                )
                count++
            }
        }
        return count
    }

    private suspend fun scanFilesAudio(volume: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return 0
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(volume)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DURATION,
        )
        val pending = "(${MediaStore.Files.FileColumns.IS_PENDING} IS NULL OR ${MediaStore.Files.FileColumns.IS_PENDING} = 0)"
        val type = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}"
        val selection = "$type AND $pending"
        var count = 0
        resolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol)
                if (mime != null && mime.startsWith("video/", ignoreCase = true)) {
                    continue
                }
                val fileId = cursor.getLong(idCol)
                val syntheticId = syntheticIdForFilesRow(fileId)
                val name = cursor.getString(nameCol) ?: "recording"
                val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                val modifiedSec = cursor.getLong(modCol)
                val size = cursor.getLong(sizeCol)
                val uri = ContentUris.withAppendedId(collection, fileId)
                upsertRow(
                    mediaStoreId = syntheticId,
                    displayName = name,
                    durationMs = duration,
                    dateModifiedMs = modifiedSec * 1000L,
                    sizeBytes = size,
                    contentUri = uri.toString(),
                )
                count++
            }
        }
        return count
    }

    private suspend fun upsertRow(
        mediaStoreId: Long,
        displayName: String,
        durationMs: Long,
        dateModifiedMs: Long,
        sizeBytes: Long,
        contentUri: String,
    ) {
        val existing = recordingDao.getById(mediaStoreId)
        val uuid = existing?.recordingUuid ?: UUID.randomUUID().toString()
        val status = existing?.syncStatus ?: SyncStatus.PENDING
        val entity = RecordingEntity(
            mediaStoreId = mediaStoreId,
            recordingUuid = uuid,
            displayName = displayName,
            durationMs = durationMs,
            dateModifiedMs = dateModifiedMs,
            sizeBytes = sizeBytes,
            contentUri = contentUri,
            syncStatus = status,
            objectKey = existing?.objectKey,
            etag = existing?.etag,
            remoteSizeBytes = existing?.remoteSizeBytes,
            lastError = existing?.lastError,
            lastStableSizeBytes = existing?.lastStableSizeBytes,
            lastStableCheckMs = existing?.lastStableCheckMs,
            contentSha256 = existing?.contentSha256,
            syncProgressPercent = existing?.syncProgressPercent ?: 0,
        )
        recordingDao.upsertScanRow(entity)
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

    companion object {
        /** 与 Audio.Media 正数 _ID 错开，用于 Files 表行的 Room 主键 */
        private const val FILES_ID_BIAS = 10_000_000_000L

        fun syntheticIdForFilesRow(fileId: Long): Long = -(fileId + FILES_ID_BIAS)

        fun isSyntheticFilesId(mediaStoreId: Long): Boolean = mediaStoreId < -FILES_ID_BIAS
    }
}
