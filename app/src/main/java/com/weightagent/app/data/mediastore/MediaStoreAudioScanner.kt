package com.weightagent.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
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

    suspend fun refreshFromMediaStore(): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val extra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.IS_PENDING} = 0"
        } else {
            null
        }
        val mimeClause = "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
        val selection = if (extra != null) "$mimeClause AND $extra" else mimeClause
        var count = 0
        resolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.DATE_MODIFIED} DESC")
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
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
                    count++
                }
            }
        count
    }
}
