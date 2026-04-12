package com.weightagent.app.work

import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadRecordingWorkerObjectKeyTest {

    @Test
    fun buildObjectKey_includesUuidAndSanitizesName() {
        val entity = RecordingEntity(
            mediaStoreId = 1L,
            recordingUuid = "uuid-1",
            displayName = "bad:name?.m4a",
            durationMs = 1000L,
            dateModifiedMs = 1L,
            sizeBytes = 10L,
            contentUri = "content://x",
            syncStatus = SyncStatus.PENDING,
            objectKey = null,
            etag = null,
            remoteSizeBytes = null,
            lastError = null,
            lastStableSizeBytes = null,
            lastStableCheckMs = null,
        )
        val key = UploadRecordingWorker.buildObjectKey("pre/", entity)
        assertTrue(key.startsWith("pre/uuid-1_"))
        assertTrue(key.contains("bad_name_"))
    }
}
