package com.weightagent.app.work

import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.data.settings.CosSettings
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettings
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadRemotePathTest {

    private val entity = RecordingEntity(
        mediaStoreId = 1L,
        recordingUuid = "uuid-1",
        displayName = "a.m4a",
        durationMs = 0L,
        dateModifiedMs = 0L,
        sizeBytes = 100L,
        contentUri = "content://x",
        syncStatus = SyncStatus.PENDING,
        objectKey = null,
        etag = null,
        remoteSizeBytes = null,
        lastError = null,
        lastStableSizeBytes = null,
        lastStableCheckMs = null,
    )

    @Test
    fun cosPathUsesPrefix() {
        val cos = CosSettings("id", "key", "ap-guangzhou", "bkt", "pre/")
        val path = UploadRemotePath.build(CloudStorageKind.OBJECT_COS, cos, null, entity)
        assertTrue(path.startsWith("pre/"))
        assertTrue(path.contains("uuid-1"))
    }

    @Test
    fun aliyunPathUsesFolder() {
        val ad = AliyunDriveSettings("rt", "myrec")
        val path = UploadRemotePath.build(CloudStorageKind.CONSUMER_ALIYUN_DRIVE, null, ad, entity)
        assertTrue(path.startsWith("myrec/"))
        assertTrue(path.contains("uuid-1"))
    }
}
