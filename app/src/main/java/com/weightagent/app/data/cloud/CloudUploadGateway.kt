package com.weightagent.app.data.cloud

import com.weightagent.app.data.aliyundrive.AliyunDriveConsumerClient
import com.weightagent.app.data.cos.CosObjectStorageClient
import com.weightagent.app.data.settings.CloudStorageSelectionStore
import com.weightagent.app.data.settings.CosSettingsStore
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettingsStore

/**
 * 根据用户选择的 [CloudStorageKind] 分发到对象存储或消费级网盘实现。
 */
class CloudUploadGateway(
    private val selectionStore: CloudStorageSelectionStore,
    private val cosSettingsStore: CosSettingsStore,
    private val aliyunDriveSettingsStore: AliyunDriveSettingsStore,
    private val cosObjectStorageClient: CosObjectStorageClient,
    private val aliyunDriveConsumerClient: AliyunDriveConsumerClient,
) {

    suspend fun isReadyToUpload(): Boolean {
        val kind = selectionStore.readKindOrNull() ?: return false
        return when (kind) {
            CloudStorageKind.OBJECT_COS ->
                cosSettingsStore.read()?.isComplete() == true
            CloudStorageKind.CONSUMER_ALIYUN_DRIVE ->
                aliyunDriveSettingsStore.read()?.isComplete() == true
        }
    }

    suspend fun testConnection() {
        when (selectionStore.readKind()) {
            CloudStorageKind.OBJECT_COS -> cosObjectStorageClient.testConnection()
            CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> aliyunDriveConsumerClient.testConnection()
        }
    }

    suspend fun uploadFile(localAbsolutePath: String, remotePath: String): CloudUploadOutcome {
        return when (selectionStore.readKind()) {
            CloudStorageKind.OBJECT_COS -> {
                val r = cosObjectStorageClient.uploadFile(localAbsolutePath, remotePath)
                CloudUploadOutcome(etag = r.etag, remoteFileId = null)
            }
            CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> {
                val r = aliyunDriveConsumerClient.uploadFile(localAbsolutePath, remotePath)
                CloudUploadOutcome(etag = null, remoteFileId = r.remoteFileId)
            }
        }
    }
}

data class CloudUploadOutcome(
    val etag: String?,
    val remoteFileId: String?,
)
