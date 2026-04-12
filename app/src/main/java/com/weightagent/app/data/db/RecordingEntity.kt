package com.weightagent.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val mediaStoreId: Long,
    val recordingUuid: String,
    val displayName: String,
    val durationMs: Long,
    val dateModifiedMs: Long,
    val sizeBytes: Long,
    val contentUri: String,
    val syncStatus: SyncStatus,
    val objectKey: String?,
    val etag: String?,
    val remoteSizeBytes: Long?,
    val lastError: String?,
    val lastStableSizeBytes: Long?,
    val lastStableCheckMs: Long?,
    /** 预留：后续可对文件做 SHA256 校验；本期可为 null */
    val contentSha256: String? = null,
)
